#include <stdio.h>
/**
 *  @file serial_reader_l3.c
 *  @brief Mercury API - serial reader low level implementation
 *  @author Nathan Williams
 *  @date 11/2/2009
 */

/*
 * Copyright (c) 2009 ThingMagic, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#include "tm_reader.h"
#include "serial_reader_imp.h"
#include "tmr_utils.h"

#ifdef TMR_ENABLE_SERIAL_READER

static TMR_Status filterbytes(TMR_TagProtocol protocol,
                              const TMR_TagFilter *filter, 
                              uint8_t *option, uint8_t *i, uint8_t *msg,
                              uint32_t accessPassword, bool usePassword);


/*
 * ThingMagic-mutated CRC used for messages.
 * Notably, not a CCITT CRC-16, though it looks close.
 */
static uint16_t crctable[] = 
{
  0x0000, 0x1021, 0x2042, 0x3063,
  0x4084, 0x50a5, 0x60c6, 0x70e7,
  0x8108, 0x9129, 0xa14a, 0xb16b,
  0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
};

static uint16_t
tm_crc(uint8_t *u8Buf, uint8_t len)
{
  uint16_t crc;
  int i;

  crc = 0xffff;

  for (i = 0; i < len ; i++)
  {
    crc = ((crc << 4) | (u8Buf[i] >> 4))  ^ crctable[crc >> 12];
    crc = ((crc << 4) | (u8Buf[i] & 0xf)) ^ crctable[crc >> 12];
  }

  return crc;
}


/**
 * Send a byte string
 *
 * @param reader The reader
 * @param[in] len Number of bytes to send
 * @param[in] data Bytes to send, with length in byte 1. Byte 0 is reserved for the SOF character, and two characters at the end are reserved for the CRC.
 * @param timeoutMs Timeout value.
 */
TMR_Status
TMR_SR_sendBytes(TMR_Reader *reader, uint8_t len, uint8_t *data, uint32_t timeoutMs)
{
  TMR_SR_SerialTransport *transport;
  TMR_Status ret;

  transport = &reader->u.serialReader.transport;

  if (NULL != reader->transportListeners)
  {
    TMR__notifyTransportListeners(reader, true, len, data, timeoutMs);
  }

  ret = transport->sendBytes(transport, len, data, timeoutMs);
  return ret;
}

/**
 * Send a message to the reader
 *
 * @param reader The reader
 * @param[in] data Message to send, with length in byte 1. Byte 0 is reserved for the SOF character, and two characters at the end are reserved for the CRC.
 * @param[out] opcode Opcode sent with message (pass this value to receiveMessage to match against response)
 * @param timeoutMs Timeout value.
 */
TMR_Status
TMR_SR_sendMessage(TMR_Reader *reader, uint8_t *data, uint8_t *opcode, uint32_t timeoutMs)
{
  TMR_SR_SerialReader *sr;
  TMR_Status ret;
  uint16_t crc;
  uint8_t len;

  sr = &reader->u.serialReader;
  timeoutMs += sr->transportTimeout;

  /* Wake up processor from deep sleep.  Tickle the RS-232 line, then
   * wait a fixed delay while the processor spins up communications again. */
  if (sr->powerMode >= TMR_SR_POWER_MODE_MEDSAVE)
  {
    uint8_t flushBytes[] = {
      0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF
    };

    TMR_SR_sendBytes(reader, sizeof(flushBytes)/sizeof(uint8_t), flushBytes, timeoutMs);
    {
      uint32_t bytesper100ms;
      uint32_t bytesSent;

      /* Calculate fixed delay in terms of byte-lengths at current speed */
      /* @todo Optimize delay length.  This value (100 bytes at 9600bps) is taken
       * directly from arbser, which was itself using a hastily-chosen value. */
      bytesper100ms = sr->baudRate / 100;
      for (bytesSent=0; bytesSent<bytesper100ms;
           bytesSent += (sizeof(flushBytes)/sizeof(uint8_t)))
      {
        TMR_SR_sendBytes(reader, sizeof(flushBytes)/sizeof(uint8_t), flushBytes, timeoutMs);
      }
    }
  }

  /* Layout of message in data array: 
   * [0] [1] [2] [3] [4]  ... [LEN+2] [LEN+3] [LEN+4]
   * FF  LEN OP  xx  xx   ... xx      CRCHI   CRCLO
   */
  data[0] = 0xff;
  len = data[1];
  crc = tm_crc(&data[1], len + 2);
  data[len + 3] = crc >> 8;
  data[len + 4] = crc & 0xff;

  *opcode = data[2];

  ret = TMR_SR_sendBytes(reader, len+5, data, timeoutMs);
  return ret;
}


/**
 * Receive a response.
 *
 * @param reader The reader
 * @param[in] data Message to send, with length in byte 1. Byte 0 is reserved for the SOF character, and two characters at the end are reserved for the CRC.
 * @param[out] data Message received.
 * @param opcode Opcode that was sent with message that elicited this response, to be matched against incoming response opcode.
 * @param timeoutMs Timeout value.
 */
TMR_Status
TMR_SR_receiveMessage(TMR_Reader *reader, uint8_t *data, uint8_t opcode, uint32_t timeoutMs)
{
  TMR_Status ret;
  uint16_t crc, status;
  uint8_t len;
  uint32_t inlen;
  uint32_t sohPosition;
  bool sohFound;
  int i;
  TMR_SR_SerialTransport *transport;

  transport = &reader->u.serialReader.transport;
  timeoutMs += reader->u.serialReader.transportTimeout;

  ret = transport->receiveBytes(transport, 7, &inlen, data, timeoutMs);
  if (TMR_SUCCESS != ret)
  {
    /* @todo Figure out how many bytes were actually obtained in a failed receive */
    TMR__notifyTransportListeners(reader, false, inlen, data, timeoutMs);
    return ret;
  }

  sohPosition = 0;
  sohFound = false;
  if (data[0] != (uint8_t)0xFF)
  {
    for (i = 1; i < 6; i++) 
    {
      if (data[i] == 0xFF)
      {
        sohPosition = i;
        sohFound = true;
        break;
      }
    }
    if (sohFound == false)
      return TMR_ERROR_TIMEOUT;
    else
      memcpy(data,data + sohPosition,7 - sohPosition);
  }

  /* Layout of response in data array: 
   * [0] [1] [2] [3]      [4]      [5] [6]  ... [LEN+4] [LEN+5] [LEN+6]
   * FF  LEN OP  STATUSHI STATUSLO xx  xx   ... xx      CRCHI   CRCLO
   */
  len = data[sohPosition + 1];

  if (0 == len)
  {
    inlen = 0;
  }
  else
  {
    ret = transport->receiveBytes(transport, len + sohPosition, &inlen, data + 7 - sohPosition, timeoutMs);
  }

  if (NULL != reader->transportListeners)
  {
    TMR__notifyTransportListeners(reader, false, inlen + 7, data, timeoutMs);
  }

  crc = tm_crc(&data[1], len + 4);
  if ((data[len + 5] != (crc >> 8)) ||
      (data[len + 6] != (crc & 0xff)))
  {
    return TMR_ERROR_CRC_ERROR;
  }

  if ((data[2] != opcode)&&((data[2] !=0x2F)||(!reader->u.serialReader.useStreaming)))
  {
    /* We got a response for a different command than the one we
     * sent. This usually means we recieved the boot-time message from
     * a M6e, and thus that the device was rebooted somewhere between
     * the previous command and this one. Report this as a problem.
     */
    return TMR_ERROR_DEVICE_RESET;
 }

  status = GETU16AT(data, 3);
  if (status != 0)
  {
    ret = TMR_ERROR_CODE(status);
  }
  
  return ret;
}

/**
 * Send a message and receive a response.
 *
 * @param reader The reader
 * @param[in] data Message to send, with length in byte 1. Byte 0 is reserved for the SOF character, and two characters at the end are reserved for the CRC.
 * @param[out] data Message received.
 * @param timeoutMs Timeout value.
 */
TMR_Status
TMR_SR_sendTimeout(TMR_Reader *reader, uint8_t *data, uint32_t timeoutMs)
{
  TMR_Status ret;
  uint8_t opcode;
  ret = TMR_SR_sendMessage(reader, data, &opcode, timeoutMs);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  ret = TMR_SR_receiveMessage(reader, data, opcode, timeoutMs);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  return ret;
}


TMR_Status
TMR_SR_send(TMR_Reader *reader, uint8_t *data)
{
  return TMR_SR_sendTimeout(reader, data,
                            reader->u.serialReader.commandTimeout);
}

/**
 * Set the operating frequency of the device.
 * Testing command.
 *
 * @param reader The reader
 * @param frequency the frequency to set, in kHz
 */ 
