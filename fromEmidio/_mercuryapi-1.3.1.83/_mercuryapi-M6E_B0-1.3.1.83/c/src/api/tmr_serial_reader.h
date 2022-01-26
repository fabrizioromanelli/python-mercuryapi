/* ex: set tabstop=2 shiftwidth=2 expandtab cindent: */
#ifndef _TMR_SERIAL_READER_H
#define _TMR_SERIAL_READER_H
/**
 *  @file tmr_serial_reader.h
 *  @brief Mercury API - Serial Reader interface
 *  @author Nathan Williams
 *  @date 10/20/2009
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

#include "tmr_region.h"
#include "tmr_tag_protocol.h"
#include "tmr_serial_transport.h"

#define TMR_SR_MAX_PACKET_SIZE 256

/**
 * Defines the values for the parameter @c /reader/powerMode and the
 * return value and parameter to TMR_SR_cmdGetPowerMode and
 * TMR_SR_cmdSetPowerMode.
 */
typedef enum TMR_SR_PowerMode
{
  TMR_SR_POWER_MODE_MIN     = 0,
  TMR_SR_POWER_MODE_FULL    = 0,
  TMR_SR_POWER_MODE_MINSAVE = 1,
  TMR_SR_POWER_MODE_MEDSAVE = 2,
  TMR_SR_POWER_MODE_MAXSAVE = 3,
  TMR_SR_POWER_MODE_MAX     = TMR_SR_POWER_MODE_MAXSAVE,
  TMR_SR_POWER_MODE_INVALID = TMR_SR_POWER_MODE_MAXSAVE + 1,
} TMR_SR_PowerMode;

/** 
Operation Options for cmdSetUserProfile 
*/
typedef enum TMR_SR_SetUserProfileOption
{          
/**  Save operation */
TMR_SR_SAVE = 0x01,     
/**  Restore operation */
TMR_SR_RESTORE = 0x02, 
/**  Verify operation */
TMR_SR_VERIFY = 0x03,  
/** Clear operation  */
TMR_SR_CLEAR = 0x04,           
}TMR_SR_SetUserProfileOption; 

/**
Congfig key for cmdSetUserProfile
*/
typedef enum TMR_SR_SetUserProfileKey
{
  /** All Configuration */
TMR_SR_ALL=0x01,   
}TMR_SR_SetUserProfileKey;

/** 
The config values for cmdSetUserProfile
*/
typedef enum TMR_SR_SetUserProfileValue
{
/** Firmware default configurations */
TMR_SR_FIRMWARE_DEFAULT=0x00,
/** Custom configurations */
TMR_SR_CUSTOM_CONFIGURATION=0x01,      
}TMR_SR_SetUserProfileValue;

/**
 * Defines the values for the parameter /reader/userMode and the
 * return value and parameter to TMR_SR_cmdGetUserMode() and
 * TMR_SR_cmdSetUserMode.
 */
typedef enum TMR_SR_UserMode
{
  TMR_SR_USER_MODE_MIN      = 0,
  TMR_SR_USER_MODE_UNSPEC   = 0,
  TMR_SR_USER_MODE_PRINTER  = 1,
  TMR_SR_USER_MODE_CONVEYOR = 2,
  TMR_SR_USER_MODE_PORTAL   = 3,
  TMR_SR_USER_MODE_HANDHELD = 4,
  TMR_SR_USER_MODE_MAX      = TMR_SR_USER_MODE_HANDHELD,
  TMR_SR_USER_MODE_INVALID  = TMR_SR_USER_MODE_MAX + 1,
}TMR_SR_UserMode;

/**
 * This represents the types of Q algorithms avaliable on the reader.
 */
typedef enum TMR_SR_GEN2_QType
{
  TMR_SR_GEN2_Q_MIN     = 0,
  TMR_SR_GEN2_Q_DYNAMIC = 0,
  TMR_SR_GEN2_Q_STATIC  = 1,
  TMR_SR_GEN2_Q_MAX     = TMR_SR_GEN2_Q_STATIC,
  TMR_SR_GEN2_Q_INVALID = TMR_SR_GEN2_Q_MAX + 1,
} TMR_SR_GEN2_QType;

