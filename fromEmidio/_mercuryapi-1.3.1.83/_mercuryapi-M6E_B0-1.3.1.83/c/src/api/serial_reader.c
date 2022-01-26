/**
 *  @file serial_reader.c
 *  @brief Mercury API - serial reader high level implementation
 *  @author Nathan Williams
 *  @date 10/28/2009
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
#include <stdio.h>
#include <string.h>

#include "tm_reader.h"
#include "serial_reader_imp.h"
#include "tmr_utils.h"
#include "osdep.h"

#define HASPORT(mask, port) ((1 << ((port)-1)) & (mask))

#ifdef TMR_ENABLE_SERIAL_READER

static TMR_Status
initTxRxMapFromPorts(TMR_Reader *reader)
{
  TMR_Status ret;
  TMR_SR_PortDetect ports[TMR_SR_MAX_ANTENNA_PORTS];
  uint8_t i, numPorts;
  TMR_SR_SerialReader *sr;

  numPorts = numberof(ports);
  sr = &reader->u.serialReader;

  /* Need number of ports to set up Tx-Rx map */
  ret = TMR_SR_cmdAntennaDetect(reader, &numPorts, ports);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  sr->portMask = 0;
  for (i = 0; i < numPorts; i++)
  {
    sr->portMask |= 1 << (ports[i].port - 1);
    sr->staticTxRxMapData[i].antenna = ports[i].port;
    sr->staticTxRxMapData[i].txPort = ports[i].port;
    sr->staticTxRxMapData[i].rxPort = ports[i].port;

    if (0 == reader->tagOpParams.antenna && ports[i].detected)
    {
      reader->tagOpParams.antenna = ports[i].port;
    }
  }
  sr->staticTxRxMap.max = TMR_SR_MAX_ANTENNA_PORTS;
  sr->staticTxRxMap.len = numPorts;
  sr->staticTxRxMap.list = sr->staticTxRxMapData;
  sr->txRxMap = &sr->staticTxRxMap;

  return TMR_SUCCESS;
}

static TMR_Status
TMR_SR_boot(TMR_Reader *reader, uint32_t currentBaudRate)
{
  TMR_Status ret;
  uint8_t program;
  bool boolval;
  TMR_SR_SerialReader *sr;
  TMR_SR_SerialTransport *transport;
  int i;

  ret = TMR_SUCCESS;
  sr = &reader->u.serialReader;
  transport = &sr->transport;

  /* Get current program */
  ret = TMR_SR_cmdGetCurrentProgram(reader, &program);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  /* If bootloader, enter app */
  if ((program & 0x3) == 1)
  {
    TMR_SR_cmdBootFirmware(reader); /* No error check here */
  }

  /* Initialize cached power mode value */
  /* Should read power mode as soon as possible.
   * Default mode assumes module is in deep sleep and
   * adds a lengthy "wake-up preamble" to every command.
   */
  if (sr->powerMode == TMR_SR_POWER_MODE_INVALID)
  {
    ret = TMR_paramGet(reader, TMR_PARAM_POWERMODE, &sr->powerMode);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
  }

  if (sr->baudRate != currentBaudRate)
  {
    /* Bring baud rate up to the parameterized value */
    ret = TMR_SR_cmdSetBaudRate(reader, sr->baudRate);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }

    ret = transport->setBaudRate(transport, sr->baudRate);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
  }

  ret = TMR_SR_cmdVersion(reader, &sr->versionInfo);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  /* If we need to check the version information for something operational,
     this is the place to do it. */
  sr->gpioDirections = -1; /* Needs fetching */
  sr->useStreaming = (TMR_SR_MODEL_M6E == sr->versionInfo.hardware[0]);
  sr->useStreaming = false; //fix bug 1745 in the current release temporarily

  /* Initialize the paramPresent and paramConfirmed bits. */
  /* This block is expected to be collapsed by the compiler into a
   * small number of constant-value writes into the sr->paramPresent
   * array.
   */
  for (i = 0 ; i < TMR_PARAMWORDS; i++)
  {
    sr->paramPresent[i] = 0;
  }

  BITSET(sr->paramPresent, TMR_PARAM_BAUDRATE);
  BITSET(sr->paramPresent, TMR_PARAM_COMMANDTIMEOUT);
  BITSET(sr->paramPresent, TMR_PARAM_TRANSPORTTIMEOUT);
  BITSET(sr->paramPresent, TMR_PARAM_POWERMODE);
  BITSET(sr->paramPresent, TMR_PARAM_USERMODE);
  BITSET(sr->paramPresent, TMR_PARAM_ANTENNA_CHECKPORT);
  BITSET(sr->paramPresent, TMR_PARAM_ANTENNA_PORTLIST);
  BITSET(sr->paramPresent, TMR_PARAM_ANTENNA_CONNECTEDPORTLIST);
  BITSET(sr->paramPresent, TMR_PARAM_ANTENNA_PORTSWITCHGPOS);
  BITSET(sr->paramPresent, TMR_PARAM_ANTENNA_SETTLINGTIMELIST);
  BITSET(sr->paramPresent, TMR_PARAM_ANTENNA_TXRXMAP);
  BITSET(sr->paramPresent, TMR_PARAM_GPIO_INPUTLIST);
  BITSET(sr->paramPresent, TMR_PARAM_GPIO_OUTPUTLIST);
  BITSET(sr->paramPresent, TMR_PARAM_GEN2_ACCESSPASSWORD);
  BITSET(sr->paramPresent, TMR_PARAM_GEN2_Q);
  BITSET(sr->paramPresent, TMR_PARAM_GEN2_TAGENCODING);
  BITSET(sr->paramPresent, TMR_PARAM_GEN2_SESSION);
  BITSET(sr->paramPresent, TMR_PARAM_GEN2_TARGET);
  BITSET(sr->paramPresent, TMR_PARAM_READ_ASYNCOFFTIME);
  BITSET(sr->paramPresent, TMR_PARAM_READ_ASYNCONTIME);
  BITSET(sr->paramPresent, TMR_PARAM_READ_PLAN);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_ENABLEPOWERSAVE);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_POWERMAX);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_POWERMIN);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_PORTREADPOWERLIST);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_PORTWRITEPOWERLIST);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_READPOWER);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_WRITEPOWER);
  BITSET(sr->paramPresent, TMR_PARAM_RADIO_TEMPERATURE);
  BITSET(sr->paramPresent, TMR_PARAM_TAGREADDATA_RECORDHIGHESTRSSI);
  BITSET(sr->paramPresent, TMR_PARAM_TAGREADDATA_REPORTRSSIINDBM);
  BITSET(sr->paramPresent, TMR_PARAM_TAGREADDATA_UNIQUEBYANTENNA);
  BITSET(sr->paramPresent, TMR_PARAM_TAGREADDATA_UNIQUEBYDATA);
  BITSET(sr->paramPresent, TMR_PARAM_TAGOP_ANTENNA);
  BITSET(sr->paramPresent, TMR_PARAM_TAGOP_PROTOCOL);
  BITSET(sr->paramPresent, TMR_PARAM_VERSION_HARDWARE);
  BITSET(sr->paramPresent, TMR_PARAM_VERSION_MODEL);
  BITSET(sr->paramPresent, TMR_PARAM_VERSION_SOFTWARE);
  BITSET(sr->paramPresent, TMR_PARAM_VERSION_SUPPORTEDPROTOCOLS);
  BITSET(sr->paramPresent, TMR_PARAM_REGION_ID);
  BITSET(sr->paramPresent, TMR_PARAM_REGION_SUPPORTEDREGIONS);
  BITSET(sr->paramPresent, TMR_PARAM_REGION_HOPTABLE);
  BITSET(sr->paramPresent, TMR_PARAM_REGION_HOPTIME);
  BITSET(sr->paramPresent, TMR_PARAM_REGION_LBT_ENABLE);

  for (i = 0 ; i < TMR_PARAMWORDS; i++)
  {
    sr->paramConfirmed[i] = sr->paramPresent[i];
  }

  /* Set extended EPC */
  boolval = true;
  ret = TMR_SR_cmdSetReaderConfiguration(reader,
                                         TMR_SR_CONFIGURATION_EXTENDED_EPC,
                                         &boolval);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  /* Set region if user set the param */
  if (TMR_REGION_NONE != sr->regionId)
  {
    ret = TMR_SR_cmdSetRegion(reader, sr->regionId);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
  }

  reader->tagOpParams.protocol = TMR_TAG_PROTOCOL_GEN2; /* default */
  reader->tagOpParams.antenna = 0;

  ret = initTxRxMapFromPorts(reader);
  
  return ret;
}