TMR_Status 
TMR_SR_cmdTestSetFrequency(TMR_Reader *reader, uint32_t frequency)
{
  
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  i = 2;
  SETU8(msg, i,TMR_SR_OPCODE_SET_OPERATING_FREQ);
  SETU32(msg, i, frequency);
  msg[1] = i - 3; /* Install length */
  return TMR_SR_send(reader, msg);
}

/**
 * Turn CW transmission on or off.
 * Testing command.
 *
 * @param reader The reader
 * @param on whether to turn CW on or off
 */ 
TMR_Status 
TMR_SR_cmdTestSendCw(TMR_Reader *reader, bool on)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  i = 2;
  SETU8(msg, i,TMR_SR_OPCODE_TX_CW_SIGNAL);
  if(on)
    SETU8(msg, i,1);
  else
    SETU8(msg, i,0);
  msg[1] = i - 3; /* Install length */
  return TMR_SR_send(reader, msg);
}

/**
 * Turn on pseudo-random bit stream transmission for a particular
 * duration.  
 * Testing command.
 *
 * @param reader The reader
 * @param duration the duration to transmit the PRBS signal.
 */ 
TMR_Status 
TMR_SR_cmdTestSendPrbs(TMR_Reader *reader, uint16_t duration)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  i = 2;
  SETU8(msg, i,TMR_SR_OPCODE_TX_CW_SIGNAL);
  SETU8(msg, i,2);
  SETU16(msg, i,duration);
  msg[1] = i - 3; /* Install length */
  return TMR_SR_send(reader, msg);
}

/**
 * Setting user profile on the basis of option,key and value parameter
 *
 * @param reader The reader
 * @param option Save,restore,verify and reset configuration
 * @param key Which part of configuration to operate on -- only TMR_SR_ALL is currently supported
 * @param value Type of configuration value to use (default, custom...)
 */



TMR_Status 
TMR_SR_cmdSetUserProfile(TMR_Reader *reader,TMR_SR_SetUserProfileOption option,TMR_SR_SetUserProfileKey key, TMR_SR_SetUserProfileValue value)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  TMR_Status ret;
  TMR_Status ret1;
  static uint32_t rates[] = { 9600, 115200, 921600, 19200, 38400, 57600,
                              230400, 460800};
  uint32_t rate;
  TMR_SR_SerialReader *sr;
  TMR_SR_SerialTransport *transport;
  int j;

  sr = &reader->u.serialReader;
  transport = &reader->u.serialReader.transport;

  i = 2;
  SETU8(msg,i,TMR_SR_OPCODE_SET_USER_PROFILE);
  SETU8(msg,i,option);
  SETU8(msg,i,key);
  SETU8(msg,i,value);
  msg[1] = i - 3; /* Install length */
  ret1 = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret1)
  {
    return ret1;
  }

  if ((option == TMR_SR_RESTORE)||(option == TMR_SR_CLEAR))  //reprobe the baudrate
  {
    if (reader->connected == false)
    {
      ret = transport->open(transport);
      if (TMR_SUCCESS != ret)
      {
        return ret;
      }

    }

    /* Make contact at some baud rate */
    for (j = 0; j <= numberof(rates); j++)
    {
      if (0 == j)
      {
        rate = sr->baudRate; /* Try this first */
      }
      else
      {
        rate = rates[j-1];
        if (rate == sr->baudRate)
          continue; /* We already tried this one */
      }

      ret = transport->setBaudRate(transport, rate);
      if (TMR_SUCCESS != ret)
      {
        return ret;
      }

      ret = transport->flush(transport);
      if (TMR_SUCCESS != ret)
      {
        return ret;
      }

      ret = TMR_SR_cmdVersion(reader, NULL);

      if (TMR_SUCCESS == ret)
      {
        break;
      }
      /* Timeouts are okay -- they usually mean "wrong baud rate",
      * so just try the next one.  All other errors are real
      * and should be forwarded immediately. */
      else if (TMR_ERROR_TIMEOUT != ret)
      {
        return ret;
      }
    }
    if (j == numberof(rates))
    {
      return TMR_ERROR_TIMEOUT;
    }
    reader->connected = true;
  }

  return ret1;
}

/**
 * Get Save/Restore Configuration 
 *
 * @param reader The reader
 * @param byte array consists of a opcode option(s)
 * @param length Length of byte array
 * @param response Response of the operation
 * @param response_length Length of response array
 */

TMR_Status TMR_SR_cmdGetUserProfile(TMR_Reader *reader, uint8_t byte[], uint8_t length, uint8_t response[], uint8_t* response_length)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i,j;
  i = 2;
  SETU8(msg,i,TMR_SR_OPCODE_GET_USER_PROFILE);
  for(j=0;j<length;j++)
  {
    SETU8(msg,i,byte[j]);
  }
  msg[1] = i - 3;
  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  for(j=0;j<msg[1];j++)
  {
    response[j]=msg[5+j];
  }
  *response_length=msg[1];
  return ret;
}

TMR_Status TMR_SR_cmdBlockWrite(TMR_Reader *reader, uint16_t timeout, TMR_GEN2_Bank bank, uint32_t wordPtr, 
                                 uint32_t wordCount, const uint8_t* data, uint32_t accessPassword, const TMR_TagFilter* target)
        {   
            uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
            uint8_t i, option=0,rec;
            i = 2;        
            SETU8(msg,i,TMR_SR_OPCODE_WRITE_TAG_SPECIFIC);
            SETU16(msg, i,timeout);
            SETU8(msg,i,0x00);//chip type
            rec=i;
            SETU8(msg,i,0x40);//option
            SETU8(msg,i,0x00);
            SETU8(msg,i,0xC7);
            filterbytes(TMR_TAG_PROTOCOL_GEN2, target, &option, &i, msg,accessPassword,true);
            msg[rec]=msg[rec]|option;
            SETU8(msg,i,0x00);
            SETU8(msg,i,bank);
            SETU32(msg,i,wordPtr);
            SETU8(msg,i,(uint8_t)wordCount);
            memcpy(&msg[i], data, 2*wordCount);
            i += (uint8_t)(2*wordCount);
            msg[1] = i - 3;
            return TMR_SR_send(reader, msg);
         }

TMR_Status 
TMR_SR_cmdBlockPermaLock(TMR_Reader *reader, uint16_t timeout,uint32_t readLock, TMR_GEN2_Bank bank, uint32_t blockPtr, uint32_t blockRange, uint16_t* mask, uint32_t accessPassword, TMR_TagFilter* target, uint16_t data[])
        { 
            TMR_Status ret;
            uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
            uint8_t i, option=0,rec;
            unsigned int j;
            i = 2;    
            SETU8(msg,i,TMR_SR_OPCODE_ERASE_BLOCK_TAG_SPECIFIC);
            SETU16(msg,i,timeout);
            SETU8(msg,i,0x00);
            rec=i;
            SETU8(msg,i,0x40);
            SETU8(msg,i,0x01);
            filterbytes(TMR_TAG_PROTOCOL_GEN2,target, &option, &i, msg,accessPassword,true);
            msg[rec]=msg[rec]|option;
            SETU8(msg,i,0x00);
            SETU8(msg,i,(uint8_t)readLock);
            
            SETU8(msg,i,bank);
            SETU32(msg,i,blockPtr);
            SETU8(msg,i,(uint8_t)blockRange);
            
            if (readLock==0x01)
            {
              for(j=0;j<blockRange;j++)
              {
               SETU8(msg,i,(mask[j]>>8)&(0xff));
               SETU8(msg,i,(mask[j]>>0) & (0xff));
              }
            }
            msg[1] = i - 3;
            ret =  TMR_SR_send(reader, msg);
            if (TMR_SUCCESS != ret)
            {
              return ret;
            }
            if (readLock == 0)
            {
              memcpy(data,msg+7,(msg[1]-2));
            }
            return ret;
        }  
TMR_Status
TMR_SR_cmdVersion(TMR_Reader *reader, TMR_SR_VersionInfo *info)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i, j;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_VERSION);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  if (NULL != info)
  {
    i = 5;
    for (j = 0; j < 4 ; j++)
    {
      info->bootloader[j] = GETU8(msg, i);
    }
    for (j = 0; j < 4 ; j++)
    {
      info->hardware[j] = GETU8(msg, i);
    }
    for (j = 0; j < 4 ; j++)
    {
      info->fwDate[j] = GETU8(msg, i);
    }
    for (j = 0; j < 4 ; j++)
    {
      info->fwVersion[j] = GETU8(msg, i);
    }
    info->protocols = GETU32(msg, i);
  }

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdBootFirmware(TMR_Reader *reader)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_BOOT_FIRMWARE);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, 1000);
}


TMR_Status
TMR_SR_cmdSetBaudRate(TMR_Reader *reader, uint32_t rate)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_BAUD_RATE);
  SETU32(msg, i, rate);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdEraseFlash(TMR_Reader *reader, uint8_t sector, uint32_t password)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_ERASE_FLASH);
  SETU32(msg, i, password);
  SETU8(msg, i, sector);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, 30000);
}