/** Configuration of the static-Q algorithm. */
typedef struct TMR_SR_GEN2_QStatic
{
  /** The initial Q value to use. */
  uint8_t initialQ;
} TMR_SR_GEN2_QStatic;

/**
 * This represents a Q algorithm on the reader.
 */
typedef struct TMR_SR_GEN2_Q
{
  /** The type of Q algorithm. */
  TMR_SR_GEN2_QType type;
  union
  {
    /** Configuration of the static-Q algorithm. */
    TMR_SR_GEN2_QStatic staticQ;
  }u;
} TMR_SR_GEN2_Q;


/** An antenna port with an associated uint16_t value. */
typedef struct TMR_PortValue
{
  /** The port number */
  uint8_t port;
  /** The value */
  uint16_t value;
} TMR_PortValue;

/** List of TMR_PortValue values */
typedef struct TMR_PortValueList
{
  /** The array of values */
  TMR_PortValue *list;
  /** The number of entries there is space for in the array */
  uint8_t max;
  /** The number of entries in the list - may be larger than max, indicating truncated data. */
  uint8_t len;
} TMR_PortValueList;


/**
 * Mapping between an arbitrary "antenna" number and a TX/RX port
 * pair. Not all TX/RX pairings are valid for a device.
 */
typedef struct TMR_AntennaMap
{
  /** The antenna number - an arbitrary value. */
  uint8_t antenna;
  /** The device antenna port to use for transmission. */
  uint8_t txPort;
  /** The device antenna port to use for reception. */
  uint8_t rxPort;
} TMR_AntennaMap;

/** List of antenna mappings */
typedef struct TMR_AntennaMapList
{
  /** The array of values */
  TMR_AntennaMap *list;
  /** The number of entries there is space for in the array */
  uint8_t max;
  /** The number of entries in the list - may be larger than max, indicating truncated data. */
  uint8_t len;
} TMR_AntennaMapList;

/**
 * The version structure returned from cmdVersion().
 */
typedef struct TMR_SR_VersionInfo
{
  /** Bootloader version, as four 8-bit numbers */
  uint8_t bootloader[4];
  /** Hardware version. Opaque format */
  uint8_t hardware[4];
  /** Date app firmware was built, as BCD YYYYMMDD */
  uint8_t fwDate[4];
  /** App firmware version, as four 8-bit numbers */
  uint8_t fwVersion[4];
  /** Bitmask of the protocols supported by the device (indexed by TMR_TagProtocol values minus one).  */
  uint32_t protocols;
} TMR_SR_VersionInfo;

/**
 * The serial reader structure.
 */