TMR_Status
TMR_SR_connect(TMR_Reader *reader)
{
  TMR_Status ret;
  static uint32_t rates[] = { 9600, 115200, 921600, 19200, 38400, 57600,
                              230400, 460800};
  uint32_t rate;
  TMR_SR_SerialReader *sr;
  TMR_SR_SerialTransport *transport;
  int i;
  
  ret = TMR_SUCCESS;
  sr = &reader->u.serialReader;
  transport = &reader->u.serialReader.transport;

  ret = transport->open(transport);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  /* Make contact at some baud rate */
  for (i = 0; i <= numberof(rates); i++)
  {
    if (0 == i)
    {
      rate = sr->baudRate; /* Try this first */
    }
    else
    {
      rate = rates[i-1];
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

    ret = TMR_SR_cmdVersion(reader, &(reader->u.serialReader.versionInfo));

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
  if (i == numberof(rates))
  {
    return TMR_ERROR_TIMEOUT;
  }
  reader->connected = true;

  /* Boot */
  ret = TMR_SR_boot(reader, rate);

  return ret;
}

TMR_Status
TMR_SR_destroy(TMR_Reader *reader)
{
  TMR_SR_SerialTransport *transport;

  transport = &reader->u.serialReader.transport;

  transport->shutdown(transport);
  reader->connected = false;

  return TMR_SUCCESS;
}

static TMR_Status
autoDetectAntennaList(struct TMR_Reader *reader)
{
  TMR_Status ret;
  TMR_SR_PortDetect ports[TMR_SR_MAX_ANTENNA_PORTS];
  TMR_SR_PortPair searchList[TMR_SR_MAX_ANTENNA_PORTS];
  uint8_t i, listLen, numPorts;
  uint16_t j;
  TMR_AntennaMapList *map;
    
  ret = TMR_SUCCESS;
  map = reader->u.serialReader.txRxMap;

  /* 1. Detect current set of antennas */
  numPorts = TMR_SR_MAX_ANTENNA_PORTS;
  ret = TMR_SR_cmdAntennaDetect(reader, &numPorts, ports);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  /* 2. Set antenna list based on detected antennas (Might be clever
   * to cache this and not bother sending the set-list command
   * again, but it's more code and data space).
   */
  for (i = 0, listLen = 0; i < numPorts; i++)
  {
    if (ports[i].detected)
    {
      /* Ensure that the port exists in the map */
      for (j = 0; j < map->len; j++)
        if (ports[i].port == map->list[j].txPort)
        {
          searchList[listLen].txPort = map->list[j].txPort;
          searchList[listLen].rxPort = map->list[j].rxPort;
          listLen++;
          break;
        }
    }
  }
  if (0 == listLen) /* No ports auto-detected */
  {
    return TMR_ERROR_NO_ANTENNA;
  }
  ret = TMR_SR_cmdSetAntennaSearchList(reader, listLen, searchList);
  
  return ret;
}

static TMR_Status
setAntennaList(struct TMR_Reader *reader, TMR_uint8List *antennas)
{
  TMR_SR_PortPair searchList[16];
  uint16_t i, j, listLen;
  TMR_AntennaMapList *map;

  map = reader->u.serialReader.txRxMap;

  /** @todo cache the set list and don't reset it if it hasn't changed */
  listLen = 0;
  for (i = 0; i < antennas->len ; i++)
  {
    for (j = 0; j < map->len; j++)
    {
      if (antennas->list[i] == map->list[j].antenna)
      {
        searchList[listLen].txPort = map->list[j].txPort;
        searchList[listLen].rxPort = map->list[j].rxPort;
        listLen++;
        break;
      }
    }
  }
  return TMR_SR_cmdSetAntennaSearchList(reader,(uint8_t)listLen, searchList);
}

static TMR_Status
setProtocol(struct TMR_Reader *reader, TMR_TagProtocol protocol)
{
  TMR_Status ret;

  ret = TMR_SR_cmdSetProtocol(reader, protocol);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  /* Set extended EPC -- This bit is reset when the protocol changes */
  {
    bool boolval;
    boolval = true;
    ret = TMR_SR_cmdSetReaderConfiguration(reader,
					   TMR_SR_CONFIGURATION_EXTENDED_EPC,
					   &boolval);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
  }

  reader->u.serialReader.currentProtocol = protocol;

  return TMR_SUCCESS;
}

static TMR_Status
prepForSearch(TMR_Reader *reader, TMR_uint8List *antennaList)
{
  TMR_Status ret;
  if (antennaList->len == 0)
  {
    ret = autoDetectAntennaList(reader);
  }
  else
  {
    ret = setAntennaList(reader, antennaList);
  }
  return ret;
}

static TMR_Status
TMR_SR_read_internal(struct TMR_Reader *reader, uint32_t timeoutMs,
                     int32_t *tagCount, TMR_ReadPlan *rp)
{
  TMR_Status ret = TMR_SUCCESS;
  TMR_SR_SerialReader *sr;
  TMR_SR_MultipleStatus multipleStatus;
  uint32_t count, elapsed;
  uint32_t readTimeMs, starttimeLow, starttimeHigh;
  TMR_uint8List *antennaList = NULL;

  sr = &reader->u.serialReader;

  if (TMR_READ_PLAN_TYPE_MULTI == rp->type)
  {
    int i;
    TMR_TagProtocolList p;
    TMR_TagProtocolList *protocolList = &p;
    TMR_TagFilter *filters[TMR_MAX_SERIAL_MULTIPROTOCOL_LENGTH];
    TMR_TagProtocol protocols[TMR_MAX_SERIAL_MULTIPROTOCOL_LENGTH];


    if (TMR_MAX_SERIAL_MULTIPROTOCOL_LENGTH < rp->u.multi.planCount)
    {
      return TMR_ERROR_TOO_BIG ;
    }

    protocolList->len = rp->u.multi.planCount;
    protocolList->max = rp->u.multi.planCount;


    protocolList->list = protocols;

    for (i = 0; i < rp->u.multi.planCount; i++)
    {
      protocolList->list[i] = rp->u.multi.plans[i]->u.simple.protocol;
      filters[i]= rp->u.multi.plans[i]->u.simple.filter; 
    }

    if (rp->u.multi.totalWeight == 0)
    {
      TMR_SR_SearchFlag antennas = TMR_SR_SEARCH_FLAG_CONFIGURED_LIST;
      antennas |= ((reader->u.serialReader.useStreaming)? TMR_SR_SEARCH_FLAG_TAG_STREAMING : 0);
      antennaList = &(((TMR_SimpleReadPlan*)(rp->u.multi.plans[0]))->antennas);
      ret = prepForSearch(reader, antennaList);
      if (TMR_SUCCESS != ret)
      {
        return ret;
      }
      ret = TMR_SR_cmdMultipleProtocolSearch(reader, 
        TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE,
        protocolList, TMR_TRD_METADATA_FLAG_ALL, 
        antennas,
        filters, 
        (uint16_t)timeoutMs, &count);
      if (NULL != tagCount)
      {
        *tagCount += count;
      }
      return ret;
    }

  }

  if (TMR_READ_PLAN_TYPE_SIMPLE == rp->type)
  {
    antennaList = &rp->u.simple.antennas;
  }
  else if (TMR_READ_PLAN_TYPE_MULTI == rp->type)
  {
    uint32_t subTimeout;
    int i;

    for (i = 0; i < rp->u.multi.planCount; i++)
    {
      subTimeout = rp->u.multi.plans[i]->weight * timeoutMs 
        / rp->u.multi.totalWeight;
      ret = TMR_SR_read_internal(reader, subTimeout, tagCount, 
        rp->u.multi.plans[i]);
      if (TMR_SUCCESS != ret && TMR_ERROR_NO_TAGS_FOUND != ret)
      {
        return ret;
      }
    }
    return ret;
  }
  else
  {
    return TMR_ERROR_INVALID;
  }

  /* At this point we're guaranteed to have a simple read plan */
  ret = prepForSearch(reader, antennaList);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }


  /* Set protocol to that specified by the read plan. */
  if (reader->u.serialReader.currentProtocol != rp->u.simple.protocol)
  {
    reader->u.serialReader.currentProtocol = rp->u.simple.protocol;
    ret = setProtocol(reader, rp->u.simple.protocol);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
  }

  /* Cache the read time so it can be put in tag read data later */
  tm_gettime_consistent(&starttimeHigh, &starttimeLow);
  sr->readTimeHigh = starttimeHigh;
  sr->readTimeLow = starttimeLow;

  /* Cache search timeout for later call to streaming receive */
  sr->searchTimeoutMs = timeoutMs;

  elapsed = tm_time_subtract(tmr_gettime_low(), starttimeLow);
  while (elapsed < timeoutMs)
  {
    readTimeMs = timeoutMs - elapsed;
    if (readTimeMs > 65535)
    {
      readTimeMs = 65535;
    }

    if (NULL == rp->u.simple.tagop)
    {
      ret = TMR_SR_cmdReadTagMultiple(reader,(uint16_t)readTimeMs,
        TMR_SR_SEARCH_FLAG_CONFIGURED_LIST,
        rp->u.simple.filter,
        rp->u.simple.protocol,
        &count);
    }
    else
    {
      uint8_t msg[256];
      uint8_t i, lenbyte;

      i = 2;
      ret = TMR_SR_msgSetupReadTagMultiple(reader,
        msg, &i, (uint16_t)readTimeMs, TMR_SR_SEARCH_FLAG_CONFIGURED_LIST
        | TMR_SR_SEARCH_FLAG_EMBEDDED_COMMAND,
        rp->u.simple.filter, rp->u.simple.protocol,
        sr->gen2AccessPassword);

      SETU8(msg, i, 1);  /* Embedded command count */
      lenbyte = i++;

      switch (rp->u.simple.tagop->type)
      {
      case TMR_TAGOP_GEN2_READDATA:
        {
          TMR_Tagop_GEN2_ReadData *args;

          args = &rp->u.simple.tagop->u.gen2_readData;
          TMR_SR_msgAddGEN2DataRead(msg, &i, 0, args->bank, args->wordAddress,
            args->len);
          break;
        }
      case TMR_TAGOP_GEN2_WRITEDATA:
        {
          TMR_Tagop_GEN2_WriteData *args;
          int idx ;

          args = &rp->u.simple.tagop->u.gen2_writeData;
          TMR_SR_msgAddGEN2DataWrite(msg, &i, 0, args->bank, args->wordAddress);

          for(idx = 0 ; idx< args->data.len; idx++)
          {
            msg[i++]= (args->data.list[idx]>>8) & 0xFF;
            msg[i++]= (args->data.list[idx]>>0) & 0xFF;
          }
          break;
        }
      case TMR_TAGOP_GEN2_LOCK:
        {
          TMR_Tagop_GEN2_Lock *args;

          args = &rp->u.simple.tagop->u.gen2_lock;
          TMR_SR_msgAddGEN2LockTag(msg, &i, 0, args->mask, args->action, 0);
          break;
        }
      case TMR_TAGOP_GEN2_KILL:
        {
          TMR_Tagop_GEN2_Kill *args;

          args = &rp->u.simple.tagop->u.gen2_kill;
          TMR_SR_msgAddGEN2KillTag(msg, &i, 0, args->password);
          break;
        }
      case TMR_TAGOP_GEN2_BLOCKWRITE:
        {
          TMR_Tagop_GEN2_BlockWrite *args;
          args = &rp->u.simple.tagop->u.gen2_blockWrite;
          TMR_SR_msgAddGEN2BlockWrite(msg, &i, 0, args->bank, args->wordPtr, args->wordCount, args->data, args->accessPassword,NULL);
          break;
        }
      case TMR_TAGOP_GEN2_BLOCKPERMALOCK:
        {
          TMR_Tagop_GEN2_BlockPermaLock *args;
          args = &rp->u.simple.tagop->u.gen2_blockPermaLock;
          TMR_SR_msgAddGEN2BlockPermaLock(msg, &i, 0,args->readLock, args->bank, args->blockPtr, args->blockRange, args->mask, args->accessPassword, NULL);
          break;
        }
      case TMR_TAGOP_LIST:
        return TMR_ERROR_UNIMPLEMENTED; /* Module doesn't implement these */
      default:
        return TMR_ERROR_INVALID; /* Unknown tagop - internal error */
      }

      msg[lenbyte] = i - (lenbyte + 2); /* Install length of subcommand */
      msg[1] = i - 3; /* Install length */
      ret = TMR_SR_executeEmbeddedRead(reader, msg, (uint16_t)timeoutMs, &multipleStatus);
      count = multipleStatus.tagsFound;
    }

    if (TMR_ERROR_NO_TAGS_FOUND == ret)
    {
      count = 0;
      ret = TMR_SUCCESS;
    }
    else if (TMR_SUCCESS != ret)
    {
      return ret;
    }

    sr->tagsRemaining += count;
    if (NULL != tagCount)
    {
      *tagCount += count;
    }

    if (sr->useStreaming)
    {
      sr->tagsRemaining = 1;
      break;
    }
    else
    {
      elapsed = tm_time_subtract(tmr_gettime_low(), starttimeLow);
    }
  }

  return ret;
}

TMR_Status
TMR_SR_read(struct TMR_Reader *reader, uint32_t timeoutMs, int32_t *tagCount)
{
  TMR_Status ret;
  TMR_ReadPlan *rp;

  ret = TMR_SR_cmdClearTagBuffer(reader);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  reader->u.serialReader.tagsRemaining = 0;

  rp = reader->readParams.readPlan;

  if (tagCount)
  {
    *tagCount = 0;
  }
  
  return TMR_SR_read_internal(reader, timeoutMs, tagCount, rp);
}



TMR_Status
TMR_SR_hasMoreTags(struct TMR_Reader *reader)
{
  TMR_SR_SerialReader* sr;
  TMR_Status ret;

  sr = &reader->u.serialReader;

  if ((sr->useStreaming) && (0 == sr->tagsRemainingInBuffer))
  {
    uint8_t *msg;
    uint32_t timeoutMs;
    uint8_t response_type_pos;
    uint8_t response_type;
    
    msg = sr->bufResponse;
    timeoutMs = sr->searchTimeoutMs;

    ret = TMR_SR_receiveMessage(reader, msg, TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE, timeoutMs);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
    /* Need at least enough bytes to get to Response Type field */
    if ((msg[1] < 6) ||(msg[2] == 0x2F))
    {
      return TMR_ERROR_PARSE;
    }
    response_type_pos = (0x10 == (msg[5] & 0x10)) ? 10 : 8;

    response_type = msg[response_type_pos];
    switch (response_type)
    {
    case 0x01:
      /* Stream continues after this message */
      sr->tagsRemainingInBuffer = 1;
      sr->bufPointer = 11;
      return TMR_SUCCESS;
    case 0x00:
      /* Stream ends with this message */
      sr->tagsRemaining = 0;

      if (sr->oldQ.type != TMR_SR_GEN2_Q_INVALID)
      {
        ret = TMR_paramSet(reader, TMR_PARAM_GEN2_Q, &(sr->oldQ));
        if (TMR_SUCCESS != ret)
        {
          return ret;
        }
        sr->oldQ.type = TMR_SR_GEN2_Q_INVALID;
      }
      return TMR_ERROR_NO_TAGS;
    default:
      /* Unknown response type */
      return TMR_ERROR_PARSE;
    }
  }
  else
  {
    ret = (sr->tagsRemaining > 0) ? TMR_SUCCESS : TMR_ERROR_NO_TAGS;
    return ret;
  }
}

TMR_Status
TMR_SR_getNextTag(struct TMR_Reader *reader, TMR_TagReadData *read)
{
  TMR_SR_SerialReader *sr;
  TMR_Status ret;
  uint8_t *msg;
  uint8_t i;
  uint16_t flags = 0;
  uint32_t timeoutMs;
  uint8_t subResponseLen = 0;
  uint8_t crclen = 2 ;
  uint8_t epclen = 0;

  sr = &reader->u.serialReader;
  timeoutMs = sr->searchTimeoutMs;

  {
    msg = sr->bufResponse;

    if (sr->tagsRemaining == 0)
    {
      return TMR_ERROR_NO_TAGS;
    }

    if (sr->tagsRemainingInBuffer == 0)
    {
      /* Fetch the next set of tags from the reader */
      if (sr->useStreaming)
      {
        ret = TMR_SR_hasMoreTags(reader);
        if (TMR_SUCCESS != ret)
        {
          return ret;
        }
      }
      else
      {
        if (reader->u.serialReader.opCode == TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE)
        {
          i = 2;
          SETU8(msg, i, TMR_SR_OPCODE_GET_TAG_ID_BUFFER);
          SETU16(msg, i, TMR_TRD_METADATA_FLAG_ALL);
          SETU8(msg, i, 0); /* read options */
          msg[1] = i-3; /* Install length */
          ret = TMR_SR_send(reader, msg);
          if (TMR_SUCCESS != ret)
          {
            return ret;
          }
          sr->tagsRemainingInBuffer = msg[8];
          sr->bufPointer = 9;
        }
        else if (reader->u.serialReader.opCode == TMR_SR_OPCODE_READ_TAG_ID_SINGLE)
        {
          TMR_SR_receiveMessage(reader, msg, reader->u.serialReader.opCode, timeoutMs);
          sr->tagsRemainingInBuffer = (uint8_t)GETU32AT(msg , 9);
          sr->tagsRemaining = sr->tagsRemainingInBuffer;
          sr->bufPointer = 13 ;
        }
        else
        {
           return TMR_ERROR_INVALID_OPCODE; 
        }
      }
    }

    i = sr->bufPointer;
    if (reader->u.serialReader.opCode == TMR_SR_OPCODE_READ_TAG_ID_MULTIPLE)
    {
      flags = GETU16AT(msg, sr->useStreaming ? 8 : 5);
      TMR_SR_parseMetadataFromMessage(reader, read, flags, &i, msg);
      
    }
    if (reader->u.serialReader.opCode == TMR_SR_OPCODE_READ_TAG_ID_SINGLE)
    {
      flags = GETU16AT(msg, i + 6);
      subResponseLen = msg[i+1];
      i += 7;
      TMR_SR_parseMetadataOnly(reader, read, flags, &i, msg);
      epclen = subResponseLen + 4 - (i - sr->bufPointer) - crclen;
      read->tag.epcByteCount=epclen;
      memcpy(&(read->tag.epc), &msg[i], read->tag.epcByteCount);
      i+=epclen;
      read->tag.crc = GETU16(msg, i);
    }
    sr->bufPointer = i;
    
    
    TMR_SR_postprocessReaderSpecificMetadata(read, sr);

    sr->tagsRemainingInBuffer--;

    if (false == sr->useStreaming)
    {
      sr->tagsRemaining--;
    }
  
    return TMR_SUCCESS;
  }
}


TMR_Status
TMR_SR_tagop_execute(TMR_Reader *reader, TMR_Tagop *tagop)
{
  TMR_Status ret;
  TMR_SR_GEN2_Q zeroQ;
  TMR_SR_ProtocolConfiguration key;
  TMR_ReadPlan tagopReadPlan;
  TMR_SR_SerialReader *sr;
  int32_t count;

  sr = &reader->u.serialReader;
  key.protocol = TMR_TAG_PROTOCOL_GEN2;
  key.u.gen2 = TMR_SR_GEN2_CONFIGURATION_Q;
  zeroQ.type = TMR_SR_GEN2_Q_STATIC;
  zeroQ.u.staticQ.initialQ = (uint8_t)0;
  /* A note on the interactions with the tag buffer: The read commands
   * invoked here will add records to the tag buffer if they are
   * successful. This won't interfere with an ongoing getNextTag()
   * sequence from a single read, as this code tracks the number of
   * tags it expects to receive, and won't continue after it has that
   * number. Future read() operations will not be affected, since it
   * starts by clearing the tag buffer. There is a chance that a long
   * sequence of tag operations will fill the tag buffer, which is a
   * problem; it is also unclear how it will interact with a streaming
   * implementation of read()/getNextTag().
   * 
   */

  /* Get old value of Q and Store it*/
  ret = TMR_paramGet(reader, TMR_PARAM_GEN2_Q, &(sr->oldQ));
  if (TMR_SUCCESS != ret)
    return ret;

  /* Set to static Q=0 */
  ret = TMR_paramSet(reader, TMR_PARAM_GEN2_Q, &zeroQ);
  if (TMR_SUCCESS != ret)
    return ret;

  /* Set up read plan with tagop antenna and protocol and supplied op */
  TMR_RP_init_simple(&tagopReadPlan, 1, &reader->tagOpParams.antenna,
                     reader->tagOpParams.protocol, 1);
  TMR_RP_set_tagop(&tagopReadPlan, tagop);
                     
  /* @todo This routine could support multi-tagop sequences by
   * repeatedly calling read_internal with individual operations in
   * the sequence. Is it worthwhile to do that, or is it misleading to
   * provide that behavior when really we'd be re-running the
   * inventory round and singulation for each operation?
   */

  /* Execute read_internal with the constructed plan */
  ret = TMR_SR_read_internal(reader, sr->commandTimeout, &count,
                             &tagopReadPlan);

  if (ret != TMR_SUCCESS)
    return ret;

  if (0 == count)
    return TMR_ERROR_NO_TAGS_FOUND;

  return TMR_SUCCESS;
}


TMR_Status 
TMR_SR_writeTag(struct TMR_Reader *reader, const TMR_TagFilter *filter,
                const TMR_TagData *data)
{
  TMR_Status ret;
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  ret = setProtocol(reader, reader->tagOpParams.protocol);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  if (TMR_TAG_PROTOCOL_GEN2 == reader->tagOpParams.protocol)
  {  
    /* Serial reader doesn't support selecting tags before writing the EPC */
    if (NULL != filter)
    {
      return TMR_ERROR_UNSUPPORTED;
    }

    return TMR_SR_cmdWriteTagEpc(reader, (uint16_t)(sr->commandTimeout), 
                                 data->epcByteCount, data->epc, 0);
  }
  else
  {
    return TMR_ERROR_UNIMPLEMENTED;
  }
}


TMR_Status
TMR_SR_readTagMemWords(TMR_Reader *reader, const TMR_TagFilter *target, 
                       uint32_t bank, uint32_t wordAddress, 
                       uint16_t wordCount, uint16_t data[])
{
  TMR_Status ret;

  ret = TMR_SR_readTagMemBytes(reader, target, bank, wordAddress * 2,
                               wordCount * 2, (uint8_t *)data);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

#ifndef TMR_BIG_ENDIAN_HOST
  {
    uint16_t i;
    uint8_t *data8;
    /* We used the uint16_t data as temporary storage for the values read,
       but we need to adjust for possible endianness differences.
       This is technically correct on all platforms, though it's a no-op
       on big-endian ones. */
    data8 = (uint8_t *)data;
    for (i = 0; i < wordCount; i++)
    {
      data[i] = (data8[2*i] << 8) | data8[2*i + 1];
    }
  }
#endif

  return TMR_SUCCESS;
}


static TMR_Status
TMR_SR_readTagMemBytesUnaligned(TMR_Reader *reader,
                                const TMR_TagFilter *target, 
                                uint32_t bank, uint32_t byteAddress, 
                                uint16_t byteCount, uint8_t data[])
{
  TMR_Status ret;
  TMR_TagReadData read;
  uint16_t wordCount;
  uint8_t buf[254];
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  wordCount = (uint16_t)((byteCount + 1 + (byteAddress & 1) ) / 2);
  read.data.max = 254;
  read.data.list = buf;

  ret = TMR_SR_cmdGEN2ReadTagData(reader, (uint16_t)(sr->commandTimeout),
                                  bank, byteAddress / 2, (uint8_t)wordCount,
                                  sr->gen2AccessPassword, target, &read);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  memcpy(data, buf + (byteAddress & 1), byteCount);

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_readTagMemBytes(TMR_Reader *reader, const TMR_TagFilter *target, 
                       uint32_t bank, uint32_t byteAddress, 
                       uint16_t byteCount, uint8_t data[])
{
  TMR_Status ret;
  TMR_TagReadData read;
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  ret = setProtocol(reader, reader->tagOpParams.protocol);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  read.data.max = byteCount;
  read.data.list = (uint8_t *)data;

  if (TMR_TAG_PROTOCOL_GEN2 == reader->tagOpParams.protocol)
  {
    /*
     * Handling unaligned reads takes spare memory; avoid allocating that
     * (on that stack) if not necessary.
     */
    if ((byteAddress & 1) || (byteCount & 1))
    {
      return TMR_SR_readTagMemBytesUnaligned(reader, target, bank, byteAddress,
                                             byteCount, data);
    }

    return TMR_SR_cmdGEN2ReadTagData(reader, (uint16_t)(sr->commandTimeout),
                                     bank, byteAddress / 2, byteCount / 2,
                                     sr->gen2AccessPassword, target, &read);
  }
#ifdef TMR_ENABLE_ISO180006B
  else if (TMR_TAG_PROTOCOL_ISO180006B == reader->tagOpParams.protocol)
  {
    return TMR_SR_cmdISO180006BReadTagData(reader,(uint16_t)(sr->commandTimeout),
                                           (uint8_t)byteAddress, (uint8_t)byteCount, target,
                                           &read);
  }
#endif /* TMR_ENABLE_ISO180006B */
  else
  {
    return TMR_ERROR_UNIMPLEMENTED;
  }
}

TMR_Status
TMR_SR_writeTagMemWords(struct TMR_Reader *reader, const TMR_TagFilter *filter,
                        uint32_t bank, uint32_t address,
                        uint16_t count, const uint16_t data[])
{
  const uint8_t *dataPtr;
#ifndef TMR_BIG_ENDIAN_HOST
  uint8_t buf[254];
  int i;

    for (i = 0 ; i < count ; i++)
    {
      buf[2*i    ] = data[i] >> 8;
      buf[2*i + 1] = data[i] & 0xff;
    }
    dataPtr = buf;
#else
    dataPtr = (const uint8_t *)data;
#endif

    return TMR_SR_writeTagMemBytes(reader, filter, bank, address * 2, count * 2,
                                   dataPtr);
}

TMR_Status
TMR_SR_writeTagMemBytes(struct TMR_Reader *reader, const TMR_TagFilter *filter,
                        uint32_t bank, uint32_t address,
                        uint16_t count, const uint8_t data[])
{
  TMR_Status ret;
  TMR_SR_SerialReader *sr;
  TMR_GEN2_WriteMode mode;
  TMR_GEN2_WriteMode *value = &mode;
 
  sr = &reader->u.serialReader;
  TMR_paramGet(reader, TMR_PARAM_GEN2_WRITEMODE, value);

  ret = setProtocol(reader, reader->tagOpParams.protocol);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  if (TMR_TAG_PROTOCOL_GEN2 == reader->tagOpParams.protocol)
  {
    /* Misaligned writes are not permitted */
    if ((address & 1) || (count & 1))
    {
      return TMR_ERROR_INVALID;
    }

    switch (mode)
    {
    case TMR_GEN2_WORD_ONLY:
      return TMR_SR_cmdGEN2WriteTagData(reader, (uint16_t)(sr->commandTimeout),
        bank, address / 2, (uint8_t)count, data,
        sr->gen2AccessPassword, filter);
    case TMR_GEN2_BLOCK_ONLY:
      return TMR_SR_cmdBlockWrite(reader,(uint16_t)sr->commandTimeout, bank, address / 2, (uint8_t)(count/2), data, sr->gen2AccessPassword, filter);
    case TMR_GEN2_BLOCK_FALLBACK:

      ret =  TMR_SR_cmdBlockWrite(reader,(uint16_t)sr->commandTimeout, bank, address / 2, (uint8_t)(count/2), data, sr->gen2AccessPassword, filter);
      if (TMR_SUCCESS == ret)
      {
        return ret;
      }
      else 
      {
        return TMR_SR_cmdGEN2WriteTagData(reader, (uint16_t)(sr->commandTimeout),
          bank, address / 2, (uint8_t)count, data,
          sr->gen2AccessPassword, filter);

      }
    default: 
      return TMR_SUCCESS;
    }


  }
#ifdef TMR_ENABLE_ISO180006B
  else if (TMR_TAG_PROTOCOL_ISO180006B == reader->tagOpParams.protocol)
  {
    if (count != 1)
    {
      return TMR_ERROR_INVALID;
    }
    return TMR_SR_cmdISO180006BWriteTagData(reader, (uint16_t)(sr->commandTimeout),
                                            (uint8_t)address, 1, data, filter);
  }
#endif
  else
  {
    return TMR_ERROR_INVALID;
  }
}


TMR_Status
TMR_SR_lockTag(struct TMR_Reader *reader, const TMR_TagFilter *filter,
               TMR_TagLockAction *action)
{
  TMR_Status ret;
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  ret = setProtocol(reader, reader->tagOpParams.protocol);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  if (TMR_TAG_PROTOCOL_GEN2 == reader->tagOpParams.protocol)
  {
    if (TMR_LOCK_ACTION_TYPE_GEN2_LOCK_ACTION != action->type)
    {
      /* Lock type doesn't match tagop protocol */
      return TMR_ERROR_INVALID;
    }
    return TMR_SR_cmdGEN2LockTag(reader, (uint16_t)sr->commandTimeout,
                                 action->u.gen2LockAction.mask, 
                                 action->u.gen2LockAction.action, 
                                 sr->gen2AccessPassword,
                                 filter);
  }
#ifdef TMR_ENABLE_ISO180006B
  else if (TMR_TAG_PROTOCOL_ISO180006B == reader->tagOpParams.protocol)
  {
    if (TMR_LOCK_ACTION_TYPE_ISO180006B_LOCK_ACTION != action->type)
    {
      /* Lock type doesn't match tagop protocol */
      return TMR_ERROR_INVALID;
    }
    return TMR_SR_cmdISO180006BLockTag(reader, (uint16_t)(sr->commandTimeout),
                                       action->u.iso180006bLockAction.address, 
                                       filter);
  }
#endif
  else
  {
    return TMR_ERROR_UNIMPLEMENTED;
  }
}


TMR_Status
TMR_SR_killTag(struct TMR_Reader *reader, const TMR_TagFilter *filter,
               const TMR_TagAuthentication *auth)
{
  TMR_Status ret;
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  ret = setProtocol(reader, reader->tagOpParams.protocol);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  if (TMR_TAG_PROTOCOL_GEN2 == reader->tagOpParams.protocol)
  {
    if (TMR_AUTH_TYPE_GEN2_PASSWORD != auth->type)
    {
      /* Auth type doesn't match tagop protocol */
      return TMR_ERROR_INVALID;
    }

    return TMR_SR_cmdKillTag(reader,
                             (uint16_t)(sr->commandTimeout),
                             auth->u.gen2Password,
                             filter);
  }
  else
  {
    return TMR_ERROR_UNIMPLEMENTED;
  }
}

TMR_Status
TMR_SR_gpoSet(struct TMR_Reader *reader, uint8_t count,
              const TMR_GpioPin state[])
{
  TMR_Status ret;
  int i;
  
  for (i = 0; i < count; i++)
  {
    ret = TMR_SR_cmdSetGPIO(reader, state[i].id, state[i].high);
    if (TMR_SUCCESS != ret)
      return ret;
  }

  return TMR_SUCCESS;
}


TMR_Status TMR_SR_gpiGet(struct TMR_Reader *reader, uint8_t *count,
                         TMR_GpioPin state[])
{
  TMR_Status ret;
  bool pinStates[4];
  uint8_t i, numPins;

  numPins = numberof(pinStates);
  ret = TMR_SR_cmdGetGPIO(reader, &numPins, pinStates);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  if (numPins > *count)
  {
    numPins = *count;
  }

  for (i = 0 ; i < numPins ; i++)
  {
    state[i].id = 1 + i;
    state[i].high = pinStates[i];
  }

  *count = numPins;

  return TMR_SUCCESS;
}


TMR_Status
TMR_SR_firmwareLoad(struct TMR_Reader *reader, void *cookie,
                    TMR_FirmwareDataProvider provider)
{
  static const uint8_t magic[] =
    { 0x54, 0x4D, 0x2D, 0x53, 0x50, 0x61, 0x69, 0x6B, 0x00, 0x00, 0x00, 0x02 };

  TMR_Status ret;
  uint8_t buf[256], *bufptr;
  uint16_t packetLen, packetRemaining, size, offset;
  uint32_t len, rate, address, remaining;
  TMR_SR_SerialReader *sr;
  TMR_SR_SerialTransport *transport;

  sr = &reader->u.serialReader;
  transport = &sr->transport;

  remaining = numberof(magic) + 4;
  offset = 0;
  bufptr = buf;
  while (remaining > 0)
  {
    size = (uint16_t)remaining;
    if (false == provider(cookie, &size, buf + offset))
    {
      return TMR_ERROR_FIRMWARE_FORMAT;
    }
    
    remaining -= size;
    offset += size;
  }

  if (0 != memcmp(buf, magic, numberof(magic)))
  {
    return TMR_ERROR_FIRMWARE_FORMAT;
  }

  len = GETU32AT(buf, 12);

  /* @todo get any params we want to reset */

  /*
   * Drop baud to 9600 so we know for sure what it will be after going
   * back to the bootloader.
   */
  ret = TMR_SR_cmdSetBaudRate(reader, 9600);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  ret = transport->setBaudRate(transport, 9600);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  ret = TMR_SR_cmdBootBootloader(reader);
  if ((TMR_SUCCESS != ret)
      /* Invalid Opcode okay -- means "already in bootloader" */
      && (TMR_ERROR_INVALID_OPCODE != ret))
  {
    return ret;
  }

  /*
   * Wait for the bootloader to be entered. 200ms is enough.
   */
  tmr_sleep(200);

  /* Bootloader doesn't support high speed operation */
  rate = sr->baudRate;
  if (rate > 115200)
  {
    rate = 115200;
  }

  ret = TMR_SR_cmdSetBaudRate(reader, rate);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }
  ret = transport->setBaudRate(transport, rate);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  ret = TMR_SR_cmdEraseFlash(reader, 2, 0x08959121);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  address = 0;
  remaining = len;
  while (remaining > 0)
  {
    packetLen = 240;
    if (packetLen > remaining)
    {
      packetLen = (uint16_t)remaining;
    }
    offset = 0;
    packetRemaining = packetLen;
    while (packetRemaining > 0)
    {
      size = packetRemaining;
      if (false == provider(cookie, &size, buf + offset))
      {
        return TMR_ERROR_FIRMWARE_FORMAT;
      }
      packetRemaining -= size;
      offset += size;
    }
    ret = TMR_SR_cmdWriteFlashSector(reader, 2, address, 0x02254410,(uint8_t) packetLen,
                                     buf, 0);
    if (TMR_SUCCESS != ret)
    {
      return ret;
    }
    address += packetLen;
    remaining -= packetLen;
  }
  
  return TMR_SR_boot(reader, rate);
}


static void
TMR_stringCopy(TMR_String *dest, const char *src, int len)
{

  if (dest->max - 1 < len)
  {
    len = dest->max - 1;
  }
  if (dest->max > 0)
  {
    memcpy(dest->value, src, len);
    dest->value[len] = '\0';
  }
}

static TMR_Status
getHardwareInfo(struct TMR_Reader *reader, void *value)
{
  TMR_Status ret;
  uint8_t buf[127];
  char tmp[255];
  uint8_t count;
  TMR_SR_VersionInfo *info;
  
  info = &reader->u.serialReader.versionInfo;

  TMR_hexDottedQuad(info->hardware, tmp);
  tmp[11] = '-';
  count = 127;
  ret = TMR_SR_cmdGetHardwareVersion(reader, 0, 0, &count, buf);
  if (TMR_SUCCESS != ret)
  {
    count = 0;
    tmp[11] = '\0';
  }

  TMR_bytesToHex(buf, count, tmp + 12);
  TMR_stringCopy(value, tmp, 12 + 2*count);

  return TMR_SUCCESS;
}

static TMR_Status
getSerialNumber(struct TMR_Reader *reader, void *value)
{
  /* See http://trac/swtree/changeset/6498 for previous implementation */
  TMR_Status ret;
  uint8_t buf[127];
  uint8_t count;
  char tmp[127];
  int tmplen;

  count = 127;
  tmplen = 0;
  ret = TMR_SR_cmdGetHardwareVersion(reader, 0, 0x40, &count, buf);
  if (TMR_SUCCESS != ret)
  {
    count = 0;
  }
  else
  {
    int idx;
    uint8_t len;

    idx = 3;
    len = buf[idx++];
    if (len > (count-3))
    {
      ret = TMR_ERROR_UNIMPLEMENTED;
    }
    else
    {
      for (tmplen=0; tmplen<len; tmplen++)
      {
        tmp[tmplen] = (char)buf[idx+tmplen];
      }
    }
  }
  TMR_stringCopy(value, tmp, tmplen);
  return ret;
}

/* Abuse the structure layout of TMR_SR_PortPowerAndSettlingTime a bit
 * so that this can be one function instead of three only slightly
 * different pieces of code. Since all three per-port values are
 * represented as uint16_t, take an additional 'offset' parameter that
 * gives the pointer distance from the first element to the element
 * (readPower, writePower, settlingTime) that we actually want to set.
 */
static TMR_Status
setPortValues(struct TMR_Reader *reader, const TMR_PortValueList *list,
              int offset)
{
  TMR_Status ret;
  TMR_SR_PortPowerAndSettlingTime ports[TMR_SR_MAX_ANTENNA_PORTS];
  uint8_t count;
  uint16_t i, j;

  count = numberof(ports);

  ret = TMR_SR_cmdGetAntennaPortPowersAndSettlingTime(reader, &count, ports);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  /* If a value is left out, 0 is assumed */
  for (j = 0; j < count; j++)
  {
    *(&ports[j].readPower + offset) = 0;
  }

  /* 
   * For each settling time in the user's list, try to find an
   * existing entry in the list returned from the reader.
   */
  for (i = 0; i < list->len; i++)
  {
    for (j = 0 ; j < count ; j++)
    {
      if (list->list[i].port == ports[j].port)
      {
        break;
      }
    }
    if (j == count)
    {
      if (count == TMR_SR_MAX_ANTENNA_PORTS)
      {
        return TMR_ERROR_TOO_BIG;
      }
      ports[j].port = list->list[i].port;
      ports[j].readPower = 0;
      ports[j].writePower = 0;
      ports[j].settlingTime = 0;
      count++;
    }
    *(&ports[j].readPower + offset) = list->list[i].value;
  }
  return TMR_SR_cmdSetAntennaPortPowersAndSettlingTime(reader, count, ports);
}

/* See comment before setPortValues() for the meaning of offset */
static TMR_Status
getPortValues(struct TMR_Reader *reader, TMR_PortValueList *list, int offset)
{
  TMR_Status ret;
  TMR_SR_PortPowerAndSettlingTime ports[TMR_SR_MAX_ANTENNA_PORTS];
  uint8_t count;
  uint16_t i, j;

  count = numberof(ports);

  ret = TMR_SR_cmdGetAntennaPortPowersAndSettlingTime(reader, &count, ports);
  if (TMR_SUCCESS != ret)
  {
    return ret;
  }

  for (i = 0, j = 0; i < count; i++)
  {
    if (0 == *(&ports[i].readPower + offset))
    {
      continue;
    }
    if (j < list->max)
    {
      list->list[j].port = ports[i].port;
      list->list[j].value = *(&ports[i].readPower + offset);
    }
    j++;
  }
  list->len = (uint8_t)j;

  return TMR_SUCCESS;
}


static bool
validateReadPlan(struct TMR_Reader *reader, TMR_ReadPlan *plan)
{
  TMR_SR_SerialReader *sr;
  int i, j;

  sr = &reader->u.serialReader;

  if (TMR_READ_PLAN_TYPE_MULTI == plan->type)
  {
    plan->u.multi.totalWeight = 0;
    for (i = 0; i < plan->u.multi.planCount; i++)
    {
      if (false == validateReadPlan(reader, plan->u.multi.plans[i]))
      {
        return false;
      }
      plan->u.multi.totalWeight += plan->u.multi.plans[i]->weight;
    }
    if (0 == plan->u.multi.totalWeight)
    {
      return false;
    }
  }
  else if (TMR_READ_PLAN_TYPE_SIMPLE == plan->type)
  {
    if (0 == ((1 << (plan->u.simple.protocol - 1)) &
              sr->versionInfo.protocols))
    {
      return false;
    }
    for (i = 0 ; i < plan->u.simple.antennas.len; i++)
    {
      for (j = 0; j < sr->txRxMap->len; j++)
      {
        if (plan->u.simple.antennas.list[i] == sr->txRxMap->list[j].antenna)
        {
          break;
        }
      }
      if (j == sr->txRxMap->len)
      {
        return false;
      }
    }
    if (NULL != plan->u.simple.tagop)
    {
      if (TMR_TAGOP_LIST == plan->u.simple.tagop->type)
        return false; /* not yet supported */
    }
  }

  return true;
}


static void
TMR_SR_paramProbe(struct TMR_Reader *reader, TMR_Param key)
{
  TMR_SR_SerialReader *sr;
 /* buf is at least as large as the largest parameter, with all values 0
  * (NULL pointers and 0 lengths).
  */
  uint32_t buf[] = {0, 0, 0, 0, 0, 0, 0, 0};
  TMR_Status ret;

  sr = &reader->u.serialReader;

  ret = TMR_paramGet(reader, key, &buf);
  if (TMR_SUCCESS == ret)
  {
    BITSET(sr->paramPresent, key);
  }
  BITSET(sr->paramConfirmed, key);
}

TMR_Status
TMR_paramList(struct TMR_Reader *reader, TMR_Param *keys, uint32_t *len)
{
  int i, count, max;
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  max = *len;
  count = 0;
  for (i = TMR_PARAM_MIN; i <= TMR_PARAM_MAX ; i++)
  {
    if (0 == BITGET(sr->paramConfirmed, i))
    {
      TMR_SR_paramProbe(reader, i);
    }

    if (BITGET(sr->paramPresent, i))
    {
      if (count < max)
        keys[count] = i;
      count++;
    }
  }

  *len = count;

  return TMR_SUCCESS;
}


static TMR_Status
TMR_SR_paramSet(struct TMR_Reader *reader, TMR_Param key, const void *value)
{
  TMR_Status ret;
  TMR_SR_Configuration readerkey;
  TMR_SR_ProtocolConfiguration protokey;
  TMR_SR_SerialReader *sr;
  TMR_SR_SerialTransport *transport;

  ret = TMR_SUCCESS;
  sr = &reader->u.serialReader;
  readerkey = -1;
  protokey.protocol = TMR_TAG_PROTOCOL_GEN2;

  if (0 == BITGET(sr->paramConfirmed, key))
  {
    TMR_SR_paramProbe(reader, key);
  }

  if (BITGET(sr->paramConfirmed, key) && 0 == BITGET(sr->paramPresent, key))
  {
    return TMR_ERROR_NOT_FOUND;
  }

  switch (key)
  {
  case TMR_PARAM_REGION_ID:
    sr->regionId = *(TMR_Region*)value;
    if (reader->connected)
    {
      ret = TMR_SR_cmdSetRegion(reader, sr->regionId);
    }
    break;

  case TMR_PARAM_BAUDRATE:
  {
    uint32_t rate;

    rate = *(uint32_t *)value;
    if (reader->connected)
    {
      ret = TMR_SR_cmdSetBaudRate(reader, rate);
      if (TMR_SUCCESS != ret)
      {
        break;
      }
      sr->baudRate = rate;
      transport = &sr->transport;
      transport->setBaudRate(transport, sr->baudRate);
    }
    else
    {
      sr->baudRate = rate;
    }
    break;
  }

  case TMR_PARAM_COMMANDTIMEOUT:
    sr->commandTimeout = *(uint32_t *)value;
    break;
  case TMR_PARAM_TRANSPORTTIMEOUT:
	  sr->transportTimeout = *(uint32_t *)value;
	  break;
  case TMR_PARAM_RADIO_ENABLEPOWERSAVE:
	  readerkey = TMR_SR_CONFIGURATION_TRANSMIT_POWER_SAVE;
	  break;
  case TMR_PARAM_RADIO_READPOWER:
	  ret = TMR_SR_cmdSetReadTxPower(reader, *(uint16_t *)value);
	  break;
  case TMR_PARAM_RADIO_WRITEPOWER:
    ret = TMR_SR_cmdSetWriteTxPower(reader, *(uint16_t *)value);
    break;

  case TMR_PARAM_RADIO_PORTREADPOWERLIST:
    ret = setPortValues(reader, value, 0);
    break;

  case TMR_PARAM_RADIO_PORTWRITEPOWERLIST:
    ret = setPortValues(reader, value, 1);
    break;

  case TMR_PARAM_ANTENNA_SETTLINGTIMELIST:
    ret = setPortValues(reader, value, 2);
    break;

  case TMR_PARAM_ANTENNA_CHECKPORT:
    readerkey = TMR_SR_CONFIGURATION_SAFETY_ANTENNA_CHECK;
    break;

  case TMR_PARAM_TAGREADDATA_RECORDHIGHESTRSSI:
    readerkey = TMR_SR_CONFIGURATION_RECORD_HIGHEST_RSSI;
    break;

  case TMR_PARAM_TAGREADDATA_REPORTRSSIINDBM:
    readerkey = TMR_SR_CONFIGURATION_RSSI_IN_DBM;
    break;

  case TMR_PARAM_TAGREADDATA_UNIQUEBYANTENNA:
    readerkey = TMR_SR_CONFIGURATION_UNIQUE_BY_ANTENNA;
    break;

  case TMR_PARAM_TAGREADDATA_UNIQUEBYDATA:
    readerkey = TMR_SR_CONFIGURATION_UNIQUE_BY_DATA;
    break;

  case TMR_PARAM_ANTENNA_PORTSWITCHGPOS:
  {
    uint8_t portmask;
    const TMR_uint8List *u8list;
    uint16_t i;

    u8list = value;
    portmask = 0;
    for (i = 0 ; i < u8list->len && i < u8list->max ; i++)
    {
      portmask |= 1 << (u8list->list[i] - 1);
    }
    
    ret = TMR_SR_cmdSetReaderConfiguration(
      reader, TMR_SR_CONFIGURATION_ANTENNA_CONTROL_GPIO, &portmask);

    if (TMR_SUCCESS != ret)
    {
      break;
    }
    ret = initTxRxMapFromPorts(reader);
    
    break;
  }

  case TMR_PARAM_ANTENNA_TXRXMAP:
  {
    const TMR_AntennaMapList *map;
    TMR_AntennaMapList *mymap;
    uint16_t i;

    map = value;
    mymap = sr->txRxMap;

    if (map->len > mymap->max)
    {
      ret = TMR_ERROR_TOO_BIG;
      break;
    }

    for (i = 0 ; i < map->len ; i++)
    {
      if (!HASPORT(sr->portMask, map->list[i].txPort) ||
          !HASPORT(sr->portMask, map->list[i].rxPort))
      {
        return TMR_ERROR_NO_ANTENNA;
      }
    }
    for (i = 0 ; i < map->len ; i++)
    {
      mymap->list[i] = map->list[i];
    }
    mymap->len = map->len;

    break;
  }

  case TMR_PARAM_REGION_HOPTABLE:
  {
    const TMR_uint32List *u32list;

    u32list = value;

    ret = TMR_SR_cmdSetFrequencyHopTable(reader, (uint8_t)u32list->len, u32list->list);
    break;
  }

  case TMR_PARAM_REGION_HOPTIME:
    ret = TMR_SR_cmdSetFrequencyHopTime(reader, *(uint32_t *)value);
    break;

  case TMR_PARAM_REGION_LBT_ENABLE:
  {
    uint32_t hopTable[64];
    uint8_t count;

    count = numberof(hopTable);
    ret = TMR_SR_cmdGetFrequencyHopTable(reader, &count, hopTable);
    if (TMR_SUCCESS != ret)
    {
      break;
    }

    ret = TMR_SR_cmdSetRegionLbt(reader, sr->regionId, *(bool *)value);
    if (TMR_SUCCESS != ret)
    {
      break;
    }

    ret = TMR_SR_cmdSetFrequencyHopTable(reader, count, hopTable);
    break;
  }

  case TMR_PARAM_TAGOP_ANTENNA:
  {
    uint16_t i;
    TMR_AntennaMapList *map;
    uint8_t antenna;
    uint8_t txPort, rxPort;

    map = sr->txRxMap;
    antenna = *(uint8_t *)value;

    txPort = rxPort = 0;
    for (i = 0; i < map->len && i < map->max; i++)
    {
      if (map->list[i].antenna == antenna)
      {
        txPort = map->list[i].txPort;
        rxPort = map->list[i].rxPort;
        reader->tagOpParams.antenna = antenna;
        break;
      }
    }
    if (txPort == 0)
    {
      ret = TMR_ERROR_NO_ANTENNA;
    }
    else
    {
      ret = TMR_SR_cmdSetTxRxPorts(reader, txPort, rxPort);
    }
    break;
  }

  case TMR_PARAM_TAGOP_PROTOCOL:
    if (0 == ((1 << (*(TMR_TagProtocol *)value - 1)) &
              sr->versionInfo.protocols))
    {
      ret = TMR_ERROR_UNSUPPORTED;
    }
    else
    {
      reader->tagOpParams.protocol = *(TMR_TagProtocol *)value;
    }
    break;

  case TMR_PARAM_READ_PLAN:
  {
    const TMR_ReadPlan *plan;
    TMR_ReadPlan tmpPlan;

    plan = value;
    tmpPlan = *plan;

    if (false == validateReadPlan(reader, &tmpPlan))
    {
      return TMR_ERROR_INVALID;
    }
    
    *reader->readParams.readPlan = tmpPlan;
    break;
  }

  case TMR_PARAM_GPIO_INPUTLIST:
  case TMR_PARAM_GPIO_OUTPUTLIST:
    if (TMR_SR_MODEL_M6E == sr->versionInfo.hardware[0])
    {
      const TMR_uint8List *u8list;      
      int bit, i, newDirections, pin;

      u8list = value;

      if (key == TMR_PARAM_GPIO_OUTPUTLIST)
      {
        newDirections = 0;
      }
      else
      {
        newDirections = 0x1e;
      }

      for (i = 0 ; i < u8list->len && i < u8list->max ; i++)
      {
        newDirections ^= 1 << u8list->list[i];
      }

      for (pin = 1 ; pin <= 4 ; pin++)
      {
        bit = 1 << pin;
        if (sr->gpioDirections == -1
            || (sr->gpioDirections & bit) != (newDirections & bit))
        {
          ret = TMR_SR_cmdSetGPIODirection(reader, pin,
                                           (newDirections & bit) != 0);
          if (TMR_SUCCESS != ret)
          {
            return ret;
          }
        }
      }
      sr->gpioDirections = newDirections;
      break;
    }
  case TMR_PARAM_RADIO_POWERMAX:
  case TMR_PARAM_RADIO_POWERMIN:
  case TMR_PARAM_REGION_SUPPORTEDREGIONS:
  case TMR_PARAM_ANTENNA_PORTLIST:
  case TMR_PARAM_ANTENNA_CONNECTEDPORTLIST:
  case TMR_PARAM_VERSION_SUPPORTEDPROTOCOLS:
  case TMR_PARAM_RADIO_TEMPERATURE:
  case TMR_PARAM_VERSION_HARDWARE:
  case TMR_PARAM_VERSION_MODEL:
  case TMR_PARAM_VERSION_SOFTWARE:
    ret = TMR_ERROR_READONLY;
    break;

  case TMR_PARAM_POWERMODE:
    if (reader->connected)
    {
      ret = TMR_SR_cmdSetPowerMode(reader, *(TMR_SR_PowerMode *)value);
      if (TMR_SUCCESS == ret)
      {
        sr->powerMode = *(TMR_SR_PowerMode *)value;
      }
    }
    else
    {
      sr->powerMode = *(TMR_SR_PowerMode *)value;
    }
    break;

  case TMR_PARAM_USERMODE:
    ret = TMR_SR_cmdSetUserMode(reader, *(TMR_SR_UserMode *)value);
    break;

  case TMR_PARAM_GEN2_Q:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_Q;
    break;

  case TMR_PARAM_GEN2_TAGENCODING:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_TAGENCODING;
    break;

  case TMR_PARAM_GEN2_SESSION:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_SESSION;
    break;

  case TMR_PARAM_GEN2_TARGET:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_TARGET;
    break;

  case TMR_PARAM_GEN2_BLF:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_LINKFREQUENCY;
    break;

  case TMR_PARAM_GEN2_TARI:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_TARI;
    break;

  case TMR_PARAM_GEN2_WRITEMODE:
    sr->writeMode = *(TMR_GEN2_WriteMode *)value;
    break;

#ifdef TMR_ENABLE_ISO180006B
  case TMR_PARAM_ISO180006B_BLF:
    protokey.protocol = TMR_TAG_PROTOCOL_ISO180006B;
    protokey.u.iso180006b = TMR_SR_ISO180006B_CONFIGURATION_LINKFREQUENCY;
    break;
#endif /* TMR_ENABLE_ISO180006B */

  case TMR_PARAM_GEN2_ACCESSPASSWORD:
    sr->gen2AccessPassword = *(TMR_GEN2_Password *)value;
    break;

  default:
    ret = TMR_ERROR_NOT_FOUND;
  }

  switch (key)
  {
  case TMR_PARAM_ANTENNA_CHECKPORT:
  case TMR_PARAM_RADIO_ENABLEPOWERSAVE:
  case TMR_PARAM_TAGREADDATA_RECORDHIGHESTRSSI:
  case TMR_PARAM_TAGREADDATA_REPORTRSSIINDBM:
  case TMR_PARAM_TAGREADDATA_UNIQUEBYANTENNA:
  case TMR_PARAM_TAGREADDATA_UNIQUEBYDATA:
    ret = TMR_SR_cmdSetReaderConfiguration(reader, readerkey, value);
    break;

  case TMR_PARAM_GEN2_Q:
  case TMR_PARAM_GEN2_TAGENCODING:
  case TMR_PARAM_GEN2_SESSION:
  case TMR_PARAM_GEN2_TARGET:
  case TMR_PARAM_GEN2_BLF:
  case TMR_PARAM_GEN2_TARI:
#ifdef TMR_ENABLE_ISO180006B
  case TMR_PARAM_ISO180006B_BLF:
#endif /* TMR_ENABLE_ISO180006B */
    ret = TMR_SR_cmdSetProtocolConfiguration(reader, protokey.protocol, 
                                             protokey, value);
    break;

  default:
    ;
  }
  return ret;
}

static TMR_Status
TMR_SR_paramGet(struct TMR_Reader *reader, TMR_Param key, void *value)
{
  TMR_Status ret;
  TMR_SR_Configuration readerkey;
  TMR_SR_ProtocolConfiguration protokey;
  TMR_SR_SerialReader *sr;

  ret = TMR_SUCCESS;
  sr = &reader->u.serialReader;
  readerkey = -1;
  protokey.protocol = TMR_TAG_PROTOCOL_GEN2;

  if (BITGET(sr->paramConfirmed, key) && 0 == BITGET(sr->paramPresent, key))
  {
    return TMR_ERROR_NOT_FOUND;
  }

  switch (key)
  {
  case TMR_PARAM_BAUDRATE:
    *(uint32_t *)value = sr->baudRate;
    break;

  case TMR_PARAM_COMMANDTIMEOUT:
    *(uint32_t *)value = sr->commandTimeout;
    break;

  case TMR_PARAM_TRANSPORTTIMEOUT:
    *(uint32_t *)value = sr->transportTimeout;
    break;

  case TMR_PARAM_REGION_ID:
	*(TMR_Region *)value = sr->regionId;
	break;

  case TMR_PARAM_RADIO_ENABLEPOWERSAVE:
	readerkey = TMR_SR_CONFIGURATION_TRANSMIT_POWER_SAVE;
	break;

  case TMR_PARAM_RADIO_POWERMAX:
  {
    TMR_SR_PowerWithLimits power;

    ret = TMR_SR_cmdGetReadTxPowerWithLimits(reader, &power);
    if (TMR_SUCCESS != ret)
    {
      break;
    }
    *(uint16_t *)value = power.maxPower;
    break;
  }

  case TMR_PARAM_RADIO_POWERMIN:
  {
    TMR_SR_PowerWithLimits power;

    ret = TMR_SR_cmdGetReadTxPowerWithLimits(reader, &power);
    if (TMR_SUCCESS != ret)
      break;
    *(uint16_t *)value = power.minPower;
    break;
  }

  case TMR_PARAM_RADIO_READPOWER:
    ret = TMR_SR_cmdGetReadTxPower(reader, (uint16_t *)value);
    break;

  case TMR_PARAM_RADIO_WRITEPOWER:
    ret = TMR_SR_cmdGetWriteTxPower(reader, (uint16_t *)value);
    break;


  case TMR_PARAM_ANTENNA_CHECKPORT:
    readerkey = TMR_SR_CONFIGURATION_SAFETY_ANTENNA_CHECK;
    break;

  case TMR_PARAM_TAGREADDATA_RECORDHIGHESTRSSI:
    readerkey = TMR_SR_CONFIGURATION_RECORD_HIGHEST_RSSI;
    break;

  case TMR_PARAM_TAGREADDATA_REPORTRSSIINDBM:
    readerkey = TMR_SR_CONFIGURATION_RSSI_IN_DBM;
    break;

  case TMR_PARAM_TAGREADDATA_UNIQUEBYANTENNA:
    readerkey = TMR_SR_CONFIGURATION_UNIQUE_BY_ANTENNA;
    break;

  case TMR_PARAM_TAGREADDATA_UNIQUEBYDATA:
    readerkey = TMR_SR_CONFIGURATION_UNIQUE_BY_DATA;
    break;

  case TMR_PARAM_ANTENNA_PORTSWITCHGPOS:
  {
    uint8_t portmask;
    TMR_uint8List *u8list;

    u8list = value;

    ret = TMR_SR_cmdGetReaderConfiguration(
      reader, TMR_SR_CONFIGURATION_ANTENNA_CONTROL_GPIO, &portmask);
    if (TMR_SUCCESS != ret)
    {
      break;
    }
    u8list->len = 0;
    if (portmask & 1)
    {
      LISTAPPEND(u8list, 1);
    }
    if (portmask & 2)
    {
      LISTAPPEND(u8list, 2);
    }
    break;
  }

  case TMR_PARAM_ANTENNA_SETTLINGTIMELIST:
    ret = getPortValues(reader, value, 2);
    break;

  case TMR_PARAM_RADIO_PORTREADPOWERLIST:
    ret = getPortValues(reader, value, 0);
    break;

  case TMR_PARAM_RADIO_PORTWRITEPOWERLIST:
    ret = getPortValues(reader, value, 1);
    break;

  case TMR_PARAM_GPIO_INPUTLIST:
  case TMR_PARAM_GPIO_OUTPUTLIST:
  {
    TMR_uint8List *u8list;

    u8list = value;

    u8list->len = 0;
    if (TMR_SR_MODEL_M6E == sr->versionInfo.hardware[0])
    {
      int pin, wantout;
      bool out;

      wantout = (key == TMR_PARAM_GPIO_OUTPUTLIST) ? 1 : 0;
      if (-1 == sr->gpioDirections)
      {
        /* Cache the current state */
        sr->gpioDirections = 0;
        for (pin = 1; pin <= 4 ; pin++)
        {
          ret = TMR_SR_cmdGetGPIODirection(reader, pin, &out);
          if (TMR_SUCCESS != ret)
          {
            return ret;
          }
          if (out)
          {
            sr->gpioDirections |= 1 << pin;
          }
        }
      }
      for (pin = 1; pin <= 4 ; pin++)
      {
        if (wantout == ((sr->gpioDirections >> pin) & 1))
        {
          LISTAPPEND(u8list, pin);
        }
      }
    }
    else
    {
      LISTAPPEND(u8list, 1);
      LISTAPPEND(u8list, 2);
    }
    break;
  }

  case TMR_PARAM_ANTENNA_PORTLIST:
  {
    TMR_SR_PortDetect ports[TMR_SR_MAX_ANTENNA_PORTS];
    uint8_t i, numPorts;
    TMR_uint8List *u8list;

    u8list = value;

    numPorts = TMR_SR_MAX_ANTENNA_PORTS;
    ret = TMR_SR_cmdAntennaDetect(reader, &numPorts, ports);
    if (TMR_SUCCESS != ret)
    {
      break;
    }
    u8list->len = 0;
    for (i = 0; i < numPorts; i++)
    {
      LISTAPPEND(u8list, ports[i].port);
    }
    break;
  }

  case TMR_PARAM_ANTENNA_CONNECTEDPORTLIST:
  {
    TMR_SR_PortDetect ports[TMR_SR_MAX_ANTENNA_PORTS];
    uint8_t i, numPorts;
    TMR_uint8List *u8list;

    u8list = value;

    numPorts = numberof(ports);
    ret = TMR_SR_cmdAntennaDetect(reader, &numPorts, ports);
    if (TMR_SUCCESS != ret)
    {
      break;
    }
    u8list->len = 0;
    for (i = 0; i < numPorts; i++)
    {
      if (ports[i].detected)
      {
        LISTAPPEND(u8list, ports[i].port);
      }
    }
    break;
  }

  case TMR_PARAM_ANTENNA_TXRXMAP:
  {
    TMR_AntennaMapList *map, *mymap;
    uint16_t i;

    map = value;
    mymap = sr->txRxMap;

    for (i = 0 ; i < mymap->len && i < map->max ; i++)
      map->list[i] = mymap->list[i];
    map->len = mymap->len;
    break;
  }

  case TMR_PARAM_REGION_HOPTABLE:
  {
    TMR_uint32List *u32List;
    uint8_t count;

    u32List = value;

    count = (uint8_t)u32List->max;
    ret = TMR_SR_cmdGetFrequencyHopTable(reader,&count, u32List->list);
    if (TMR_SUCCESS != ret)
    {
      break;
    }
    u32List->len = count;
    break;
  }

  case TMR_PARAM_REGION_HOPTIME:
    ret = TMR_SR_cmdGetFrequencyHopTime(reader, value);
    break;

  case TMR_PARAM_REGION_LBT_ENABLE:
    ret = TMR_SR_cmdGetRegionConfiguration(reader,
                                           TMR_SR_REGION_CONFIGURATION_LBT_ENABLED, value);
    if (TMR_SUCCESS != ret && TMR_ERROR_IS_CODE(ret))
    {
      *(bool *)value = false;
      ret = TMR_SUCCESS;
    }
    break;

  case TMR_PARAM_TAGOP_ANTENNA:
    *(uint8_t *)value = reader->tagOpParams.antenna;
    break;

  case TMR_PARAM_TAGOP_PROTOCOL:
    *(TMR_TagProtocol *)value = reader->tagOpParams.protocol;
    break;

  case TMR_PARAM_POWERMODE:
    if (reader->connected)
    {
      TMR_SR_PowerMode pm;
      ret = TMR_SR_cmdGetPowerMode(reader, &pm);
      if (TMR_SUCCESS == ret)
      {
        sr->powerMode = pm;
      }
    }
    *(TMR_SR_PowerMode*)value = sr->powerMode;
    break;

  case TMR_PARAM_USERMODE:
    ret = TMR_SR_cmdGetUserMode(reader, value);
    break;

  case TMR_PARAM_GEN2_Q:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_Q;
    break;

  case TMR_PARAM_GEN2_TAGENCODING:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_TAGENCODING;
    break;

  case TMR_PARAM_GEN2_SESSION:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_SESSION;
    break;

  case TMR_PARAM_GEN2_TARGET:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_TARGET;
    break;

  case TMR_PARAM_GEN2_BLF:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_LINKFREQUENCY;
    break;

  case TMR_PARAM_GEN2_TARI:
    protokey.u.gen2 = TMR_SR_GEN2_CONFIGURATION_TARI;
    break;

  case TMR_PARAM_GEN2_WRITEMODE:
    *(TMR_GEN2_WriteMode *)value = sr->writeMode;
    break;

#ifdef TMR_ENABLE_ISO180006B
  case TMR_PARAM_ISO180006B_BLF:
    protokey.protocol = TMR_TAG_PROTOCOL_ISO180006B;
    protokey.u.iso180006b = TMR_SR_ISO180006B_CONFIGURATION_LINKFREQUENCY;
    break;
#endif /* TMR_ENABLE_ISO180006B */

  case TMR_PARAM_GEN2_ACCESSPASSWORD:
    *(TMR_GEN2_Password *)value = sr->gen2AccessPassword;
    break;

  case TMR_PARAM_REGION_SUPPORTEDREGIONS:
    ret = TMR_SR_cmdGetAvailableRegions(reader, value);
    break;

  case TMR_PARAM_VERSION_SUPPORTEDPROTOCOLS:
    ret = TMR_SR_cmdGetAvailableProtocols(reader, value);
    break;

  case TMR_PARAM_RADIO_TEMPERATURE:
    ret = TMR_SR_cmdGetTemperature(reader, value);
    break;

  case TMR_PARAM_VERSION_HARDWARE:
    ret = getHardwareInfo(reader, value);
    break;

  case TMR_PARAM_VERSION_SERIAL:
    ret = getSerialNumber(reader, value);
    break;

  case TMR_PARAM_VERSION_MODEL:
  {
    const char *model;

    switch (sr->versionInfo.hardware[0])
    {
    case TMR_SR_MODEL_M5E:
      model = "M5e";
      break;
    case TMR_SR_MODEL_M5E_COMPACT:
      model = "M5e Compact";
      break;
    case TMR_SR_MODEL_M5E_EU:
      model = "M5e EU";
      break;
    case TMR_SR_MODEL_M4E:
      model = "M4e";
      break;
    case TMR_SR_MODEL_M6E:
      model = "M6e";
      break;
    default:
      model = "Unknown";
    }
    TMR_stringCopy(value, model, (int)strlen(model));
    break;
  }    

  case TMR_PARAM_VERSION_SOFTWARE:
  {
    char tmp[38];
    TMR_SR_VersionInfo *info;

    info = &sr->versionInfo;

    TMR_hexDottedQuad(info->fwVersion, tmp);
    tmp[11] = '-';
    TMR_hexDottedQuad(info->fwDate, tmp + 12);
    tmp[23] = '-';
    tmp[24] = 'B';
    tmp[25] = 'L';
    TMR_hexDottedQuad(info->bootloader, tmp+26);
    TMR_stringCopy(value, tmp, 37);
    break;
  }

  default:
    ret = TMR_ERROR_NOT_FOUND;
  }

  switch (key)
  {
  case TMR_PARAM_ANTENNA_CHECKPORT:
  case TMR_PARAM_RADIO_ENABLEPOWERSAVE:
  case TMR_PARAM_TAGREADDATA_RECORDHIGHESTRSSI:
  case TMR_PARAM_TAGREADDATA_REPORTRSSIINDBM:
  case TMR_PARAM_TAGREADDATA_UNIQUEBYANTENNA:
  case TMR_PARAM_TAGREADDATA_UNIQUEBYDATA:
	  ret = TMR_SR_cmdGetReaderConfiguration(reader, readerkey, value);
	  break;

  case TMR_PARAM_GEN2_Q:
  case TMR_PARAM_GEN2_TAGENCODING:
  case TMR_PARAM_GEN2_SESSION:
  case TMR_PARAM_GEN2_TARGET:
  case TMR_PARAM_GEN2_BLF:
  case TMR_PARAM_GEN2_TARI:
#ifdef TMR_ENABLE_ISO180006B
  case TMR_PARAM_ISO180006B_BLF:
#endif /* TMR_ENABLE_ISO180006B */
	  ret = TMR_SR_cmdGetProtocolConfiguration(reader, protokey.protocol, 
		  protokey, value);
	  break;

  default:
	  ;
  }

  if (0 == BITGET(sr->paramConfirmed, key))
  {
    if (TMR_SUCCESS == ret)
    {
      BITSET(sr->paramPresent, key);
    }
    BITSET(sr->paramConfirmed, key);
  }

  return ret;
}

TMR_Status
TMR_SR_SerialReader_init(TMR_Reader *reader)
{

  reader->readerType = TMR_READER_TYPE_SERIAL;

#ifndef TMR__SERIAL_ONLY
  reader->connect = TMR_SR_connect;
  reader->destroy = TMR_SR_destroy;
  reader->read = TMR_SR_read;
  reader->hasMoreTags = TMR_SR_hasMoreTags;
  reader->getNextTag = TMR_SR_getNextTag;
  reader->readTagMemWords = TMR_SR_readTagMemWords;
  reader->readTagMemBytes = TMR_SR_readTagMemBytes;
#endif
  reader->paramSet = TMR_SR_paramSet;
  reader->paramGet = TMR_SR_paramGet;
 
  memset(reader->u.serialReader.paramConfirmed,0,
         sizeof(reader->u.serialReader.paramConfirmed));
  memset(reader->u.serialReader.paramPresent,0,
         sizeof(reader->u.serialReader.paramPresent));
  reader->u.serialReader.baudRate = 115200;
  reader->u.serialReader.powerMode = TMR_SR_POWER_MODE_INVALID;
  reader->u.serialReader.transportTimeout = 2000;
  reader->u.serialReader.commandTimeout = 2000;
  reader->u.serialReader.regionId = TMR_REGION_NONE;
  reader->u.serialReader.tagsRemaining = 0;
  reader->u.serialReader.tagsRemainingInBuffer = 0;
  reader->u.serialReader.gen2AccessPassword = 0;
  reader->u.serialReader.oldQ.type = TMR_SR_GEN2_Q_INVALID;
  return TMR_reader_init_internal(reader);
}

TMR_Status
TMR_SR_executeTagOp(struct TMR_Reader *reader, TMR_Tagop *tagop, TMR_TagFilter *filter, uint16_t *data)
{ 
  TMR_SR_SerialReader *sr;

  sr = &reader->u.serialReader;

  switch (tagop->type)
  {
  case (TMR_TAGOP_GEN2_KILL):
    {
      return TMR_SR_cmdKillTag(reader,
        (uint16_t)(sr->commandTimeout),
        tagop->u.gen2_kill.password,
        filter);
    }
  case (TMR_TAGOP_GEN2_LOCK):
    {
      return TMR_SR_cmdGEN2LockTag(reader, (uint16_t)sr->commandTimeout,
        tagop->u.gen2_lock.mask, 
        tagop->u.gen2_lock.action, 
        sr->gen2AccessPassword,
        filter);
    }
  case (TMR_TAGOP_GEN2_WRITEDATA):
    {
      return TMR_SR_writeTagMemWords(reader, filter, tagop->u.gen2_writeData.bank, tagop->u.gen2_writeData.wordAddress, tagop->u.gen2_writeData.data.len, tagop->u.gen2_writeData.data.list);
    }
  case (TMR_TAGOP_GEN2_READDATA):
    {
      return TMR_SR_readTagMemWords(reader, filter, tagop->u.gen2_readData.bank, tagop->u.gen2_readData.wordAddress, tagop->u.gen2_readData.len, data);
    }
  case (TMR_TAGOP_GEN2_BLOCKWRITE):
    {
      return TMR_SR_cmdBlockWrite(reader,(uint16_t)sr->commandTimeout, tagop->u.gen2_blockWrite.bank, tagop->u.gen2_blockWrite.wordPtr, tagop->u.gen2_blockWrite.wordCount, tagop->u.gen2_blockWrite.data, tagop->u.gen2_blockWrite.accessPassword, filter);
    }
  case (TMR_TAGOP_GEN2_BLOCKPERMALOCK):
    {
      return TMR_SR_cmdBlockPermaLock(reader, (uint16_t)sr->commandTimeout, tagop->u.gen2_blockPermaLock.readLock, tagop->u.gen2_blockPermaLock.bank, tagop->u.gen2_blockPermaLock.blockPtr, tagop->u.gen2_blockPermaLock.blockRange, tagop->u.gen2_blockPermaLock.mask, tagop->u.gen2_blockPermaLock.accessPassword, filter, data);
    }
  case (TMR_TAGOP_ISO180006B_READDATA):
    {
      return TMR_SR_readTagMemWords(reader, filter, 0 ,tagop->u.iso180006b_readData.wordAddress, tagop->u.iso180006b_readData.len, data);
    }
  case (TMR_TAGOP_ISO180006B_WRITEDATA):
    {
      return TMR_SR_writeTagMemWords(reader, filter, 0, tagop->u.iso180006b_writeData.wordAddress, tagop->u.iso180006b_writeData.data.len, tagop->u.iso180006b_writeData.data.list);
    }
  case (TMR_TAGOP_ISO180006B_LOCK):
    {
      return TMR_SR_cmdISO180006BLockTag(reader, (uint16_t)(sr->commandTimeout),tagop->u.iso180006b_lock.address, filter);

    }
  default:
    {
      return TMR_ERROR_UNIMPLEMENTED_FEATURE; 
    }

  }

}

#endif /* TMR_ENABLE_SERIAL_READER */