TMR_Status
TMR_SR_cmdWriteFlashSector(TMR_Reader *reader, uint8_t sector, uint32_t address,
                           uint32_t password, uint8_t length, const uint8_t data[],
                           uint32_t offset)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_WRITE_FLASH_SECTOR);
  SETU32(msg, i, password);
  SETU32(msg, i, address);
  SETU8(msg, i, sector);
  memcpy(&msg[i], data + offset, length);
  i += length;
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, 3000);
}



TMR_Status
TMR_SR_cmdBootBootloader(TMR_Reader *reader)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_BOOT_BOOTLOADER);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdGetHardwareVersion(TMR_Reader *reader, uint8_t option, uint8_t flags,
                             uint8_t* count, uint8_t data[])
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_HW_VERSION);
  SETU8(msg, i, option);
  SETU8(msg, i, flags);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  for (i = 0 ; i < msg[1] && i < *count; i++)
  {
    data[i] = msg[5 + i];
  }

  *count = msg[1];

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetCurrentProgram(TMR_Reader *reader, uint8_t *program)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_CURRENT_PROGRAM);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  *program = msg[5];

  return ret;
}

TMR_Status TMR_SR_msgSetupReadTagSingle(uint8_t *msg, uint8_t *i, TMR_TagProtocol protocol,TMR_TRD_MetadataFlag metadataFlags, const TMR_TagFilter *filter,uint16_t timeout)
{
  uint8_t optbyte;

  SETU8(msg, *i, TMR_SR_OPCODE_READ_TAG_ID_SINGLE);
  SETU16(msg, *i, timeout);
  optbyte = *i;
  SETU8(msg, *i, 0); /* Initialize option byte */
  msg[optbyte] |= TMR_SR_GEN2_SINGULATION_OPTION_FLAG_METADATA;
  SETU16(msg,*i, metadataFlags);
  filterbytes(protocol, filter, &msg[optbyte], i, msg,0, true);
  msg[optbyte] |= TMR_SR_GEN2_SINGULATION_OPTION_FLAG_METADATA;
  return TMR_SUCCESS;

}

TMR_Status
TMR_SR_msgSetupReadTagMultiple(TMR_Reader *reader, uint8_t *msg, uint8_t *i, uint16_t timeout,
                               TMR_SR_SearchFlag searchFlag,
                               const TMR_TagFilter *filter,
                               TMR_TagProtocol protocol,
                               TMR_GEN2_Password accessPassword)
{
  return TMR_SR_msgSetupReadTagMultipleWithMetadata(reader, msg, i, timeout,
    searchFlag, TMR_TRD_METADATA_FLAG_ALL, filter, protocol,accessPassword);
}


TMR_Status
TMR_SR_msgSetupReadTagMultipleWithMetadata(TMR_Reader *reader, uint8_t *msg, uint8_t *i, uint16_t timeout,
                               TMR_SR_SearchFlag searchFlag,
							                 TMR_TRD_MetadataFlag metadataFlag,
                               const TMR_TagFilter *filter,
                               TMR_TagProtocol protocol,
                               TMR_GEN2_Password accessPassword)
{
  TMR_Status ret;
  TMR_SR_SerialReader *sr;
  uint8_t optbyte;

  sr = &reader->u.serialReader;
  sr->opCode = TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE;

  SETU8(msg, *i, TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE);
  optbyte = *i;
  SETU8(msg, *i, 0); /* Initialize option byte */
  if (sr->useStreaming)
  {
    msg[optbyte] |= TMR_SR_GEN2_SINGULATION_OPTION_FLAG_METADATA;
    searchFlag = searchFlag
      | TMR_SR_SEARCH_FLAG_TAG_STREAMING
      | TMR_SR_SEARCH_FLAG_LARGE_TAG_POPULATION_SUPPORT;
  }
  SETU16(msg, *i, searchFlag);
  SETU16(msg, *i, timeout);
  if (sr->useStreaming)
  {
    SETU16(msg, *i, metadataFlag);
  }

  /*
   * We skip filterbytes() for a null filter and gen2 0 access password
   * so that we don't pass any filtering information at all unless necessary;
   * for some protocols (such as ISO180006B) the "null" filter is not zero-length, 
   * but we don't need to send that with this command.
   */
  if (NULL != filter 
      || (TMR_TAG_PROTOCOL_GEN2 == protocol && 0 == accessPassword))
  {
    ret = filterbytes(protocol, filter, &msg[optbyte], i, msg,
                      accessPassword, true);
    if (sr->useStreaming)
    {
      msg[optbyte] |= TMR_SR_GEN2_SINGULATION_OPTION_FLAG_METADATA;
    }
    return ret;
  }
  else
  {
    return TMR_SUCCESS;
  }
}

TMR_Status
TMR_SR_cmdReadTagMultiple(TMR_Reader *reader, uint16_t timeout,
                          TMR_SR_SearchFlag searchFlag,
                          const TMR_TagFilter *filter, TMR_TagProtocol protocol,
                          uint32_t *tagCount)
{
  TMR_Status ret;
  TMR_SR_SerialReader *sr;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  ret = TMR_SR_msgSetupReadTagMultiple(reader, msg, &i, timeout, searchFlag,
                                       filter, protocol, 0);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  msg[1] = i - 3; /* Install length */

  sr = &reader->u.serialReader;
  sr->opCode = TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE;
  if (sr->useStreaming)
  {
    uint8_t opcode;
    ret = TMR_SR_sendMessage(reader, msg, &opcode, timeout);
    *tagCount = -1;
    return ret;
  }
  else
  {
    ret = TMR_SR_sendTimeout(reader, msg, timeout);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }

    if (NULL != tagCount)
    {
      if (4 == msg[1])
      {
        /* Plain 1-byte count: Reader without large-tag-population support */
        *tagCount = GETU8AT(msg, 8);
      }
      else if (5 == msg[1])
      {
        /* Later 1-byte count: ISO18k select option included in reply */
        *tagCount = GETU8AT(msg, 9);
      }
      else if (7 == msg[1])
      {
        /* Plain 4-byte count: Reader with large-tag-population support */
        *tagCount = GETU32AT(msg, 8);
      }
      else if (8 == msg[1])
      {
        /* Later 4-byte count: Large-tag-population support and ISO18k
         * select option included in reply.
         */
        *tagCount = GETU32AT(msg, 9);
      }
      else
      {
        return TMR_ERROR_PARSE;
      }
    }

    return TMR_SUCCESS;
  }
}


TMR_Status
TMR_SR_executeEmbeddedRead(TMR_Reader *reader, uint8_t *msg, uint16_t timeout,
                           TMR_SR_MultipleStatus *status)
{
  TMR_Status ret;

  ret = TMR_SR_sendTimeout(reader, msg, timeout);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  if (NULL != status)
  {
    status->tagsFound = msg[8];
    status->successCount = GETU16AT(msg, 11);
    status->failureCount = GETU16AT(msg, 13);
  }

  return TMR_SUCCESS;
}


void
TMR_SR_msgAddGEN2DataRead(uint8_t *msg, uint8_t *i, uint16_t timeout,
                      TMR_GEN2_Bank bank, uint32_t wordAddress, uint8_t len)
{

  SETU8(msg, *i, TMR_SR_OPCODE_READ_TAG_DATA);
  SETU16(msg, *i, timeout);
  SETU8(msg, *i, 0);  /* Options - initialize */
  SETU8(msg, *i, bank);
  SETU32(msg, *i, wordAddress);
  SETU8(msg, *i, len);
}


void
TMR_SR_msgAddGEN2DataWrite(uint8_t *msg, uint8_t *i, uint16_t timeout,
                       TMR_GEN2_Bank bank, uint32_t address)
{

  SETU8(msg, *i, TMR_SR_OPCODE_WRITE_TAG_DATA);
  SETU16(msg, *i, timeout);
  SETU8(msg, *i, 0); /* Option - initialize */
  SETU32(msg, *i, address);
  SETU8(msg, *i, bank);
}


void
TMR_SR_msgAddGEN2LockTag(uint8_t *msg, uint8_t *i, uint16_t timeout, uint16_t mask,
                         uint16_t action, TMR_GEN2_Password accessPassword)
{

  SETU8(msg, *i, TMR_SR_OPCODE_LOCK_TAG);
  SETU16(msg, *i, timeout);
  SETU8(msg, *i, 0);  /* Option - initialize */
  SETU32(msg, *i, accessPassword);
  SETU16(msg, *i, mask);
  SETU16(msg, *i, action);
}


void
TMR_SR_msgAddGEN2KillTag(uint8_t *msg, uint8_t *i, uint16_t timeout,
                         TMR_GEN2_Password password)
{

  SETU8(msg, *i, TMR_SR_OPCODE_KILL_TAG);
  SETU16(msg, *i, timeout);
  SETU8(msg, *i, 0);  /* Option - initialize */
  SETU32(msg, *i, password);
}