typedef struct TM_SR_SerialReader
{
  /** @privatesection */

  /* Serial transport information */
  TMR_SR_SerialTransport transport;
  union
  {
#ifdef TMR_ENABLE_SERIAL_TRANSPORT_NATIVE
    TMR_SR_SerialPortNativeContext nativeContext;
#endif
#ifdef TMR_ENABLE_SERIAL_TRANSPORT_LLRP
    TMR_SR_LlrpEapiTransportContext llrpContext;
#endif
  } transportContext;

  /* User-configurable values */
  uint32_t baudRate;
  TMR_AntennaMapList *txRxMap;
  TMR_GEN2_Password gen2AccessPassword;
  uint32_t transportTimeout;
  uint32_t commandTimeout;
  TMR_Region regionId;

  /* Static storage for the default map */
  TMR_AntennaMap staticTxRxMapData[TMR_SR_MAX_ANTENNA_PORTS];
  TMR_AntennaMapList staticTxRxMap;

  /* Mostly-fixed information about the connected reader */
  TMR_SR_VersionInfo versionInfo;
  uint32_t portMask;
  bool useStreaming;

  /* Cached values */
  TMR_SR_PowerMode powerMode;
  TMR_TagProtocol currentProtocol;
  int8_t gpioDirections;

#define TMR_PARAMWORDS ((1 + TMR_PARAM_MAX + 31) / 32)
  /* Large bitmask that stores whether each parameter's presence
   * is known or not.
   */
  uint32_t paramConfirmed[TMR_PARAMWORDS];
  /* Large bitmask that, if the corresponding bit in paramConfirmed is set,
   * stores whether each parameter is present or not.
   */
  uint32_t paramPresent[TMR_PARAMWORDS];

  /* Temporary storage during a read and subsequent fetch of tags */
  uint32_t readTimeLow, readTimeHigh;
  uint32_t searchTimeoutMs;
  
  /* Number of tags reported by module read command.
   * In streaming mode, exact quantity is unknown, so use
   * 0 if stream has ended, non-zero if end-of-stream has
   * not yet been detected. */
  int tagsRemaining;
  /* Buffer tag records fetched from module but not yet passed to caller. */
  uint8_t bufResponse[TMR_SR_MAX_PACKET_SIZE];
  /* bufResopnse read index */
  uint8_t bufPointer;
  /* Number of tag records in buffer but not yet passed to caller */
  uint8_t tagsRemainingInBuffer;
  /*TMR opCode*/
  uint8_t opCode;
  /*Gen2 Q Value from previous tagop */
  TMR_SR_GEN2_Q oldQ;
  /*Gen2 WriteMode*/
  TMR_GEN2_WriteMode writeMode;

} TMR_SR_SerialReader;


TMR_Status TMR_SR_connect(struct TMR_Reader *reader);
TMR_Status TMR_SR_destroy(struct TMR_Reader *reader);
TMR_Status TMR_SR_read(struct TMR_Reader *reader, uint32_t timeoutMs, int32_t *tagCount);
TMR_Status TMR_SR_hasMoreTags(struct TMR_Reader *reader);
TMR_Status TMR_SR_getNextTag(struct TMR_Reader *reader, TMR_TagReadData *read);
TMR_Status TMR_SR_writeTag(struct TMR_Reader *reader, const TMR_TagFilter *filter, const TMR_TagData *data);
TMR_Status TMR_SR_killTag(struct TMR_Reader *reader, const TMR_TagFilter *filter, const TMR_TagAuthentication *auth);
TMR_Status TMR_SR_lockTag(struct TMR_Reader *reader, const TMR_TagFilter *filter, TMR_TagLockAction *action);
TMR_Status TMR_SR_tagop_execute(TMR_Reader *reader, TMR_Tagop *tagop);
TMR_Status TMR_SR_readTagMemBytes(struct TMR_Reader *reader, const TMR_TagFilter *filter,
                              uint32_t bank, uint32_t address,
                              uint16_t count, uint8_t data[]);
TMR_Status TMR_SR_readTagMemWords(struct TMR_Reader *reader, const TMR_TagFilter *filter,
                              uint32_t bank, uint32_t address,
                              uint16_t count, uint16_t *data);
TMR_Status TMR_SR_writeTagMemBytes(struct TMR_Reader *reader, const TMR_TagFilter *filter,
                               uint32_t bank, uint32_t address,
                               uint16_t count, const uint8_t data[]);
TMR_Status TMR_SR_writeTagMemWords(struct TMR_Reader *reader, const TMR_TagFilter *filter,
                               uint32_t bank, uint32_t address,
                               uint16_t count, const uint16_t data[]);
TMR_Status TMR_SR_gpoSet(struct TMR_Reader *reader, uint8_t count, const TMR_GpioPin state[]);
TMR_Status TMR_SR_gpiGet(struct TMR_Reader *reader, uint8_t *count, TMR_GpioPin state[]);
TMR_Status TMR_SR_firmwareLoad(TMR_Reader *reader, void *cookie,
                               TMR_FirmwareDataProvider provider);


/**
 * Initialize a serial reader. The reader->u.serialReader.transport
 * structure must be initialized before calling this.
 */
TMR_Status TMR_SR_SerialReader_init(TMR_Reader *reader);



#endif /* _TMR_SERIAL_READER_H */