void
TMR_SR_msgAddGEN2BlockWrite(uint8_t *msg, uint8_t *i, uint16_t timeout,TMR_GEN2_Bank bank, uint32_t wordPtr, uint32_t wordCount, uint8_t* data, uint32_t accessPassword,TMR_TagFilter* target)
{
  uint8_t option=0,rec;      
  SETU8(msg,*i,TMR_SR_OPCODE_WRITE_TAG_SPECIFIC);
  SETU16(msg, *i,timeout);
  SETU8(msg,*i,0x00);//chip type
  rec=*i;
  SETU8(msg,*i,0x40);//option
  SETU8(msg,*i,0x00);
  SETU8(msg,*i,0xC7);
  filterbytes(TMR_TAG_PROTOCOL_GEN2, target, &option, i, msg,accessPassword,true);
  msg[rec]=msg[rec]|option;
  SETU8(msg,*i,0x00);
  SETU8(msg,*i,bank);
  SETU32(msg,*i,wordPtr);
  SETU8(msg,*i,(uint8_t)wordCount);
  memcpy(&msg[*i], data, 2*wordCount);
  *i += (uint8_t)(2*wordCount);
}

void
TMR_SR_msgAddGEN2BlockPermaLock(uint8_t *msg, uint8_t *i, uint16_t timeout, uint32_t readLock, TMR_GEN2_Bank bank, uint32_t blockPtr, uint32_t blockRange, uint16_t* mask, uint32_t accessPassword,TMR_TagFilter* target)
{
  uint8_t option=0,rec;    
  SETU8(msg,*i,TMR_SR_OPCODE_ERASE_BLOCK_TAG_SPECIFIC);
  SETU16(msg,*i,timeout);
  SETU8(msg,*i,0x00);
  rec=*i;
  SETU8(msg,*i,0x40);
  SETU8(msg,*i,0x01);
  filterbytes(TMR_TAG_PROTOCOL_GEN2,target, &option, i, msg,accessPassword,true);
  msg[rec]=msg[rec]|option;
  SETU8(msg,*i,0x00);
  SETU8(msg,*i,(uint8_t)readLock);

  SETU8(msg,*i,bank);
  SETU32(msg,*i,blockPtr);
  SETU8(msg,*i,(uint8_t)blockRange);
  if (readLock==0x01)
  {
    memcpy(&msg[*i], mask, 2*blockRange);
    *i += (uint8_t)(2*blockRange);
  }

}


TMR_Status
TMR_SR_cmdWriteTagEpc(TMR_Reader *reader, uint16_t timeout,
            uint8_t count, const uint8_t *id, bool lock)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_WRITE_TAG_ID);
  SETU16(msg, i, timeout);
  SETU16(msg, i, 0);
  memcpy(&msg[i], id, count);
  i += count;
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, timeout);
}


TMR_Status
TMR_SR_cmdClearTagBuffer(TMR_Reader *reader)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_CLEAR_TAG_ID_BUFFER);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}

void
TMR_SR_parseMetadataFromMessage(TMR_Reader *reader, TMR_TagReadData *read, uint16_t flags,
                                uint8_t *i, uint8_t msg[])
{
  int msgEpcLen;

  read->metadataFlags = flags;
  read->tag.protocol = TMR_TAG_PROTOCOL_NONE;
  read->readCount = 0;
  read->rssi = 0;
  read->antenna = 0;
  read->phase = 0;
  read->frequency = 0;
  read->dspMicros = 0;
  read->timestampLow = 0;
  read->timestampHigh = 0;

  switch(reader->u.serialReader.versionInfo.hardware[0])
  {
  case TMR_SR_MODEL_M5E:
    read->gpioCount = 2;
    break;
  case TMR_SR_MODEL_M6E:
    read->gpioCount = 4;
    break;
  default:
    read->gpioCount = 4;
    break;
  }
    

  /* Fill in tag data from response */
  if (flags & TMR_TRD_METADATA_FLAG_READCOUNT)
  {
    read->readCount = GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_RSSI)
  {
    read->rssi = (int8_t)GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_ANTENNAID)
  {
    read->antenna = GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_FREQUENCY)
  {
    read->frequency = GETU24(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_TIMESTAMP)
  {
    read->dspMicros = GETU32(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_PHASE)
  {
    read->phase = GETU16(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_PROTOCOL)
  {
    read->tag.protocol = GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_DATA)
  {
    int msgDataLen, copyLen;

    msgDataLen = GETU16(msg, *i) / 8;
    read->data.len = msgDataLen;
    copyLen = msgDataLen;
    if (copyLen > read->data.max)
    {
      copyLen = read->data.max;
    }
    memcpy(read->data.list, &msg[*i], copyLen);
    *i += msgDataLen;
  }
  if (flags & TMR_TRD_METADATA_FLAG_GPIO_STATUS)
  {
    int j;
    uint8_t gpioByte=GETU8(msg, *i);
    for (j=0;j<read->gpioCount;j++) 
    {
      read->gpio[j].id = j+1;
      read->gpio[j].high = (((gpioByte >> j)&0x1)== 1);
    }
  }

  msgEpcLen = (GETU16(msg, *i) / 8) - 2;
  if (TMR_TAG_PROTOCOL_GEN2 == read->tag.protocol)
  {
    msgEpcLen -= 2;
    read->tag.u.gen2.pcByteCount = 2;
    read->tag.u.gen2.pc[0] = GETU8(msg, *i);
    read->tag.u.gen2.pc[1] = GETU8(msg, *i);
  }
  read->tag.epcByteCount = msgEpcLen;
  if (read->tag.epcByteCount > TMR_MAX_EPC_BYTE_COUNT)
    read->tag.epcByteCount = TMR_MAX_EPC_BYTE_COUNT;
  memcpy(read->tag.epc, &msg[*i], read->tag.epcByteCount);
  *i += msgEpcLen;
  read->tag.crc = GETU16(msg, *i);
}

void
TMR_SR_parseMetadataOnly(TMR_Reader *reader, TMR_TagReadData *read, uint16_t flags,
                                uint8_t *i, uint8_t msg[])
{
  read->metadataFlags = flags;
  read->tag.protocol = TMR_TAG_PROTOCOL_NONE;
  read->readCount = 0;
  read->rssi = 0;
  read->antenna = 0;
  read->phase = 0;
  read->frequency = 0;
  read->dspMicros = 0;
  read->timestampLow = 0;
  read->timestampHigh = 0;

  switch(reader->u.serialReader.versionInfo.hardware[0])
  {
  case TMR_SR_MODEL_M5E:
    read->gpioCount = 2;
    break;
  case TMR_SR_MODEL_M6E:
    read->gpioCount = 4;
    break;
  default:
    read->gpioCount = 4;
    break;
  }

  /* Fill in tag data from response */
  if (flags & TMR_TRD_METADATA_FLAG_READCOUNT)
  {
    read->readCount = GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_RSSI)
  {
    read->rssi = GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_ANTENNAID)
  {
    read->antenna = GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_FREQUENCY)
  {
    read->frequency = GETU24(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_TIMESTAMP)
  {
    read->dspMicros = GETU32(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_PHASE)
  {
    read->phase = GETU16(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_PROTOCOL)
  {
    read->tag.protocol = GETU8(msg, *i);
  }
  if (flags & TMR_TRD_METADATA_FLAG_DATA)
  {
    int msgDataLen, copyLen;

    msgDataLen = GETU16(msg, *i) / 8;
    read->data.len = msgDataLen;
    copyLen = msgDataLen;
    if (copyLen > read->data.max)
    {
      copyLen = read->data.max;
    }
    memcpy(read->data.list, &msg[*i], copyLen);
    *i += msgDataLen;
  }
  if (flags & TMR_TRD_METADATA_FLAG_GPIO_STATUS)
  {
    int j;
    uint8_t gpioByte=GETU8(msg, *i);
    for (j=0;j<read->gpioCount ;j++) 
    {
      read->gpio[j].id = j+1;
      read->gpio[j].high = (((gpioByte >> j)&0x1)== 1);
    }
  }
}

void
TMR_SR_postprocessReaderSpecificMetadata(TMR_TagReadData *read, TMR_SR_SerialReader *sr)
{
  uint16_t j;
  uint32_t timestampLow;

  timestampLow = sr->readTimeLow;
  read->timestampHigh = sr->readTimeHigh;

  timestampLow = timestampLow + read->dspMicros / 1000;
  if (timestampLow < sr->readTimeLow) /* Overflow */
  {
    read->timestampHigh++;
  }
  read->timestampLow = timestampLow;

  if (0 != read->antenna)
  {
    for (j = 0; j < sr->txRxMap->len; j++)
    {
      if ((read->antenna & 0xf) == sr->txRxMap->list[j].rxPort
          && (read->antenna >> 4) == sr->txRxMap->list[j].txPort)
      {
        read->antenna = sr->txRxMap->list[j].antenna;
        break;
      }
    }
  }
}

#ifdef TMR_ENABLE_ISO180006B
TMR_Status
TMR_SR_cmdISO180006BReadTagData(TMR_Reader *reader,
                                uint16_t timeout, uint8_t address,
                                uint8_t length, const TMR_TagFilter *filter,
                                TMR_TagReadData *read)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t copylen, i;

  if (length > 8
      || filter == NULL
      || TMR_FILTER_TYPE_TAG_DATA != filter->type
      || filter->u.tagData.epcByteCount != 8)
  {
    return TMR_ERROR_INVALID;
  }

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_READ_TAG_DATA);
  SETU16(msg, i, timeout);
  SETU8(msg, i, 0x01); /* Standard read operations */
  SETU8(msg, i, TMR_SR_ISO180006B_COMMAND_READ);
  SETU8(msg, i, 0x00); /* RFU */
  SETU8(msg, i, length);
  SETU8(msg, i, address);
  memcpy(&msg[i], filter->u.tagData.epc, 8);
  i += 8;
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_sendTimeout(reader, msg, timeout);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  read->metadataFlags = TMR_TRD_METADATA_FLAG_DATA;
  read->tag.protocol = TMR_TAG_PROTOCOL_ISO180006B;
  read->tag.epcByteCount = 0;

  read->data.len = msg[1];
  copylen = (uint8_t)read->data.len;
  if (copylen > read->data.max)
  {
    copylen = (uint8_t)read->data.max;
  }
  memcpy(read->data.list, &msg[5], copylen);

  return TMR_SUCCESS;
}

TMR_Status
TMR_SR_cmdISO180006BWriteTagData(TMR_Reader *reader,
                                 uint16_t timeout, uint8_t address,
                                 uint8_t count, const uint8_t data[],
                                 const TMR_TagFilter *filter)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;

  SETU8(msg, i, TMR_SR_OPCODE_WRITE_TAG_DATA);
  SETU16(msg, i, timeout);
  if (NULL != filter
    && TMR_FILTER_TYPE_TAG_DATA == filter->type
    && filter->u.tagData.epcByteCount == 8)
  {
    SETU8(msg, i, 
          TMR_SR_ISO180006B_WRITE_OPTION_READ_VERIFY_AFTER
          | TMR_SR_ISO180006B_WRITE_OPTION_COUNT_PROVIDED); 
    SETU8(msg, i, TMR_SR_ISO180006B_COMMAND_WRITE4BYTE);
    SETU8(msg, i, TMR_SR_ISO180006B_WRITE_LOCK_NO);
    SETU8(msg, i, address);
    memcpy(&msg[i], filter->u.tagData.epc, 8);
    i += 8;
  }
  else
  {
    SETU8(msg, i, TMR_SR_ISO180006B_WRITE_OPTION_GROUP_SELECT);
    SETU8(msg, i, TMR_SR_ISO180006B_COMMAND_WRITE4BYTE_MULTIPLE);
    SETU8(msg, i, TMR_SR_ISO180006B_WRITE_LOCK_NO);
    SETU8(msg, i, address);
    ret = filterbytes(TMR_TAG_PROTOCOL_ISO180006B, filter, NULL,
                      &i, msg, 0, false);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
  }
  SETU16(msg, i, count);
  memcpy(&msg[i], data, count);
  i += count;
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, timeout);

}


TMR_Status
TMR_SR_cmdISO180006BLockTag(TMR_Reader *reader, uint16_t timeout,
                            uint8_t address, const TMR_TagFilter *filter)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  if (filter == NULL
      || TMR_FILTER_TYPE_TAG_DATA != filter->type
      || filter->u.tagData.epcByteCount != 8)
  {
    return TMR_ERROR_INVALID;
  }

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_LOCK_TAG);
  SETU16(msg, i, timeout);
  SETU8(msg, i, TMR_SR_ISO180006B_LOCK_OPTION_TYPE_FOLLOWS);
  SETU8(msg, i, TMR_SR_ISO180006B_LOCK_TYPE_QUERYLOCK_THEN_LOCK);
  SETU8(msg, i, address);
  memcpy(&msg[i], filter->u.tagData.epc, 8);
  i += 8;
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, timeout);
}
#endif /* TMR_ENABLE_ISO180006B */

TMR_Status
TMR_SR_cmdGEN2WriteTagData(TMR_Reader *reader,
                           uint16_t timeout, TMR_GEN2_Bank bank,
                           uint32_t address, uint8_t count,
                           const uint8_t data[],
                           TMR_GEN2_Password accessPassword,
                           const TMR_TagFilter *filter)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t optbyte, i;

  i = 2;
  TMR_SR_msgAddGEN2DataWrite(msg, &i, timeout, bank, address);
  optbyte = 5;
  ret = filterbytes(TMR_TAG_PROTOCOL_GEN2, filter, &msg[optbyte], &i, msg,
                    accessPassword, true);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  if (i + count + 1 > TMR_SR_MAX_PACKET_SIZE)
  {
    return TMR_ERROR_TOO_BIG;
  }
  memcpy(&msg[i], data, count);
  i += count;
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, timeout);
}

TMR_Status
TMR_SR_cmdGEN2LockTag(TMR_Reader *reader, uint16_t timeout,
                      uint16_t mask, uint16_t action, 
                      TMR_GEN2_Password accessPassword, 
                      const TMR_TagFilter *filter)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t optbyte, i;

  i = 2;
  TMR_SR_msgAddGEN2LockTag(msg, &i, timeout, (uint16_t)accessPassword, mask, action);
  optbyte = 5;
  ret = filterbytes(TMR_TAG_PROTOCOL_GEN2, filter, &msg[optbyte], &i, msg,
                    0, false);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, timeout);
}


TMR_Status
TMR_SR_cmdKillTag(TMR_Reader *reader, uint16_t timeout,
                  TMR_GEN2_Password killPassword, 
                  const TMR_TagFilter *filter)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t optbyte, i;

  i = 2;
  TMR_SR_msgAddGEN2KillTag(msg, &i, timeout, killPassword);
  optbyte = 5;
  ret = filterbytes(TMR_TAG_PROTOCOL_GEN2, filter, &msg[optbyte], &i, msg, 
                    0, false);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  msg[1] = i - 3; /* Install length */

  return TMR_SR_sendTimeout(reader, msg, timeout);
}


TMR_Status
TMR_SR_cmdGEN2ReadTagData(TMR_Reader *reader,
                          uint16_t timeout, TMR_GEN2_Bank bank,
                          uint32_t address, uint8_t length,
                          uint32_t accessPassword, const TMR_TagFilter *filter,
                          TMR_TagReadData *read)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t copylen, optbyte, i;

  i = 2;
  TMR_SR_msgAddGEN2DataRead(msg, &i, timeout, bank, address, length);
  optbyte = 5;
  ret = filterbytes(TMR_TAG_PROTOCOL_GEN2, filter, &msg[optbyte], &i, msg, 
                    accessPassword, true);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_sendTimeout(reader, msg, timeout);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  i = 6;
  read->metadataFlags = TMR_TRD_METADATA_FLAG_DATA;
  read->tag.protocol = TMR_TAG_PROTOCOL_GEN2;
  read->tag.epcByteCount = 0;

  read->data.len = msg[1] + 5 - i;
  copylen = (uint8_t)read->data.len;
  if (copylen > read->data.max)
  {
    copylen = (uint8_t)read->data.max;
  }
  memcpy(read->data.list, &msg[i], copylen);

  return TMR_SUCCESS;
}

TMR_Status
TMR_SR_cmdSetTxRxPorts(TMR_Reader *reader, uint8_t txPort, uint8_t rxPort)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_ANTENNA_PORT);
  SETU8(msg, i, txPort);
  SETU8(msg, i, rxPort);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetAntennaSearchList(TMR_Reader *reader, uint8_t count,
                               const TMR_SR_PortPair *ports)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  uint8_t j;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_ANTENNA_PORT);
  SETU8(msg, i, 2); /* logical antenna list option */
  for (j = 0; j < count ; j++)
  {
    SETU8(msg, i, ports[j].txPort);
    SETU8(msg, i, ports[j].rxPort);
  }
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}

TMR_Status
TMR_SR_cmdSetAntennaPortPowersAndSettlingTime(
  TMR_Reader *reader, uint8_t count, const TMR_SR_PortPowerAndSettlingTime *ports)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t j, i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_ANTENNA_PORT);
  SETU8(msg, i, 4); /* power and settling time option */

  for (j = 0; j < count; j++)
  {
    SETU8(msg, i, ports[j].port);
    SETU16(msg, i, ports[j].readPower);
    SETU16(msg, i, ports[j].writePower);
    SETU16(msg, i, ports[j].settlingTime);
  }
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetReadTxPower(TMR_Reader *reader, uint16_t power)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_READ_TX_POWER);
  SETU16(msg, i, power);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetProtocol(TMR_Reader *reader, TMR_TagProtocol protocol)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_TAG_PROTOCOL);
  SETU16(msg, i, protocol);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetWriteTxPower(TMR_Reader *reader, uint16_t power)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_WRITE_TX_POWER);
  SETU16(msg, i, power);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetFrequencyHopTable(TMR_Reader *reader, uint8_t count,
                               const uint32_t *table)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i, j;

  i = 2;

  if (count > 62)
  {
    return TMR_ERROR_TOO_BIG;
  }

  SETU8(msg, i, TMR_SR_OPCODE_SET_FREQ_HOP_TABLE);
  for (j = 0; j < count; j++)
  {
    SETU32(msg, i, table[j]);
  }
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetFrequencyHopTime(TMR_Reader *reader, uint32_t hopTime)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_FREQ_HOP_TABLE);
  SETU8(msg, i, 1); /* hop time option */
  SETU32(msg, i, hopTime);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetGPIO(TMR_Reader *reader, uint8_t gpio, bool high)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_USER_GPIO_OUTPUTS);
  SETU8(msg, i, gpio);
  SETU8(msg, i, (high == true));
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetRegion(TMR_Reader *reader, TMR_Region region)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_REGION);
  SETU8(msg, i, region);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetRegionLbt(TMR_Reader *reader, TMR_Region region, bool lbt)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_REGION);
  SETU8(msg, i, region);
  SETU8(msg, i, lbt ? 1 : 0);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetPowerMode(TMR_Reader *reader, TMR_SR_PowerMode mode)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_POWER_MODE);
  SETU8(msg, i, mode);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetUserMode(TMR_Reader *reader, TMR_SR_UserMode mode)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_USER_MODE);
  SETU8(msg, i, mode);
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}


TMR_Status
TMR_SR_cmdSetReaderConfiguration(TMR_Reader *reader, TMR_SR_Configuration key,
                                 const void *value)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_READER_OPTIONAL_PARAMS);
  SETU8(msg, i, 1); /* key-value form of command */
  SETU8(msg, i, key);

  switch (key)
  {
  case TMR_SR_CONFIGURATION_ANTENNA_CONTROL_GPIO:
    SETU8(msg, i, *(uint8_t *)value);
    break;

  case TMR_SR_CONFIGURATION_UNIQUE_BY_ANTENNA:
  case TMR_SR_CONFIGURATION_UNIQUE_BY_DATA:
    SETU8(msg, i, *(bool *)value ? 0 : 1);
    break;
  case TMR_SR_CONFIGURATION_TRANSMIT_POWER_SAVE:
  case TMR_SR_CONFIGURATION_EXTENDED_EPC:
  case TMR_SR_CONFIGURATION_SAFETY_ANTENNA_CHECK:
  case TMR_SR_CONFIGURATION_SAFETY_TEMPERATURE_CHECK:
  case TMR_SR_CONFIGURATION_RECORD_HIGHEST_RSSI:
  case TMR_SR_CONFIGURATION_RSSI_IN_DBM:
    SETU8(msg, i, *(bool *)value ? 1 : 0);
    break;

  default:
    return TMR_ERROR_NOT_FOUND;
  }
  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
}

/**
 * Handles license key to enable protocol 
 *
 * @param reader The reader
 * @param key license key
 * @param key_len Length of license key or the key array
*/


TMR_Status TMR_SR_cmdSetProtocolLicenseKey(TMR_Reader *reader, uint8_t key[], int key_len,uint32_t *retData)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_PROTOCOL_LICENSEKEY);
  SETU8(msg, i, (uint8_t)0x01);

  memcpy(msg+i, key, key_len);
  i+= key_len;

  msg[1] = i - 3; /* Install length */

  return TMR_SR_send(reader, msg);
  *retData= GETU32AT(msg,12);

}

TMR_Status
TMR_SR_cmdSetProtocolConfiguration(TMR_Reader *reader, TMR_TagProtocol protocol,
                                   TMR_SR_ProtocolConfiguration key,
                                   const void *value)
{
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  uint8_t BLF = 0;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_PROTOCOL_PARAM);
  SETU8(msg, i, protocol);
  if (TMR_TAG_PROTOCOL_GEN2 == key.protocol)
  {
    SETU8(msg, i, key.u.gen2);
    switch (key.u.gen2)
    {
    case TMR_SR_GEN2_CONFIGURATION_SESSION:
      SETU8(msg, i, *(TMR_GEN2_Session *)value);
      break;

    case TMR_SR_GEN2_CONFIGURATION_TAGENCODING:
      SETU8(msg, i, *(TMR_GEN2_TagEncoding *)value);
      break;

    case TMR_SR_GEN2_CONFIGURATION_LINKFREQUENCY:
      switch (*(int *)value)
      {
      case 40:
        BLF = 0x03;
        break;
      case 250:
        BLF = 0x00;
        break;
      case 400:
        BLF = 0x02;
        break;
      case 640:
        BLF = 0x04;
        break;
      default:
        return TMR_ERROR_INVALID;
      }
      SETU8(msg, i, BLF);
      break;

    case TMR_SR_GEN2_CONFIGURATION_TARI:
      SETU8(msg, i, *(TMR_GEN2_Tari *)value);
      break;
      
    case TMR_SR_GEN2_CONFIGURATION_TARGET:
      switch (*(TMR_GEN2_Target *)value)
      {
      case TMR_GEN2_TARGET_A:
        SETU16(msg, i, 0x0100);
        break;
      case TMR_GEN2_TARGET_B:
        SETU16(msg, i, 0x0101);
        break;
      case TMR_GEN2_TARGET_AB:
        SETU16(msg, i, 0x0000);
        break;
      case TMR_GEN2_TARGET_BA:
        SETU16(msg, i, 0x0001);
        break;
      default:
        return TMR_ERROR_INVALID;
      }
      break;
      
    case TMR_SR_GEN2_CONFIGURATION_Q:
    {
      const TMR_SR_GEN2_Q *q = value;
      if (q->type == TMR_SR_GEN2_Q_DYNAMIC)
      {
        SETU8(msg, i, 0);
      }
      else if (q->type == TMR_SR_GEN2_Q_STATIC)
      {
        SETU8(msg, i, 1);
        SETU8(msg, i, q->u.staticQ.initialQ);
      }
      else
      {
        return TMR_ERROR_INVALID;
      }
      break;
    }

    default:
      return TMR_ERROR_NOT_FOUND;
    }
  }
#ifdef TMR_ENABLE_ISO180006B
  else if (TMR_TAG_PROTOCOL_ISO180006B == key.protocol 
           || TMR_TAG_PROTOCOL_ISO180006B_UCODE == key.protocol)
  {
      switch (*(int *)value)
      {
      case 40:
        BLF = 0x00;
        break;
      case 160:
        BLF = 0x01;
        break;
      default:
        return TMR_ERROR_INVALID;
      }
    SETU8(msg, i, key.u.iso180006b);
    SETU8(msg, i, BLF);
  }
#endif /* TMR_ENABLE_ISO180006B */
  else
  {
    return TMR_ERROR_INVALID;
  }

  msg[1] = i - 3; /* Install length */
  return TMR_SR_send(reader, msg);
}


TMR_Status TMR_SR_cmdGetTxRxPorts(TMR_Reader *reader, TMR_SR_PortPair *ant)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;

  SETU8(msg, i, TMR_SR_OPCODE_GET_ANTENNA_PORT);
  SETU8(msg, i, 0); /* just configured ports */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  ant->txPort = msg[5];
  ant->rxPort = msg[6];
  
  return TMR_SUCCESS;
}

TMR_Status
TMR_SR_cmdAntennaDetect(TMR_Reader *reader, uint8_t *count,
                        TMR_SR_PortDetect *ports)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i, j, numAntennas;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_ANTENNA_PORT);
  SETU8(msg, i, 5); /* antenna detect option */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  numAntennas = (msg[1] - 1) / 2;
  for (i = 1, j = 0; i < msg[1] && j < *count; i += 2, j++)
  {
    ports[j].port = msg[i + 5];
    ports[j].detected = (msg[i + 6] == 1);
  }
  *count = j;

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetAntennaPortPowersAndSettlingTime(
  TMR_Reader *reader, uint8_t *count, TMR_SR_PortPowerAndSettlingTime *ports)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i, j, numAntennas;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_ANTENNA_PORT);
  SETU8(msg, i, 4); /* power and settling time option */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  numAntennas = (msg[1] - 1) / 7;

  for (i = 1, j = 0; i < msg[1] && j < *count; i += 7, j++)
  {
    ports[j].port = GETU8AT(msg, i + 5);
    ports[j].readPower = GETU16AT(msg, i + 6);
    ports[j].writePower = GETU16AT(msg, i + 8);
    ports[j].settlingTime = GETU16AT(msg, i + 10);
  }
  *count = j;

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetReadTxPower(TMR_Reader *reader, uint16_t *power)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_READ_TX_POWER);
  SETU8(msg, i, 0); /* just return power */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  *power = GETU16AT(msg, 6);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetReadTxPowerWithLimits(TMR_Reader *reader,
                                   TMR_SR_PowerWithLimits *power)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_READ_TX_POWER);
  SETU8(msg, i, 1); /* return limits */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  power->setPower = GETU16AT(msg, 6);
  power->maxPower = GETU16AT(msg, 8);
  power->minPower = GETU16AT(msg, 10);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetWriteTxPower(TMR_Reader *reader, uint16_t *power)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_WRITE_TX_POWER);
  SETU8(msg, i, 0); /* just return power */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  *power = GETU16AT(msg, 6);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetFrequencyHopTable(TMR_Reader *reader, uint8_t *count,
                               uint32_t *hopTable)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i, j, len;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_FREQ_HOP_TABLE);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  len = msg[1] / 4;
  for (j = 0; i < *count && j < len ; j++)
  {
    hopTable[j] = GETU32AT(msg, 5 + 4*j);
  }
  *count = len;

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetFrequencyHopTime(TMR_Reader *reader, uint32_t *hopTime)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_FREQ_HOP_TABLE);
  SETU8(msg, i, 1); /* get time */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  *hopTime = GETU32AT(msg, 6);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetGPIO(TMR_Reader *reader, uint8_t *count, bool *state)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i, j, len;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_USER_GPIO_INPUTS);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
    return ret;

  len = msg[1];
  if (len > *count)
    len = *count;
  for (j = 0; j < len ; j++)
    state[j] = (msg[5 + j] == 1);
  *count = len;

  return TMR_SUCCESS;
}

TMR_Status
TMR_SR_cmdGetGPIODirection(TMR_Reader *reader, uint8_t pin, bool *out)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_USER_GPIO_OUTPUTS);
  SETU8(msg, i, pin);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
    return ret;

  *out = (msg[6] == 1);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdSetGPIODirection(TMR_Reader *reader, uint8_t pin, bool out)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_SET_USER_GPIO_OUTPUTS);
  SETU8(msg, i, 1); /* Option flag */
  SETU8(msg, i, pin);
  SETU8(msg, i, (out == true) ? 1 : 0);
  SETU8(msg, i, 0); /* New value if output */
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
    return ret;

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetRegionConfiguration(TMR_Reader *reader,
                                 TMR_SR_RegionConfiguration key,
                                 void *value)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_REGION);
  SETU8(msg, i, 1);
  SETU8(msg, i, key);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  switch (key)
  {
  case TMR_SR_REGION_CONFIGURATION_LBT_ENABLED:
    *(bool *)value = (GETU8AT(msg, 8) == 1);
    break;
  default:
    return TMR_ERROR_NOT_FOUND;
  }

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetPowerMode(TMR_Reader *reader, TMR_SR_PowerMode *mode)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_POWER_MODE);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  *mode = GETU8AT(msg, 5);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetUserMode(TMR_Reader *reader, TMR_SR_UserMode *mode)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_USER_MODE);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  *mode = GETU8AT(msg, 5);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetReaderConfiguration(TMR_Reader *reader, TMR_SR_Configuration key,
                                 void *value)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_READER_OPTIONAL_PARAMS);
  SETU8(msg, i, 1); /* key-value form of command */
  SETU8(msg, i, key);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  switch (key)
  {
  case TMR_SR_CONFIGURATION_ANTENNA_CONTROL_GPIO:
    *(uint8_t *)value = GETU8AT(msg, 7);
    break;

  case TMR_SR_CONFIGURATION_UNIQUE_BY_ANTENNA:
  case TMR_SR_CONFIGURATION_UNIQUE_BY_DATA:
    *(bool *)value = (GETU8AT(msg, 7) == 0);
    break;
  case TMR_SR_CONFIGURATION_TRANSMIT_POWER_SAVE:
  case TMR_SR_CONFIGURATION_EXTENDED_EPC:
  case TMR_SR_CONFIGURATION_SAFETY_ANTENNA_CHECK:
  case TMR_SR_CONFIGURATION_SAFETY_TEMPERATURE_CHECK:
  case TMR_SR_CONFIGURATION_RECORD_HIGHEST_RSSI:
  case TMR_SR_CONFIGURATION_RSSI_IN_DBM:
    *(bool *)value = (GETU8AT(msg, 7) == 1);
    break;

  default:
    return TMR_ERROR_NOT_FOUND;
  }

  return TMR_SUCCESS;
}

TMR_Status
TMR_SR_cmdGetProtocolConfiguration(TMR_Reader *reader, TMR_TagProtocol protocol,
                                   TMR_SR_ProtocolConfiguration key,
                                   void *value)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_PROTOCOL_PARAM);
  SETU8(msg, i, protocol);
  if (TMR_TAG_PROTOCOL_GEN2 == key.protocol)
  {
    SETU8(msg, i, key.u.gen2);
  }
#ifdef TMR_ENABLE_ISO180006B
  else if (TMR_TAG_PROTOCOL_ISO180006B == key.protocol 
           || TMR_TAG_PROTOCOL_ISO180006B_UCODE == key.protocol)
  {
    SETU8(msg, i, key.u.iso180006b);
  }
#endif
  else
  {
    return TMR_ERROR_INVALID;
  }
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  if (TMR_TAG_PROTOCOL_GEN2 == key.protocol)
  {
    switch (key.u.gen2)
    {
    case TMR_SR_GEN2_CONFIGURATION_SESSION:
      *(TMR_GEN2_Session *)value = GETU8AT(msg, 7);
      break;

    case TMR_SR_GEN2_CONFIGURATION_TAGENCODING:
      *(TMR_GEN2_TagEncoding *)value = GETU8AT(msg, 7);
      break;

    case TMR_SR_GEN2_CONFIGURATION_LINKFREQUENCY:
      *(TMR_GEN2_LinkFrequency *)value = GETU8AT(msg, 7);
      break;

    case TMR_SR_GEN2_CONFIGURATION_TARI:
      *(TMR_GEN2_Tari *)value = GETU8AT(msg, 7);
      break;

    case TMR_SR_GEN2_CONFIGURATION_TARGET:
      {
        uint16_t target;

        target = GETU16AT(msg, 7);
        switch (target)
        {
        case 0x0100:
          *(TMR_GEN2_Target *)value = TMR_GEN2_TARGET_A;
          break;
        case 0x0101:
          *(TMR_GEN2_Target *)value = TMR_GEN2_TARGET_B;
          break;
        case 0x0000:
          *(TMR_GEN2_Target *)value = TMR_GEN2_TARGET_AB;
          break;
        case 0x0001:
          *(TMR_GEN2_Target *)value = TMR_GEN2_TARGET_BA;
          break;
        default:
          *(TMR_GEN2_Target *)value = TMR_GEN2_TARGET_INVALID;
        }
        break;
      }

    case TMR_SR_GEN2_CONFIGURATION_Q:
      {
        TMR_SR_GEN2_Q *q = value;

        q->type = GETU8AT(msg, 7);
        if (q->type == TMR_SR_GEN2_Q_DYNAMIC)
        {
          ; /* No further data to get */
        }
        else if (q->type == TMR_SR_GEN2_Q_STATIC)
        {
          q->u.staticQ.initialQ = GETU8AT(msg, 8);
        }
        break;
      }

    default:
      return TMR_ERROR_NOT_FOUND;
    }
  }
#ifdef TMR_ENABLE_ISO180006B
  else if (TMR_TAG_PROTOCOL_ISO180006B == key.protocol 
    || TMR_TAG_PROTOCOL_ISO180006B_UCODE == key.protocol)
  {
    *(TMR_ISO180006B_LinkFrequency *)value = GETU8AT(msg, 7);
  }
#endif /* TMR_ENABLE_ISO180006B */
  else
  {
    return TMR_ERROR_INVALID;
  }

  return TMR_SUCCESS;
}


TMR_Status TMR_SR_cmdMultipleProtocolSearch(TMR_Reader *reader,TMR_SR_OpCode op,TMR_TagProtocolList *protocols, TMR_TRD_MetadataFlag metadataFlags,TMR_SR_SearchFlag antennas, TMR_TagFilter **filter, uint16_t timeout, uint32_t *tagsFound)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;
  uint32_t j;
  uint16_t subTimeout;
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  *tagsFound = 0 ;

  i=2;

  SETU8(msg, i, TMR_SR_OPCODE_MULTI_PROTOCOL_TAG_OP);  //Opcode
  SETU16(msg, i, timeout); //command timeout
  SETU8(msg, i, (uint8_t)0x11);//TM Option, turns on metadata
  SETU16(msg, i, (uint16_t)metadataFlags);
  SETU8(msg, i, (uint8_t)op);//sub command opcode
  SETU16(msg, i, (uint16_t)0x0000);//search flags, only 0x0001 is supported

  subTimeout =(uint16_t)(timeout/(protocols->len));

  for (j=0;j<protocols->len;j++) // iterate through the protocol search list
  {
    int PLenIdx;

    TMR_TagProtocol subProtocol=protocols->list[j];
    SETU8(msg, i, (uint8_t)(subProtocol)); //protocol ID
    PLenIdx = i;
    SETU8(msg, i, 0); //PLEN

    switch(op)
    {
    case TMR_SR_OPCODE_READ_TAG_ID_SINGLE :
      {
        TMR_SR_msgSetupReadTagSingle(msg, &i, subProtocol,metadataFlags, filter[j], subTimeout);
        break;
      }
    case TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE:
      {
        TMR_SR_msgSetupReadTagMultipleWithMetadata(reader, msg, &i, subTimeout, antennas, metadataFlags ,filter[j], subProtocol, 0);
        break;
      }
    default :
      {
        return TMR_ERROR_INVALID_OPCODE;
        break;
      }
    }

    msg[PLenIdx]= i - PLenIdx - 2; //PLEN
    msg[1]=i - 3;

  }

  if (op == TMR_SR_OPCODE_READ_TAG_ID_SINGLE)
  {
    uint8_t opcode;

    sr->opCode = op;
    ret = TMR_SR_sendMessage(reader, msg, &opcode, timeout);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
    sr->tagsRemaining = 1;
    
    
  }

  if (op == TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE)
  {
    sr->opCode = op;
    if(sr->useStreaming)
    {
      uint8_t opcode;
      ret = TMR_SR_sendMessage(reader, msg, &opcode, timeout);
      if (TMR_SUCCESS != ret)
      {
        return ret;
      }
      sr->tagsRemaining=1;
    }
    else
    {
      ret = TMR_SR_send(reader, msg);
      if (TMR_SUCCESS != ret)
      {
        return ret;
      }
      *tagsFound = GETU32AT(msg , 9);
      sr->tagsRemaining = *tagsFound;
    }
  }

  return TMR_SUCCESS;

}

TMR_Status
TMR_SR_cmdGetAvailableProtocols(TMR_Reader *reader,
                                TMR_TagProtocolList *protocols)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_AVAILABLE_PROTOCOLS);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  protocols->len = 0;
  for (i = 0; i < msg[1] ; i += 2)
  {
    LISTAPPEND(protocols, GETU16AT(msg, 5 + i)); 
  }

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_cmdGetAvailableRegions(TMR_Reader *reader, TMR_RegionList *regions)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_AVAILABLE_REGIONS);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  regions->len = 0;
  for (i = 0; i < msg[1] ; i++)
  {
    LISTAPPEND(regions, GETU8AT(msg, 5 + i)); 
  }

  return TMR_SUCCESS;
}



TMR_Status
TMR_SR_cmdGetTemperature(TMR_Reader *reader, uint8_t *temp)
{
  TMR_Status ret;
  uint8_t msg[TMR_SR_MAX_PACKET_SIZE];
  uint8_t i;

  i = 2;
  SETU8(msg, i, TMR_SR_OPCODE_GET_TEMPERATURE);
  msg[1] = i - 3; /* Install length */

  ret = TMR_SR_send(reader, msg);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  *temp = msg[5];
  return TMR_SUCCESS;
}


static TMR_Status
filterbytes(TMR_TagProtocol protocol, const TMR_TagFilter *filter, 
            uint8_t *option, uint8_t *i, uint8_t *msg,
            uint32_t accessPassword, bool usePassword)
{
  int j;

  if (NULL == filter && 0 == accessPassword)
  {
    *option = 0;
    return TMR_SUCCESS;
  }

  if (TMR_TAG_PROTOCOL_GEN2 == protocol)
  {
    if (usePassword)
    {
      SETU32(msg, *i, accessPassword);
    }
    if (NULL == filter)
    {
      *option = TMR_SR_GEN2_SINGULATION_OPTION_USE_PASSWORD;
    }
    else if (TMR_FILTER_TYPE_GEN2_SELECT == filter->type)
    {
      const TMR_GEN2_Select *fp;

      fp = &filter->u.gen2Select;

      if (1 == fp->bank)
      {
        *option = TMR_SR_GEN2_SINGULATION_OPTION_SELECT_ON_ADDRESSED_EPC;
      }
      else /* select based on the bank */
      {
        *option = fp->bank;
      }

      if(true == fp->invert)
      {
        *option |= TMR_SR_GEN2_SINGULATION_OPTION_INVERSE_SELECT_BIT;
      }

      if (fp->maskBitLength > 255)
      {
        *option |= TMR_SR_GEN2_SINGULATION_OPTION_EXTENDED_DATA_LENGTH;
      }

      SETU32(msg, *i, fp->bitPointer);

      if (fp->maskBitLength > 255)
      {
        SETU8(msg, *i, (fp->maskBitLength >> 8) & 0xFF);
      }
      SETU8(msg, *i, fp->maskBitLength & 0xFF);

      if (*i + 1 + fp->maskBitLength/8 > TMR_SR_MAX_PACKET_SIZE)
      {
        return TMR_ERROR_TOO_BIG;
      }

      for(j = 0; j < fp->maskBitLength/8 ; j++)
      {
        SETU8(msg, *i, fp->mask[j]);
      }
    }
    else if (TMR_FILTER_TYPE_TAG_DATA == filter->type)
    {
      const TMR_TagData *fp;
      int bitCount;

      fp = &filter->u.tagData;
      bitCount = fp->epcByteCount * 8;

      /* select on the EPC */
      *option = 1;
      if (bitCount > 255)
      {
        *option |= TMR_SR_GEN2_SINGULATION_OPTION_EXTENDED_DATA_LENGTH;
        SETU8(msg, *i, (bitCount>>8) & 0xFF);
      }
      SETU8(msg, *i, (bitCount & 0xFF));

      if (*i + 1 + fp->epcByteCount > TMR_SR_MAX_PACKET_SIZE)
      {
        return TMR_ERROR_TOO_BIG;
      }

      for(j = 0 ; j < fp->epcByteCount ; j++)
      {
        SETU8(msg, *i, fp->epc[j]);
      }
    }
    else
    {
      return TMR_ERROR_INVALID;
    }
  }
#ifdef TMR_ENABLE_ISO180006B
  else if (TMR_TAG_PROTOCOL_ISO180006B == protocol)
  {
    if (option)
      *option = 1;
    if (NULL == filter)
    {
      /* Set up a match-anything filter, since it isn't the default */
      SETU8(msg, *i, TMR_ISO180006B_SELECT_OP_EQUALS);
      SETU8(msg, *i, 0); /* address */
      SETU8(msg, *i, 0); /* mask - don't compare anything */
      SETU32(msg, *i, 0); /* dummy tag ID bytes 0-3, not compared */
      SETU32(msg, *i, 0); /* dummy tag ID bytes 4-7, not compared */
      return TMR_SUCCESS;
    }
    else if (TMR_FILTER_TYPE_ISO180006B_SELECT == filter->type)
    {
      const TMR_ISO180006B_Select *fp;
      
      fp = &filter->u.iso180006bSelect;
      if (false == fp->invert)
      {
        SETU8(msg, *i, fp->op);
      }
      else
      {
        SETU8(msg, *i, fp->op | 4);
      }
      SETU8(msg, *i, fp->address);
      SETU8(msg, *i, fp->mask);
      for (j = 0 ; j < 8 ; j++)
      {
        SETU8(msg, *i, fp->data[j]);
      }
    }
    else if (TMR_FILTER_TYPE_TAG_DATA == filter->type)
    {
      const TMR_TagData *fp;
      uint8_t mask;

      fp = &filter->u.tagData;

      if (fp->epcByteCount > 8)
      {
        return TMR_ERROR_INVALID;
      }

      /* Convert the byte count to a MSB-based bit mask */
      mask = (0xff00 >> fp->epcByteCount) & 0xff;

      SETU8(msg, *i, TMR_ISO180006B_SELECT_OP_EQUALS);
      SETU8(msg, *i, 0); /* Address - EPC is at the start of memory */
      SETU8(msg, *i, mask);
      for (j = 0 ; j < fp->epcByteCount; j++)
      {
        SETU8(msg, *i, fp->epc[j]);
      }
      for ( ; j < 8 ; j++)
      {
        SETU8(msg, *i, 0); /* EPC data must be 8 bytes */
      }
    }
    else
    {
      return TMR_ERROR_INVALID;
    }
    
  }
#endif /* TMR_ENABLE_ISO180006B */
  else
  {
    return TMR_ERROR_INVALID;
  }

  return TMR_SUCCESS;
}

#endif /* TMR_ENABLE_SERIAL_READER */
