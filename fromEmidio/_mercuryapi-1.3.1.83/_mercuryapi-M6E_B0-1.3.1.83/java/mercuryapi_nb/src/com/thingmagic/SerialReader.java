/*
 * Copyright (c) 2008 ThingMagic, Inc.
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
package com.thingmagic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

// Make the reader constants available here
import static com.thingmagic.EmbeddedReaderMessage.*;
import com.thingmagic.TagReadData.TagMetadataFlag;

/**
 * The SerialReader class is an implementation of a Reader object that
 * communicates with a ThingMagic embedded RFID module via the
 * embedded module serial protocol. In addition to the Reader
 * interface, direct access to the commands of the embedded module
 * serial protocol is supported.
 * <p>
 *
 * Instances of the SerialReader class are created with the {@link
 * #com.thingmagic.Reader.create} method with a "eapi" URI or a
 * generic "tmr" URI that references a local serial port.
 */
public class SerialReader extends Reader 
{
  // Connection state and (relatively) fixed values
  String serialDevice;
  SerialTransport st;
  VersionInfo versionInfo;
  Set<TagProtocol> protocolSet;
  int[] powerLimits;
  int[] ports;
  Set<Integer> portSet;  
  Region region;
  boolean useStreaming;
  int opCode;  //stores command code of message sent
  // Values affected by parameter operations
  int currentAntenna;
  int[] searchList;
  TagProtocol currentProtocol;
  int accessPassword;
  int transportTimeout;
  int commandTimeout;
  int baudRate;
  int[][] portParamList;
  PowerMode powerMode = PowerMode.INVALID;
  byte gpioDirections = (byte)0xFF;

  Map<Integer,int[]> antennaPortMap;
  Map<Integer,Integer> antennaPortReverseMap;
  Map<Integer,Integer> antennaPortTransmitMap;

  static final VersionNumber m4eType1 = new VersionNumber(0xff,0xff,0xff,0xff);
  static final VersionNumber m4eType2 = new VersionNumber(0x01,0x01,0x00,0x00);
  public static final int
    TMR_SR_MODEL_M5E         = 0x00,
    TMR_SR_MODEL_M5E_COMPACT = 0x01,
    TMR_SR_MODEL_M5E_EU      = 0x02,
    TMR_SR_MODEL_M4E         = 0x03,
    TMR_SR_MODEL_M6E         = 0x18;

  static final Map<TagProtocol,Integer> protocolToCodeMap;
  static
  {
    protocolToCodeMap = new EnumMap<TagProtocol,Integer>(TagProtocol.class);
    protocolToCodeMap.put(TagProtocol.ISO180006B, (int)PROT_ISO180006B);
    protocolToCodeMap.put(TagProtocol.GEN2, (int)PROT_GEN2);
    protocolToCodeMap.put(TagProtocol.ISO180006B_UCODE, (int)PROT_UCODE);
    protocolToCodeMap.put(TagProtocol.IPX64, (int)PROT_IPX64);
    protocolToCodeMap.put(TagProtocol.IPX256, (int)PROT_IPX256);
  }

  static final Map<Integer,TagProtocol> codeToProtocolMap;
  static
  {
    codeToProtocolMap = new HashMap<Integer,TagProtocol>();
    codeToProtocolMap.put((int)PROT_ISO180006B, TagProtocol.ISO180006B);
    codeToProtocolMap.put((int)PROT_GEN2, TagProtocol.GEN2);
    codeToProtocolMap.put((int)PROT_UCODE, TagProtocol.ISO180006B_UCODE);
    codeToProtocolMap.put((int)PROT_IPX64, TagProtocol.IPX64);
    codeToProtocolMap.put((int)PROT_IPX256, TagProtocol.IPX256);
  }

  static final Map<TagMetadataFlag,Integer> tagMetadataFlagValues;
  static
  {
    tagMetadataFlagValues = new EnumMap<TagMetadataFlag,Integer>(TagMetadataFlag.class);
    tagMetadataFlagValues.put(TagMetadataFlag.READCOUNT, TAG_METADATA_READCOUNT);
    tagMetadataFlagValues.put(TagMetadataFlag.RSSI, TAG_METADATA_RSSI);
    tagMetadataFlagValues.put(TagMetadataFlag.ANTENNAID, TAG_METADATA_ANTENNAID);
    tagMetadataFlagValues.put(TagMetadataFlag.FREQUENCY, TAG_METADATA_FREQUENCY);
    tagMetadataFlagValues.put(TagMetadataFlag.TIMESTAMP, TAG_METADATA_TIMESTAMP);
    tagMetadataFlagValues.put(TagMetadataFlag.PHASE, TAG_METADATA_PHASE);
    tagMetadataFlagValues.put(TagMetadataFlag.PROTOCOL, TAG_METADATA_PROTOCOL);
    tagMetadataFlagValues.put(TagMetadataFlag.DATA, TAG_METADATA_DATA);
    tagMetadataFlagValues.put(TagMetadataFlag.GPIO_STATUS, TAG_METADATA_GPIO_STATUS);
    tagMetadataFlagValues.put(TagMetadataFlag.ALL, TAG_METADATA_ALL);
  }

  static final Map<Reader.Region, Integer> regionToCodeMap;
  static
  {
    regionToCodeMap = new EnumMap<Reader.Region, Integer>(Reader.Region.class);
    regionToCodeMap.put(Reader.Region.NA, 1);
    regionToCodeMap.put(Reader.Region.EU, 2);
    regionToCodeMap.put(Reader.Region.KR, 3);
    regionToCodeMap.put(Reader.Region.IN, 4);
    regionToCodeMap.put(Reader.Region.JP, 5);
    regionToCodeMap.put(Reader.Region.KR2, 9);
    regionToCodeMap.put(Reader.Region.PRC, 6);
    regionToCodeMap.put(Reader.Region.EU2, 7);
    regionToCodeMap.put(Reader.Region.EU3, 8);
    regionToCodeMap.put(Reader.Region.OPEN, 255);
  }

  static final Map<Integer, Reader.Region> codeToRegionMap;
  static
  {
    codeToRegionMap = new HashMap<Integer, Reader.Region>();
    codeToRegionMap.put(1, Reader.Region.NA);
    codeToRegionMap.put(2, Reader.Region.EU);
    codeToRegionMap.put(3, Reader.Region.KR);
    codeToRegionMap.put(4, Reader.Region.IN);
    codeToRegionMap.put(5, Reader.Region.JP);
    codeToRegionMap.put(9, Reader.Region.KR2);
    codeToRegionMap.put(6, Reader.Region.PRC);
    codeToRegionMap.put(7, Reader.Region.EU2);
    codeToRegionMap.put(8, Reader.Region.EU3);
    codeToRegionMap.put(255, Reader.Region.OPEN);
  }

  static class Message
  {
    byte[] data;
    int writeIndex;
    int readIndex;

    Message()
    {
      this(256);
    }

    Message(int size)
    {
      data = new byte[size];
      writeIndex = 2;
    }

    void setu8(int val)
    {
      data[writeIndex++] = (byte)(val & 0xff);
    }

    void setu16(int val)
    {
      data[writeIndex++] = (byte)((val >> 8) & 0xff);
      data[writeIndex++] = (byte)((val >> 0) & 0xff);
    }

    void setu32(int val)
    {
      data[writeIndex++] = (byte)((val >> 24) & 0xff);
      data[writeIndex++] = (byte)((val >> 16) & 0xff);
      data[writeIndex++] = (byte)((val >>  8) & 0xff);
      data[writeIndex++] = (byte)((val >>  0) & 0xff);
    }

    void setbytes(byte[] array)
    {
     if(array!=null)
     {
       setbytes(array, 0, array.length);
     }
    }

    void setbytes(byte[] array, int start, int length)
    {
      System.arraycopy(array, start, data, writeIndex, length);
      writeIndex += length;
    }

    int getu8()
    {
      return getu8at(readIndex++);
    }

    int getu16()
    {
      int val;
      val = getu16at(readIndex);
      readIndex += 2;
      return val;
    }

    int getu24()
    {
      int val;
      val = getu24at(readIndex);
      readIndex += 3;
      return val;
    }

    int getu32()
    {
      int val;
      val = getu32at(readIndex);
      readIndex += 4;
      return val;
    }

    void getbytes(byte[] destination, int length)
    {
      System.arraycopy(data, readIndex, destination, 0, length);
      readIndex += length;
    }

    int getu8at(int offset)
    {
      return data[offset] & 0xff;
    }

    int getu16at(int offset)
    {
      return ( (data[offset] & 0xff) <<  8)
        | ((data[offset + 1] & 0xff) <<  0);
    }

    int getu24at(int offset)
    {
      return ( (data[offset] & 0xff) << 16)
        | ((data[offset + 1] & 0xff) <<  8)
        | ((data[offset + 0] & 0xff) <<  0);
    }

    int getu32at(int offset)
    {
      return ( (data[offset] & 0xff) << 24)
        | ((data[offset + 1] & 0xff) << 16)
        | ((data[offset + 2] & 0xff) <<  8)
        | ((data[offset + 3] & 0xff) <<  0);
    }


  }
  

  /* (note: explicitly not a javadoc)
   * Send a raw message to the serial reader.
   *
   * @param timeout the duration in milliseconds to wait for a response
   * @param message The bytes of the message to send to the reader,
   * starting with the opcode. The message header, length, and
   * trailing CRC are not included. The message can not be empty, or
   * longer than 251 bytes.
   * @return The bytes of the response, from the opcode to the end of
   * the message. Header, length, and CRC are not included.
   * @throws ReaderCommException in the event of a timeout (failure to
   * recieve a complete message in the specified time) or a CRC
   * error. Does not generate exceptions for non-zero status
   * responses.
   */
  public synchronized byte[] cmdRaw(int timeout, byte... message)
    throws ReaderException
  {
    Message m = new Message();

    if (message.length < 1)
    {
      throw new IllegalArgumentException("Raw serial message can not be empty");
    }

    System.arraycopy(message, 0, m.data, 2, message.length);

    sendTimeout(timeout, m);

    int len = m.getu8at(1);
    byte[] response = new byte[len];
    System.arraycopy(m.data, 2, response, 0, len);
    return response;
  }

  private synchronized void sendMessage(int timeout, Message m)
    throws ReaderException
  {
    /* Wake up processor from deep sleep.  Tickle the RS-232 line, then
     * wait a fixed delay while the processor spins up communications again. */
    if ((powerMode == powerMode.INVALID) || (powerMode.value >= PowerMode.MEDSAVE.value))
      {
      byte[] flushBytes = {
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
  (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF,
 };
      /* Calculate fixed delay in terms of byte-lengths at current speed */
      /** @todo Optimize delay length.  This value (100 bytes at 9600bps) is taken
       * directly from arbser, which was itself using a hastily-chosen value.*/ 
      int bytesper100ms = baudRate/100;
      
      for(int bytesSent=0;bytesSent<bytesper100ms;bytesSent+=flushBytes.length)
      {
        st.sendBytes(flushBytes.length, flushBytes, 0, transportTimeout);
        if (hasSerialListeners)
        {
          byte[] message = new byte[flushBytes.length];
          System.arraycopy(flushBytes, 0, message, 0, flushBytes.length);
          for (TransportListener l : serialListeners)
          {
            l.message(true, message, transportTimeout);
          }
        }
      }
    }
    m.data[0] = (byte)0xff;
    m.data[1] = (byte)(m.writeIndex - 3);

    opCode = m.data[2] & 0xff;  //Save opCode to check on Receive

    m.setu16(calcCrc(m.data, 1, m.writeIndex - 1));

    int len = m.writeIndex;

    if (hasSerialListeners)
    {
      byte[] message = new byte[len];
      System.arraycopy(m.data, 0, message, 0, len);
      for (TransportListener l : serialListeners)
      {
        l.message(true, message, timeout + transportTimeout);
      }
    }

    st.sendBytes(len, m.data, 0, timeout + transportTimeout);
  }

  private synchronized void receiveMessage(int timeout, Message m)
    throws ReaderException
  {
    int sofPosition;
    boolean sofFound=false;
    long enterTime=System.currentTimeMillis();
    while((System.currentTimeMillis()-enterTime)<timeout)
    {
      byte[] receiveBytes = st.receiveBytes(7, m.data, 0, timeout + transportTimeout);
      if(m.data[0]!=(byte)0xff)
      {
        int i=0;
        for(i=1;i<6;i++)
        {
          if(m.data[i]==(byte)0xff)
          {
            sofPosition=i;
            sofFound=true;
            System.arraycopy(m.data,sofPosition,m.data,0,7-sofPosition);
            st.receiveBytes(7-sofPosition, m.data,sofPosition, timeout + transportTimeout);
            break;
          }
        }
        if(sofFound)
          break;

      }
      else
      {
        sofFound=true;
        break;
      }
    }
    if(sofFound==false)
      {
        if (hasSerialListeners)
        {
          // Keep value of len set to length of data
          byte[] message = new byte[7];
          System.arraycopy(m.data, 0, message, 0, 7);
          for (TransportListener l : serialListeners)
          {
            l.message(false, message, timeout + transportTimeout);
          }
        }
        throw new ReaderCommException(
        String.format("No soh FOund"));
      }

    int len = m.data[1] & 0xff;
    //Now pull in the rest of the data, if exists, + the CRC

    if (len != 0) st.receiveBytes(len, m.data, 7, timeout + transportTimeout);

    if (hasSerialListeners)
    {
      // Keep value of len set to length of data
      byte[] message = new byte[len+7];
      System.arraycopy(m.data, 0, message, 0, len+7);
      for (TransportListener l : serialListeners)
      {
        l.message(false, message, timeout + transportTimeout);
      }
    }

    
    
    //Calculate the crc for the data
    int crc = calcCrc(m.data, 1, len+4);
    //Compare with message's crc
    if ((m.data[len+5] != (byte)((crc >> 8) & 0xff)) ||
        (m.data[len+6] != (byte)(crc & 0xff)))
    {    
      throw new ReaderCommException(
        String.format("Reader failed crc check.  Message crc %x %x data crc %x %x",  m.data[len+5],m.data[len+6],(crc >> 8 & 0xff), (crc&0xff)));

    }

    if ((m.data[2] != (byte) opCode)&& (m.data[2] != 0x2F || !useStreaming))
    {
      /* We got a response for a different command than the one we
       * sent. This usually means we recieved the boot-time message from
       * a M6e, and thus that the device was rebooted somewhere between
       * the previous command and this one. Report this as a problem.
     */
      throw new ReaderCommException(
        String.format("Device was reset externally.  "+
                      "Response opcode (%02x) did not match command (%02x)",
                      opCode, m.data[2]));
    }


    int status = m.getu16at(3);

    if ((status & 0x7f00) == 0x7f00)
    {
      // Module assertion. Decode the assert string from the response.
      int lineNum;

      lineNum = m.getu32at(5);
      
      throw new ReaderFatalException(
        String.format("Reader assert 0x%x at %s:%d", status, 
                      new String(m.data, 9, m.writeIndex - 9), lineNum));
    }

    if (status != TM_SUCCESS)
    {
      throw new ReaderCodeException(status);
    }

    m.writeIndex = 5 + (m.data[1] & 0xff);  //Set the write index to start of CRC
    m.readIndex = 5;                        //Set read index to start of message

  }

  private synchronized Message sendTimeout(int timeout, Message m)
    throws ReaderException
  {
    
    sendMessage(timeout, m);
    receiveMessage(timeout, m);

    return m;
  }

  private Message send(Message m)
    throws ReaderException
  {
    
    return sendTimeout(commandTimeout, m);
  }

  private Message sendOpcode(int opcode)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(opcode);
    return sendTimeout(commandTimeout, m);
  }

  // ThingMagic-mutated CRC used for messages.
  // Notably, not a CCITT CRC-16, though it looks close.
  private static int crcTable[] = 
  {
    0x0000, 0x1021, 0x2042, 0x3063,
    0x4084, 0x50a5, 0x60c6, 0x70e7,
    0x8108, 0x9129, 0xa14a, 0xb16b,
    0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
  };

  // calculates ThingMagic's CRC-16
  private static short calcCrc(byte[] message, int offset, int length)
  {
    int crc = 0xffff;

    for (int i = offset; i < offset + length; i++)
    {
      crc = ((crc << 4) | ((message[i] >> 4) & 0xf)) ^ crcTable[crc >> 12];
      crc &= 0xffff;
      crc = ((crc << 4) | ((message[i] >> 0) & 0xf)) ^ crcTable[crc >> 12];
      crc &= 0xffff;
    }
    return (short)crc;
  }

  protected List<TransportListener> serialListeners;
  protected boolean hasSerialListeners;

  /** 
   * Register an object to be notified of serial packets.
   *
   * @param l the SerialListener to add
   */
  public void addTransportListener(TransportListener l)
  {
    serialListeners.add(l);
    hasSerialListeners = true;
  }

  class ListenerAdapter implements TransportListener
  {
    public void message(boolean tx, byte[] data, int timeout)
    {
      for (TransportListener l : serialListeners)
      {
        l.message(tx, data, timeout);
      }
    }
  }

  TransportListener listenerAdapter = new ListenerAdapter();

  public void removeTransportListener(TransportListener l)
  {
    serialListeners.remove(l);
    if (serialListeners.isEmpty())
    {
      hasSerialListeners = false;
    }
  }

  /**
   * Set the baud rate of the serial port in use.  
   * <p>
   *
   * NOTE: This is a low-level command and should only be used in
   * conjunction with cmdSetBaudRate() or cmdBootBootloader()
   * below. For changing the rate used by the API in general, see the
   * "/reader/baudRate" parameter.
   */
  public void setSerialBaudRate(int rate)
    throws ReaderException
  {
    st.setBaudRate(rate);
  }

  // "Level 3" interface - direct wrappers around specific serial
  // commands


  /**
   * This class represents a version number for a component of the module.
   * Instances of this class are immutable.
   */
  public static final class VersionNumber
    implements Comparable<VersionNumber>
  {
    final int part1, part2, part3, part4;
    final long compositeVersion;

    /**
     * Construct a new VersionNumber object given the individual components.
     * Note that all version number components are discussed and
     * presented in hexadecimal format, that is, in the version number
     * "9.5.12.0", the 12 is 0x12 and should be passed to this
     * constructor as such.
     *
     * @param part1 the first part of the version number
     * @param part2 the second part of the version number
     * @param part3 the third part of the version number
     * @param part4 the fourth part of the version number
     */
    public VersionNumber(int all)
    {
      part1 = (all >> 24) & 0xff;
      part2 = (all >> 16) & 0xff;
      part3 = (all >>  8) & 0xff;
      part4 = (all >>  0) & 0xff;
      compositeVersion = all;
    }

    public VersionNumber(int part1, int part2, int part3, int part4)
    {
      if ((part1 < 0 || part1 > 0xff) ||
          (part2 < 0 || part2 > 0xff) ||
          (part3 < 0 || part3 > 0xff) ||
          (part4 < 0 || part4 > 0xff))
      {
        throw new IllegalArgumentException(
          "Version field not in range 0x0-0xff");
      }
          
      this.part1 = part1;
      this.part2 = part2;
      this.part3 = part3;
      this.part4 = part4;
      compositeVersion =
        (part1 << 24) | 
        (part2 << 16) |
        (part3 <<  8) |
        (part4 <<  0);
    }

    public int compareTo(VersionNumber v)
    {
      return (int)(this.compositeVersion - v.compositeVersion);
    }

    @Override
    /**
     * Return a string representation of the version number, as a
     * sequence of four two-digit hexadecimal numbers separated by
     * dots, for example "09.05.12.0".
     */
    public String toString()
    {
      return String.format("%02x.%02x.%02x.%02x", part1, part2, part3, part4);
    }

    @Override
    public boolean equals(Object o)
    {
      if (!(o instanceof VersionNumber))
      {
        return false;
      }
      VersionNumber vn = (VersionNumber)o;
      return compositeVersion == vn.compositeVersion;
    }

    @Override
    public int hashCode()
    {
      return (int)compositeVersion;
    }

  }

  /**
   * Container class for the version information about the device,
   * including a list of the protocols that are supported.
   */
  public static final class VersionInfo
  {
    public VersionNumber bootloader;
    public VersionNumber hardware;
    public VersionNumber fwDate;
    public VersionNumber fwVersion;
    public TagProtocol[] protocols;
  }

  /**
   * Read the contents of flash from the specified address in the specified flash sector.
   *
   * @deprecated
   * @param sector the flash sector, as described in the embedded module user manual
   * @param address the byte address to start reading from
   * @param length the number of bytes to read. Limited to 248 bytes.
   * @return the bytes read
   */

  public byte[] cmdReadFlash(int sector, int address, int length)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_READ_FLASH);
    m.setu32(address);
    m.setu8(sector);
    m.setu8(length);

    sendTimeout(3000, m);

    byte[] response = new byte[length];
    System.arraycopy(m.data, 5, response, 0, length);

    return response;
  }

  /**
   * Get the version information about the device.
   *
   * @deprecated
   * @return the VersionInfo structure describing the device.
   */
  public VersionInfo cmdVersion()
    throws ReaderException
  {

    Message m = new Message();
    m.setu8(MSG_OPCODE_VERSION);
    sendTimeout(100, m);

    VersionInfo  v = new VersionInfo();

    v.bootloader = new VersionNumber(m.getu32());
    v.hardware = new VersionNumber(m.getu32());
    v.fwDate = new VersionNumber(m.getu32());
    v.fwVersion = new VersionNumber(m.getu32());

    int protocolBits = m.getu32();
    int protocolCount = 0;
    for (int i = 0 ; i < 8; i++)
    {
      if ((protocolBits & (1 << i)) != 0)
      {
        protocolCount++;
      }
    }

    v.protocols = new TagProtocol[protocolCount];
    int j = 0;
    for (int i = 0 ; i < 8; i++)
    {
      if ((protocolBits & (1 << i)) != 0)
      {
        TagProtocol p = codeToProtocolMap.get(i + 1);
        v.protocols[j++] = p;
      }
    }

    return v;
  }

  /**
   * Tell the boot loader to verify the application firmware and execute it.
   *
   * @deprecated
   */
  public void cmdBootFirmware()
    throws ReaderException
  {
    sendOpcode(MSG_OPCODE_BOOT_FIRMWARE);
  }

  /**
   * Tell the device to change the baud rate it uses for
   * communication. Note that this does not affect the host side of
   * the serial interface; it will need to be changed separately.
   *
   * @deprecated
   * @param rate the new baud rate to use.
   */
  public void cmdSetBaudRate(int rate)
    throws ReaderException
  {
    Message m = new Message();
    int val;

    m.setu8(MSG_OPCODE_SET_BAUD_RATE);

    val = 0;
    switch (rate)
    {
    case 9600:   val = 488; break;
    case 19200:  val = 244; break;
    case 38400:  val = 122; break;
    case 57600:  val =  84; break;
    case 115200: val =  41; break; 
    case 230400: val =  20; break;
    case 460800: val =  10; break;
    case 921600: val =   5; break;
    }

    if (0 != val)
    {
      m.setu16(val);
    }
    else
    {
      m.setu32(rate);
    }
    send(m);
  }

  /**
   * Verify that the application image in flash has a valid checksum.
   * The device will report an invalid checksum with a error code
   * response, which would normally generate a ReaderCodeException;
   * this routine traps that particular exception and simply returns
   * "false".
   *
   * @deprecated
   * @return whether the image is valid
   */
  public boolean cmdVerifyImage()
    throws ReaderException
  {
    try 
    {
      sendOpcode(MSG_OPCODE_VERIFY_IMAGE_CRC);
    }
    catch (ReaderCodeException re)
    {
      if (re.getCode() == FAULT_BL_INVALID_IMAGE_CRC)
      {
        return false;
      }
      throw re;
    }
    return true;
  }

  /**
   * Erase a sector of the device's flash.
   *
   * @param sector the flash sector, as described in the embedded
   * module user manual
   * @param password the erase password for the sector
   */
  public void cmdEraseFlash(int sector, int password)
    throws ReaderException
  {
    if (sector < 0 || sector > 255)
    {
      throw new IllegalArgumentException("illegal sector " + sector);
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_ERASE_FLASH);
    m.setu32(password);
    m.setu8(sector);

    sendTimeout(30000, m);
  }

  /**
   * Write data to a previously erased region of the device's flash.
   *
   * @deprecated
   * @param sector the flash sector, as described in the embedded module user manual
   * @param address the byte address to start writing from
   * @param password the write password for the sector
   * @param length the amount of data to write. Limited to 240 bytes.
   * @param data the data to write (from offset to offset + length - 1)
   * @param offset the index of the data to be writtin in the data array
   */
  public void cmdWriteFlash(int sector, int address, int password,
                            int length, byte[] data, int offset)
    throws ReaderException
  {

    if (sector < 0 || sector > 255)
    {
      throw new IllegalArgumentException("illegal sector " + sector);
    }
    if (length > 240)
    {
      throw new IllegalArgumentException("data too long");
    }
    if (length > data.length - offset)
    {
      throw new IllegalArgumentException("not enough data supplied");
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_WRITE_FLASH_SECTOR);
    m.setu32(password);
    m.setu32(address);
    m.setu8(sector);
    m.setbytes(data, offset, length);

    sendTimeout(3000, m);
  }

  /**
   * Return the size of a flash sector of the device.
   *
   * @param sector the flash sector, as described in the embedded module user manual
   */
  public int cmdGetSectorSize(int sector)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_GET_SECTOR_SIZE);
    m.setu8(sector);
    send(m);

    return m.getu32();
  }

  /**
   * Write data to the device's flash, erasing if necessary.
   *
   * @deprecated
   * @param sector the flash sector, as described in the embedded module user manual
   * @param address the byte address to start writing from
   * @param password the write password for the sector
   * @param length the amount of data to write. Limited to 240 bytes.
   * @param data the data to write (from offset to offset + length - 1)
   * @param offset the index of the data to be writtin in the data array
   */
  public void cmdModifyFlash(int sector, int address, int password,
                             int length, byte[] data, int offset)
    throws ReaderException
  {
    int[] msgBody;

    if (sector < 0 || sector > 255)
    {
      throw new IllegalArgumentException("illegal sector " + sector);
    }
    if (length > 240)
    {
      throw new IllegalArgumentException("data too long");
    }
    if (length > data.length)
    {
      throw new IllegalArgumentException("not enough data supplied");
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_MODIFY_FLASH_SECTOR);
    m.setu32(password);
    m.setu32(address);
    m.setbytes(data, offset, length);

    sendTimeout(3000, m);
  }
  
  /**
   * Quit running the application and execute the bootloader. Note
   * that this changes the reader's baud rate to 9600; the user is
   * responsible for changing the local serial sort speed.
   *
   * @deprecated
   */ 
  public void cmdBootBootloader()
    throws ReaderException
  {
    sendOpcode(MSG_OPCODE_BOOT_BOOTLOADER);
  }

  /** 
   * Return the identity of the program currently running on the
   * device (bootloader or application).
   */ 
  public int cmdGetCurrentProgram()
    throws ReaderException
  {
    Message m;

    m = sendOpcode(MSG_OPCODE_GET_CURRENT_PROGRAM);
    return m.getu8();
  }


  /**
   * The antenna configuration to use for {@link #cmdReadTagMultiple}.
   */
  public enum AntennaSelection
  {
      CONFIGURED_ANTENNA (0),
      ANTENNA_1_THEN_2   (1),
      ANTENNA_2_THEN_1   (2),
      CONFIGURED_LIST    (3);

      final int value;
      AntennaSelection(int v)
      {
        value = v;
      }
  }
  

  /**
   * Search for a single tag for up to a specified amount of time.
   *
   * @deprecated 
   * @param timeout the duration in milliseconds to search for a tag. Valid range is 0-65535
   * @param metadataFlags the set of metadata values to retrieve and store in the returned object
   * @param filter a specification of the air protocol filtering to perform
   * @param protocol the protocol to expect in the returned data
   * @return a TagReadData object containing the tag found and the
   * metadata associated with the successful search, or null if no tag
   * is found.
   */
  public TagReadData cmdReadTagSingle(int timeout, 
                                      Set<TagMetadataFlag> metadataFlags,
                                      TagFilter filter, TagProtocol protocol)
    throws ReaderException
  {
    Message m;
    TagReadData tr;
    int optByte, option, metadataBits, epcLen;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    m = new Message();
    m.setu8(MSG_OPCODE_READ_TAG_ID_SINGLE);

    option = 0;
    metadataBits = tagMetadataSetValue(metadataFlags);

    if (metadataBits != 0)
    {
      option |= 0x10;
    }

    m.setu16(timeout);
    optByte = m.writeIndex++;
    if (metadataBits != 0)
    {
      m.setu16(metadataBits);
    }
    filterBytes(protocol, m, optByte, filter, 0, true);
    m.data[optByte] |= option;

    long startTime = System.currentTimeMillis();
    sendTimeout(timeout, m);

    tr = new TagReadData();
    metadataFromMessage(tr, m, metadataFlags);
    epcLen = m.writeIndex - m.readIndex;
    tr.tag = parseTag(m, epcLen, protocol);
    tr.readBase = startTime;
    return tr;
  }

  private void msgSetupReadTagMultiple(Message m, int timeout,
                                       int searchFlags,
                                       TagFilter filter, TagProtocol protocol,
                                       TagMetadataFlag metadataFlags,
                                       int accessPassword)
  {
    int optByte;

    m.setu8(MSG_OPCODE_READ_TAG_ID_MULTIPLE);
    optByte = m.writeIndex++;
    if (useStreaming)
    {
        m.setu16(searchFlags | READ_MULTIPLE_SEARCH_FLAGS_TAG_STREAMING|READ_MULTIPLE_SEARCH_FLAGS_LARGE_TAG_POPULATION_SUPPORT);
    }
    else
    {
        m.setu16(searchFlags);
    }
    m.setu16(timeout);

    if (useStreaming)
    {
        m.setu16(allMetaBits);
    }

    // We skip filterbytes() for a null filter and gen2 0 access
    // password so that we don't pass any filtering information at all
    // unless necessary; for some protocols (such as ISO180006B) the
    // "null" filter is not zero-length, but we don't need to send
    // that with this command.

    if (null != filter || (TagProtocol.GEN2 == protocol && 0 == accessPassword))
    {
      filterBytes(protocol, m, optByte, filter, accessPassword, true);
    }
    if (useStreaming)
    {
        m.data[optByte] |= SINGULATION_FLAG_METADATA_ENABLED;
    }
  }

  private void msgAddGEN2DataRead(Message m, int timeout,
                                  int metadataBits, Gen2.Bank bank,
                                  int address, int length)
  {
    m.setu8(MSG_OPCODE_READ_TAG_DATA);
    m.setu16(timeout);
    m.setu8(0);  // Option byte - initialize
    if (0 != metadataBits)
    {
      m.setu16(metadataBits);
    }
    m.setu8(bank.rep);
    m.setu32(address);
    m.setu8(length);
  }


  private void msgAddGEN2DataWrite(Message m, int timeout,
                                   Gen2.Bank bank, int address)
  {
    m.setu8(MSG_OPCODE_WRITE_TAG_DATA);
    m.setu16(timeout);
    m.setu8(0);  // Option byte - initialize
    m.setu32(address);
    m.setu8(bank.rep);
  }

  private void msgAddGEN2LockTag(Message m, int timeout, int mask, int action,
                                 int password)
  {
    m.setu8(MSG_OPCODE_LOCK_TAG);
    m.setu16(timeout);
    m.setu8(0); // Option byte - initialize
    m.setu32(password);
    m.setu16(mask);
    m.setu16(action);
  }

  private void msgAddGEN2KillTag(Message m, int timeout, int killPassword)
  {
    m.setu8(MSG_OPCODE_KILL_TAG);
    m.setu16(timeout);
    m.setu8(0); // Option byte - initialize
    m.setu32(killPassword);
    m.setu8(0); // RFU
  }

  private void msgAddGEN2BlockWrite(Message m, int timeout,Gen2.Bank bank,
                                  int wordPtr,byte wordCount,byte[] data,int accessPassword,TagFilter target)
  throws ReaderException
  {
    int optByte;
    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC); //opcode
    m.setu16(timeout);
    m.setu8(0x00);//chip type
    optByte = m.writeIndex++;
    m.setu8(0x00);//block write opcode
    m.setu8(0xC7);//block write opcode
    filterBytes(TagProtocol.GEN2, m,optByte, target,accessPassword, true);
    m.data[optByte] = (byte)(0x40|(m.data[optByte]));//option
    m.setu8(0x00);//Write Flags
    m.setu8(bank.rep);
    m.setu32(wordPtr);
    m.setu8(wordCount);
    m.setbytes(data,0,data.length);
    
   }

  private void msgAddGEN2BlockPermaLock(Message m, int timeout,byte readLock, Gen2.Bank memBank, int blockPtr, byte blockRange,byte[] mask, int accessPassword, TagFilter target)
  throws ReaderException
  {

    int optByte;
    m.setu8(MSG_OPCODE_ERASE_BLOCK_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(0x00);//chip type
    optByte = m.writeIndex++;
    m.setu8(0x01);
    filterBytes(TagProtocol.GEN2, m,optByte, target,accessPassword, true);
    m.data[optByte] = (byte)(0x40|(m.data[optByte]));//option
    m.setu8(0x00);//RFU
    m.setu8(readLock);
    m.setu8(memBank.rep);
    m.setu32(blockPtr);
    m.setu8(blockRange);
    if (readLock==0x01)
    {
      m.setbytes(mask);
    }
    
  }
  /**
   * Search for tags for a specified amount of time.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * tags. Valid range is 0-65535
   * @param selection the antenna or antennas to use for the search
   * @param filter a specification of the air protocol filtering to perform
   * @return the number of tags found
   * @see #cmdGetTagBuffer
   */
  public int cmdReadTagMultiple(int timeout, AntennaSelection selection, TagProtocol protocol, TagFilter filter)
    throws ReaderException
  {
    int optByte, options, antennasFlag;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    Message m = new Message();
    msgSetupReadTagMultiple(m, timeout, selection.value, filter,
                            protocol, TagMetadataFlag.ALL, 0);

    sendTimeout(timeout, m);
    return m.getu8at(8);
  }

  private int[] executeEmbeddedRead(int timeout, Message m)
    throws ReaderException
  {

    try 
    {
      sendTimeout(timeout, m);
    }
    catch (ReaderCodeException re) 
    {
      if (re.getCode() != FAULT_NO_TAGS_FOUND)
        throw re;
      return new int[] {0, 0, 0};
    }

    int[] rv = new int[3];
    m.readIndex += 3; // Skip option and antenna selection
    rv[0] = m.getu8();  // tags found
    m.readIndex += 2; // Skip embedded command count and opcode
    rv[1] = m.getu16(); // tags successfully operated on
    rv[2] = m.getu16(); // tags unsuccessfully operated on
    return rv;
  }

  /**
   * Search for tags for a specified amount of time and kill each one.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * tags. Valid range is 0-65535
   * @param selection the antenna or antennas to use for the search
   * @param filter a specification of the air protocol filtering to perform
   * @param accessPassword the access password to use when killing the tag
   * @param killPassword the kill password to use when killing found tags
   * @return A three-element array: {the number of tags found, the
   * number of tags successfully killed, the number of tags
   * unsuccessfully killed}
   * @see cmdGetTagBuffer
   */
  public int[] cmdReadTagAndKillMultiple(int timeout, AntennaSelection selection,
                                         TagFilter filter, int accessPassword,
                                         int killPassword)
    throws ReaderException
  {
    Message m;
    int lenByte;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    m = new Message();
    msgSetupReadTagMultiple(m, timeout, selection.value | 0x04, filter,
                            TagProtocol.GEN2,TagMetadataFlag.ALL, accessPassword);
    m.setu8(1); // embedded command count
    lenByte = m.writeIndex++;
    msgAddGEN2KillTag(m, 0, killPassword);
    m.data[lenByte] = (byte)(m.writeIndex - (lenByte + 2));

    return executeEmbeddedRead(timeout, m);
  }

  /**
   * Search for tags for a specified amount of time and lock each one.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * tags. Valid range is 0-65535
   * @param selection the antenna or antennas to use for the search
   * @param accessPassword the password to use when locking the tag
   * @param filter a specification of the air protocol filtering to perform
   * @param mask the Gen2 lock mask
   * @param action the Gen2 lock action
   * @return A three-element array: {the number of tags found, the
   * number of tags successfully locked, the number of tags
   * unsuccessfully locked}
   * @see cmdGetTagBuffer
   */
  public int[] cmdReadTagAndLockMultiple(int timeout, AntennaSelection selection,
                                         TagFilter filter, int accessPassword,
                                         int mask, int action)
    throws ReaderException
  {
    Message m;
    int lenByte;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    m = new Message();
    msgSetupReadTagMultiple(m, timeout, selection.value | 0x04, filter,
                            TagProtocol.GEN2,TagMetadataFlag.ALL, accessPassword);
    m.setu8(1); // embedded command count
    lenByte = m.writeIndex++;
    msgAddGEN2LockTag(m, 0, mask, action, 0);
    m.data[lenByte] = (byte)(m.writeIndex - (lenByte + 2));

    return executeEmbeddedRead(timeout, m);
  }

  /**
   * Search for tags for a specified amount of time and write data to each one.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * tags. Valid range is 0-65535
   * @param selection the antenna or antennas to use for the search
   * @param filter a specification of the air protocol filtering to perform
   * @param accessPassword the password to use when writing the tag
   * @param bank the Gen2 memory bank to write to
   * @param address the word address to start writing at
   * @param data the data to write
   * @return A three-element array: {the number of tags found, the
   * number of tags successfully written to, the number of tags
   * unsuccessfully written to}.
   * @see cmdGetTagBuffer
   */
  public int[] cmdReadTagAndDataWriteMultiple(int timeout, 
                                              AntennaSelection selection,
                                              TagFilter filter,
                                              int accessPassword, 
                                              Gen2.Bank bank,
                                              int address, byte[] data)
    throws ReaderException
  {
    Message m;
    int lenByte;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    m = new Message();
    msgSetupReadTagMultiple(m, timeout, selection.value | 0x04, filter,
                            TagProtocol.GEN2,TagMetadataFlag.ALL, accessPassword);
    m.setu8(1); // embedded command count
    lenByte = m.writeIndex++;
    msgAddGEN2DataWrite(m, 0, bank, address);
    m.setbytes(data);
    m.data[lenByte] = (byte)(m.writeIndex - (lenByte + 2));

    return executeEmbeddedRead(timeout, m);
  }

  /**
   * Search for tags for a specified amount of time and read data from each one.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * tags. Valid range is 0-65535
   * @param selection the antenna or antennas to use for the search
   * @param filter a specification of the air protocol filtering to perform
   * @param accessPassword the password to use when writing the tag
   * @param bank the Gen2 memory bank to read from
   * @param address the word address to start reading from
   * @param length the number of words to read. Only two words per tag
   * will be stored in the tag buffer.
   * @return A three-element array, containing: {the number of tags
   * found, the number of tags successfully read from, the number
   * of tags unsuccessfully read from}.
   * @see cmdGetTagBuffer
   */
  public int[] cmdReadTagAndDataReadMultiple(int timeout,
                                             AntennaSelection selection,
                                             TagFilter filter,
                                             int accessPassword, 
                                             Gen2.Bank bank,
                                             int address, int length)
    throws ReaderException
  {
    Message m;
    int lenByte;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    m = new Message();
    msgSetupReadTagMultiple(m, timeout, selection.value | 0x04, filter,
                            TagProtocol.GEN2,TagMetadataFlag.ALL, accessPassword);
    m.setu8(1); // embedded command count
    lenByte = m.writeIndex++;
    msgAddGEN2DataRead(m, 0, 0, bank, address, length);
    m.data[lenByte] = (byte)(m.writeIndex - (lenByte + 2));

    return executeEmbeddedRead(timeout, m);
  }

  /**
   * Write the EPC of a tag and update the PC bits. Behavior is
   * unspecified if more than one tag can be found.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for a tag
   * to write. Valid range is 0-65535
   * @param epc the EPC to write to the tag
   * @param lock whether to lock the tag (does not apply to all protocols)
   */
  public void cmdWriteTagEpc(int timeout, byte[] epc, boolean lock)
    throws ReaderException
  {

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_WRITE_TAG_ID);
    m.setu16(timeout);
    m.setu16(0); // RFU fields
    m.setbytes(epc);

    sendTimeout(timeout, m);
  }

  void checkMemParams(int address, int count)
  {
    if (count < 0 || count > 127)
    {
      throw new IllegalArgumentException("Invalid word count " + count
                                         + " (out of range)");
    }
  }

  /**
   * Write data to a Gen2 tag.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * a tag to write to. Valid range is 0-65535
   * @param bank the Gen2 memory bank to write to
   * @param address the word address to start writing at
   * @param data the data to write - must be an even number of bytes
   * @param accessPassword the password to use when writing the tag
   * @param filter a specification of the air protocol filtering to
   * perform to find the tag
   */
  public void cmdGen2WriteTagData(int timeout, 
                                  Gen2.Bank bank, int address, byte[] data,
                                  int accessPassword, TagFilter filter)
    throws ReaderException
  {
    Message m;
    int optByte;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }
    checkMemParams(address, data.length/2);

    m = new Message();
    msgAddGEN2DataWrite(m, timeout, bank, address);
    filterBytesGen2(m, 5, filter, accessPassword, true);
    m.setbytes(data);

    sendTimeout(timeout, m);
  }

  /**
   * Write tag to an ISO180006B tag
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * a tag to write to. Valid range is 0-65535
   * @param address the address to start writing at
   * @param data the data to write
   * @param filter a specification of the air protocol filtering to
   * 
   */
  public void cmdIso180006bWriteTagData(int timeout, int address, byte[] data,
                                        TagFilter filter)

    throws ReaderException
  {
    Message m;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    if (address < 0 || address > 255)
    {
      throw new IllegalArgumentException("illegal address " + address);
    }

    m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_DATA);
    m.setu16(timeout);
    if (filter != null
        && (filter instanceof TagData)
        && ((TagData)filter).epc.length == 8)
    {
      m.setu8(ISO180006B_WRITE_OPTION_READ_VERIFY_AFTER
              | ISO180006B_WRITE_OPTION_COUNT_PROVIDED);
      m.setu8(ISO180006B_COMMAND_WRITE4BYTE);
      m.setu8(ISO180006B_WRITE_LOCK_NO);
      m.setu8(address);
      m.setbytes(((TagData)filter).epc);
    }
    else
    {
      m.setu8(ISO180006B_WRITE_OPTION_GROUP_SELECT
              | ISO180006B_WRITE_OPTION_COUNT_PROVIDED);
      m.setu8(ISO180006B_COMMAND_WRITE4BYTE_MULTIPLE);
      m.setu8(ISO180006B_WRITE_LOCK_NO);
      m.setu8(address);
      filterBytesIso180006b(m, -1, filter);
    }
    m.setu16(data.length);
    m.setbytes(data);

    sendTimeout(timeout, m);
  }

  /**
   * Lock a Gen2 tag
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * a tag to lock. Valid range is 0-65535
   * @param mask the Gen2 lock mask
   * @param action the Gen2 lock action
   * @param accessPassword the password to use when locking the tag
   * @param filter a specification of the air protocol filtering to perform
   */
  public void cmdGen2LockTag(int timeout, int mask, int action,
                             int accessPassword, TagFilter filter)
    throws ReaderException
  {
    Message m;
    int optByte;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    m = new Message();
    msgAddGEN2LockTag(m, timeout, mask, action, accessPassword);
    filterBytesGen2(m, 5, filter, 0, false);

    sendTimeout(timeout, m);
  }

  /**
   * Lock an ISO180006B tag
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * a tag to lock. Valid range is 0-65535
   * @param address the part of the tag to lock. Valid range is 0-255
   * @param filter a specification of the air protocol filtering to perform
   */ 
  public void cmdIso180006bLockTag(int timeout, int address, TagFilter filter)
    throws ReaderException
  {
    Message m;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    if (address < 0 || address > 255)
    {
      throw new IllegalArgumentException("illegal address " + address);
    }

    if (filter == null
        || !(filter instanceof TagData)
        || ((TagData)filter).epc.length != 8)
    {
      throw new IllegalArgumentException("illegal filter " + filter);
    }

    m = new Message();
    m.setu8(MSG_OPCODE_LOCK_TAG);
    m.setu16(timeout);
    m.setu8(ISO180006B_LOCK_OPTION_TYPE_FOLLOWS);
    m.setu8(ISO180006B_LOCK_TYPE_QUERYLOCK_THEN_LOCK);
    m.setu8(address);
    m.setbytes(((TagData)filter).epc);

    sendTimeout(timeout, m);
  }

  /**
   * Kill a Gen2 tag.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * a tag to kill. Valid range is 0-65535
   * @param killPassword the tag kill password
   * @param filter a specification of the air protocol filtering to perform
   */
  public void cmdKillTag(int timeout, int killPassword, TagFilter filter)
    throws ReaderException
  {
    Message m;
    int optByte;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }

    m = new Message();
    msgAddGEN2KillTag(m, timeout, killPassword);
    filterBytesGen2(m, 5, filter, 0, false);

    sendTimeout(timeout, m);
  }

  /**
   * Read the memory of a Gen2 tag.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for the operation. Valid range is 0-65535
   * @param metadataFlags the set of metadata values to retreive and store in the returned object
   * @param bank the Gen2 memory bank to read from
   * @param address the word address to start reading from
   * @param length the number of words to read.
   * @param accessPassword the password to use when writing the tag
   * @param filter a specification of the air protocol filtering to perform
   * @return a TagReadData object containing the tag data and any
   * requested metadata (note: the tag EPC will not be present in the
   * object)
   */ 
  public TagReadData cmdGen2ReadTagData(int timeout,
                                        Set<TagMetadataFlag> metadataFlags,
                                        Gen2.Bank bank, int address, 
                                        int length, int accessPassword,
                                        TagFilter filter)
    throws ReaderException
  {
    Message m;
    int metadataBits;
    TagReadData tr;
    long startTime;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }
    checkMemParams(address, length);

    metadataBits = tagMetadataSetValue(metadataFlags);

    m = new Message();
    msgAddGEN2DataRead(m, timeout, metadataBits, bank, address, length);
    filterBytesGen2(m, 5, filter, accessPassword, true);

    if (metadataBits != 0)
    {
      m.data[5] |= 0x10; 
    }
    
    startTime = System.currentTimeMillis();
    sendTimeout(timeout, m);

    tr = new TagReadData();
    tr.readBase = startTime;
    m.readIndex++;
    if (metadataBits != 0)
    {
      m.readIndex += 2;
      metadataFromMessage(tr, m, metadataFlags);
    }

    tr.data = new byte[m.writeIndex - m.readIndex];
    m.getbytes(tr.data, tr.data.length);
    tr.tag = new Gen2.TagData("");

    return tr;
  }

  /**
   * Read the memory of an ISO180006B tag.
   *
   * @deprecated
   * @param timeout the duration in milliseconds to search for
   * a tag to read. Valid range is 0-65535
   * @param address the address to start reading from
   * @param length the number of bytes to read
   * @param filter a specification of the air protocol filtering to perform
   * @return the data read. Length is dependent on protocol.
   */ 
  public byte[] cmdIso180006bReadTagData(int timeout, int address, int length,
                               TagFilter filter)
    throws ReaderException
  {
    Message m;

    if (timeout < 0 || timeout > 65535)
    {
      throw new IllegalArgumentException("illegal timeout " + timeout);
    }
    if (address < 0 || address > 255)
    {
      throw new IllegalArgumentException("illegal address " + address);
    }
    if (length < 0 || length > 8)
    {
      throw new IllegalArgumentException("illegal length " + length);
    }

    if (filter == null
        || !(filter instanceof TagData)
        || ((TagData)filter).epc.length != 8)
    {
      throw new IllegalArgumentException("illegal filter " + filter);
    }

    m = new Message();
    m.setu8(MSG_OPCODE_READ_TAG_DATA);
    m.setu16(timeout);
    m.setu8(1); // Standard read operation
    m.setu8(ISO180006B_COMMAND_READ);
    m.setu8(0); // RFU
    m.setu8(length);
    m.setu8(address);
    m.setbytes(((TagData)filter).epc);

    sendTimeout(timeout, m);

    byte[] result = new byte[length];
    m.getbytes(result, length);

    return result;
  }


  /**
   * Get the number of tags stored in the tag buffer
   *
   * @deprecated
   * @return a three-element array containing: {the number of tags
   * remaining, the current read index of the tag buffer, the
   * current write index of the tag buffer}.
   */
  public int[] cmdGetTagsRemaining()
    throws ReaderException
  {
    int writeIndex, readIndex;

    Message m = sendOpcode(MSG_OPCODE_GET_TAG_BUFFER);

    readIndex = m.getu16();
    writeIndex = m.getu16();
    return new int[] {writeIndex - readIndex, readIndex, writeIndex};
  }

  /**
   * Get tag data of a number of tags from the tag buffer. This
   * command moves a read index into the tag buffer, so that repeated
   * calls will fetch all of the tags in the buffer.
   *
   * @deprecated
   * @param count the maximum of tags to get from the buffer. No more
   * than 65535 may be requested. It is an error to request more tags
   * than exist.
   * @param epc496 Whether the EPCs expected are 496 bits (true) or 96 bits (false)
   * @return the tag data. Fewer tags may be returned than were requested.
   */ 
  public TagData[] cmdGetTagBuffer(int count, boolean epc496,
                                   TagProtocol protocol)
    throws ReaderException
  {
    Message m;

    if (count < 0 || count > 65535)
    {
      throw new IllegalArgumentException("illegal count " + count);
    }

    m = new Message();
    m.setu8(MSG_OPCODE_GET_TAG_BUFFER);
    m.setu16(count);

    send(m);

    return parseTagBuffer(m, epc496, protocol);
  }
  
  /**
   * Get tag data of a tags from certain locations in the tag buffer,
   * without updating the read index.
   *
   * @deprecated
   * @param start the start index to read from
   * @param end the end index to read to
   * @param epc496 Whether the EPCs expected are 496 bits (true) or 96 bits (false)
   * @return the tag data. Fewer tags may be returned than were requested. 
   */ 
  public TagData[] cmdGetTagBuffer(int start, int end, boolean epc496,
                                   TagProtocol protocol)
    throws ReaderException
  {
    Message m;

    if (start < 0 || start > 65535)
    {
      throw new IllegalArgumentException("illegal start index " + start);
    }
    if (end < 0 || end > 65535)
    {
      throw new IllegalArgumentException("illegal end index " + end);
    }

    m = new Message();
    m.setu8(MSG_OPCODE_GET_TAG_BUFFER);
    m.setu16(start);
    m.setu16(end);

    send(m);

    return parseTagBuffer(m, epc496, protocol);
  }

  TagData[] parseTagBuffer(Message m, boolean epc496,
                           TagProtocol protocol)
  {
    TagData[] tags;
    int epcLen, recordLen, numTags;
    int i, j, off;

    recordLen = epc496 ? 68 : 18;
    numTags = (m.writeIndex - m.readIndex) / recordLen;
    tags = new TagData[numTags];
    for (i = 0, off = 0; i < numTags; i++, off += recordLen)
    {
      epcLen = m.getu16();
      tags[i] = parseTag(m, epcLen, protocol);
    }
    return tags;
  }

  TagData parseTag(Message m, int epcLen, TagProtocol protocol)
  {
    TagData tag;
    byte[] epcbits, crcbits;

    switch (protocol)
    {
    case GEN2:
      byte[] pcbits = new byte[2];
      m.getbytes(pcbits, 2);
      epcbits = new byte[epcLen - 4];
      m.getbytes(epcbits, epcLen - 4);
      crcbits = new byte[2];
      m.getbytes(crcbits, 2);
      tag = new Gen2.TagData(epcbits, crcbits, pcbits);
      break;
    case IPX256:
      epcbits = new byte[epcLen - 2];
      m.getbytes(epcbits, epcLen - 2);
      crcbits = new byte[2];
      m.getbytes(crcbits, 2);
      tag = new Ipx256.TagData(epcbits, crcbits);
      break;
    case ISO180006B:
      epcbits = new byte[epcLen - 2];
      m.getbytes(epcbits, epcLen - 2);
      crcbits = new byte[2];
      m.getbytes(crcbits, 2);
      tag = new Iso180006b.TagData(epcbits, crcbits);
      break;
    case IPX64:
      epcbits = new byte[epcLen - 2];
      m.getbytes(epcbits, epcLen - 2);
      crcbits = new byte[2];
      m.getbytes(crcbits, 2);
      tag = new Ipx64.TagData(epcbits, crcbits);
      break;
    default:
      epcbits = new byte[epcLen - 2];
      m.getbytes(epcbits, epcLen - 2);
      crcbits = new byte[2];
      m.getbytes(crcbits, 2);
      tag = new TagData(epcbits, crcbits);
    }
    return tag;
  }

  /**
   * Get tag data and associated read metadata from the tag buffer.
   *
   * @deprecated
   * @param metadataFlags the set of metadata values to retrieve and store in the returned objects
   * @param resend whether to resend the same tag data sent in a previous call
   * @return an array of TagReadData objects containing the tag and requested metadata
   */
  public TagReadData[] cmdGetTagBuffer(Set<TagMetadataFlag> metadataFlags,
                                       boolean resend, TagProtocol protocol)
    throws ReaderException
  {

    return cmdGetTagBufferInternal(tagMetadataSetValue(metadataFlags),
                                   resend, protocol);
  }


  TagReadData[] cmdGetTagBufferInternal(int metadataBits,
                                        boolean resend, TagProtocol protocol)
    throws ReaderException
  {
    TagReadData[] trs;
    byte[] response;
    int offset, numTagsInMessage;

    Message m = new Message();
    m.setu8(MSG_OPCODE_GET_TAG_BUFFER);
    m.setu16(metadataBits);
    m.setu8(resend ? 1 : 0);

    send(m);

    // the module might not support all the bits we asked for
    Set<TagMetadataFlag> metadataFlags = tagMetadataSet(m.getu16());
    m.readIndex++; // we don't need the read options
    numTagsInMessage = m.getu8();
    trs = new TagReadData[numTagsInMessage];
    for (int i = 0 ; i < numTagsInMessage; i++)
    {
      trs[i] = new TagReadData();

      metadataFromMessage(trs[i], m, metadataFlags);
      trs[i].tag = parseTag(m, m.getu16() / 8,
              protocol==TagProtocol.NONE?trs[i].readProtocol:protocol);
    }

    return trs;
  }


  /**
   * Clear the tag buffer.
   *
   * @deprecated
   */
  public void cmdClearTagBuffer()
    throws ReaderException
  {
    sendOpcode(MSG_OPCODE_CLEAR_TAG_ID_BUFFER);
  }

  /**
   * Send the Alien Higgs2 Partial Load Image command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to write on the tag
   * @param killPassword the kill password to write on the tag
   * @param EPC the EPC to write to the tag. Maximum of 12 bytes (96 bits)
   */
  public void cmdHiggs2PartialLoadImage(int timeout, int accessPassword, 
                              int killPassword, byte[] EPC)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_ALIEN_HIGGS);
    m.setu8(ALIEN_HIGGS_CHIP_SUBCOMMAND_PARTIAL_LOAD_IMAGE);
    m.setu32(killPassword);
    m.setu32(accessPassword);
    m.setbytes(EPC);

    sendTimeout(timeout, m);
  }

  /**
   * Send the Alien Higgs2 Full Load Image command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to write on the tag
   * @param killPassword the kill password to write on the tag
   * @param lockBits the lock bits to write on the tag
   * @param pcWord the PC word to write on the tag
   * @param EPC the EPC to write to the tag. Maximum of 12 bytes (96 bits)
   */
  public void cmdHiggs2FullLoadImage(int timeout, int accessPassword, 
                              int killPassword, int lockBits, 
                              int pcWord, byte[] EPC)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_ALIEN_HIGGS);
    m.setu8(ALIEN_HIGGS_CHIP_SUBCOMMAND_FULL_LOAD_IMAGE);
    m.setu32(killPassword);
    m.setu32(accessPassword);
    m.setu16(lockBits);
    m.setu16(pcWord);
    m.setbytes(EPC);

    sendTimeout(timeout, m);
  }

  /**
   * Send the Alien Higgs3 Fast Load Image command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param currentAccessPassword the access password to use to write to the tag
   * @param accessPassword the access password to write on the tag
   * @param killPassword the kill password to write on the tag
   * @param pcWord the PC word to write on the tag
   * @param EPC the EPC to write to the tag. Must be exactly 12 bytes (96 bits)
   */
  public void cmdHiggs3FastLoadImage(int timeout, int currentAccessPassword,
                                     int accessPassword, int killPassword,
                                     int pcWord, byte[] EPC)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_ALIEN_HIGGS3);
    m.setu8(ALIEN_HIGGS3_CHIP_SUBCOMMAND_FAST_LOAD_IMAGE);
    m.setu32(currentAccessPassword);
    m.setu32(killPassword);
    m.setu32(accessPassword);
    m.setu16(pcWord);
    m.setbytes(EPC);
    sendTimeout(timeout, m);
  }

  /**
   * Send the Alien Higgs3 Load Image command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param currentAccessPassword the access password to use to write to the tag
   * @param accessPassword the access password to write on the tag
   * @param killPassword the kill password to write on the tag
   * @param pcWord the PC word to write on the tag
   * @param EPCAndUserData the EPC and user data to write to the
   * tag. Must be exactly 76 bytes. The pcWord specifies which of this
   * is EPC and which is user data.
   */
  public void cmdHiggs3LoadImage(int timeout, int currentAccessPassword,
                                 int accessPassword, int killPassword,
                                 int pcWord, byte[] EPCAndUserData)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_ALIEN_HIGGS3);
    m.setu8(ALIEN_HIGGS3_CHIP_SUBCOMMAND_LOAD_IMAGE);
    m.setu32(currentAccessPassword);
    m.setu32(killPassword);
    m.setu32(accessPassword);
    m.setu16(pcWord);
    m.setbytes(EPCAndUserData);
    sendTimeout(timeout, m);
  }

  /**
   * Send the Alien Higgs3 Block Read Lock command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @param lockBits a bitmask of bits to lock. Valid range 0-255
   */
  public void cmdHiggs3BlockReadLock(int timeout, int accessPassword,
                                     int lockBits)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_ALIEN_HIGGS3);
    m.setu8(ALIEN_HIGGS3_CHIP_SUBCOMMAND_BLOCK_READ_LOCK);
    m.setu32(accessPassword);
    m.setu8(lockBits);

    sendTimeout(timeout, m);
  }

  /**
   * Send the NXP Set Read Protect command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   */
  public void cmdNxpSetReadProtect(int timeout, int accessPassword)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_NXP);
    m.setu8(NXP_CHIP_SUBCOMMAND_SET_QUIET);
    m.setu32(accessPassword);
    sendTimeout(timeout, m);
  }

  /**
   * Send the NXP Reset Read Protect command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   */
  public void cmdNxpResetReadProtect(int timeout, int accessPassword)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_NXP);
    m.setu8(NXP_CHIP_SUBCOMMAND_RESET_QUIET);
    m.setu32(accessPassword);
    sendTimeout(timeout, m);
  }

  /**
   * Send the NXP Change EAS command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @param reset true to reset the EAS, false to set it
   */
  public void cmdNxpChangeEas(int timeout, int accessPassword, boolean reset)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_NXP);
    m.setu8(NXP_CHIP_SUBCOMMAND_CHANGE_EAS);
    m.setu32(accessPassword);
    m.setu8(reset ? 2 : 1);
    sendTimeout(timeout, m);
  }
  
  /**
   * Send the NXP EAS Alarm command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param dr Gen2 divide ratio to use
   * @param m Gen2 M parameter to use
   * @param trExt Gen2 TrExt value to use
   * @return 8 bytes of EAS alarm data
   */
  public byte[] cmdNxpEasAlarm(int timeout, Gen2.DivideRatio dr,
                               Gen2.TagEncoding millerM, Gen2.TrExt trExt)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_NXP);
    m.setu8(NXP_CHIP_SUBCOMMAND_EAS_ALARM);
    m.setu8(dr.rep);
    m.setu8(millerM.rep);
    m.setu8(trExt.rep);
    sendTimeout(timeout, m);

    m.readIndex += 2;
    byte[] rv = new byte[8];
    m.getbytes(rv, 8);
    return rv;
  }

  /**
   * Send the NXP Calibrate command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @return 64 bytes of calibration data
   */
  public byte[] cmdNxpCalibrate(int timeout, int accessPassword)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_NXP);
    m.setu8(NXP_CHIP_SUBCOMMAND_CALIBRATE);
    m.setu32(accessPassword);
    sendTimeout(timeout, m);

    m.readIndex += 2;
    byte[] rv = new byte[64];
    m.getbytes(rv, 64);
    return rv;
  }

  /**
   * Send the Hitachi Hibiki Read Lock command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @param mask bitmask of read lock bits to alter
   * @param action action value of read lock bits to alter
   */
  public void cmdHibikiReadLock(int timeout, int accessPassword,
                                 int mask, int action)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_HITACHI_HIBIKI);
    m.setu8(HITACHI_HIBIKI_CHIP_SUBCOMMAND_READ_LOCK);
    m.setu32(accessPassword);
    m.setu8(mask);
    m.setu8(action);
    sendTimeout(timeout, m);
  }


  public static class HibikiSystemInformation
  {
    public int infoFlags;
    public byte reservedMemory;
    public byte epcMemory;
    public byte tidMemory;
    public byte userMemory;
    public byte setAttenuate;
    public int bankLock;
    public int blockReadLock;
    public int blockRwLock;
    public int blockWriteLock;
  }

  /**
   * Send the Hitachi Hibiki Get System Information command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @return 10-element array of integers: {info flags, reserved memory size,
   * EPC memory size, TID memory size, user memory size, set attenuate value,
   * bank lock bits, block read lock bits, block r/w lock bits, block write
   * lock bits}
   */
  public HibikiSystemInformation
  cmdHibikiGetSystemInformation(int timeout, int accessPassword)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_HITACHI_HIBIKI);
    m.setu8(HITACHI_HIBIKI_CHIP_SUBCOMMAND_GET_SYSTEM_INFORMATION);
    m.setu32(accessPassword);
    sendTimeout(timeout, m);

    HibikiSystemInformation rv = new HibikiSystemInformation();
    m.readIndex += 2;
    rv.infoFlags =      m.getu16();
    rv.reservedMemory = (byte)m.getu8();
    rv.epcMemory =      (byte)m.getu8();
    rv.tidMemory =      (byte)m.getu8();
    rv.userMemory =     (byte)m.getu8();
    rv.setAttenuate =   (byte)m.getu8();
    rv.bankLock =       m.getu16();
    rv.blockReadLock =  m.getu16();
    rv.blockRwLock =    m.getu16();
    rv.blockWriteLock = m.getu16();

    return rv;
  }

  /**
   * Send the Hitachi Hibiki Set Attenuate command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @param level the attenuation level to set
   * @param lock whether to permanently lock the attenuation level
   */
   
  public void cmdHibikiSetAttenuate(int timeout, int accessPassword, int level,
                                    boolean lock)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_HITACHI_HIBIKI);
    m.setu8(HITACHI_HIBIKI_CHIP_SUBCOMMAND_SET_ATTENUATE);
    m.setu32(accessPassword);
    m.setu8(level);
    m.setu8(lock ? 1 : 0);
    sendTimeout(timeout, m);
  }

  /**
   * Send the Hitachi Hibiki Block Lock command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @param block the block of memory to operate on
   * @param blockPassword the password for the block
   * @param mask bitmask of lock bits to alter
   * @param action value of lock bits to alter
   */
  public void cmdHibikiBlockLock(int timeout, int accessPassword, int block,
                                 int blockPassword, int mask, int action)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_HITACHI_HIBIKI);
    m.setu8(HITACHI_HIBIKI_CHIP_SUBCOMMAND_BLOCK_LOCK);
    m.setu32(accessPassword);
    m.setu8(block);
    m.setu32(blockPassword);
    m.setu8(mask);
    m.setu8(action);
    sendTimeout(timeout, m);
  }

  /**
   * Send the Hitachi Hibiki Block Read Lock command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @param block the block of memory to operate on
   * @param blockPassword the password for the block
   * @param mask bitmask of read lock bits to alter
   * @param action value of read lock bits to alter
   */
  public void cmdHibikiBlockReadLock(int timeout, int accessPassword, int block,
                                     int blockPassword, int mask, int action)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_HITACHI_HIBIKI);
    m.setu8(HITACHI_HIBIKI_CHIP_SUBCOMMAND_BLOCK_READ_LOCK);
    m.setu32(accessPassword);
    m.setu8(block);
    m.setu32(blockPassword);
    m.setu8(mask);
    m.setu8(action);
    sendTimeout(timeout, m);
  }


  /**
   * Send the Hitachi Hibiki Write Multiple Words Lock command.
   *
   * @deprecated
   * @param timeout the timeout of the operation, in milliseconds.
   * Valid range is 0-65535.
   * @param accessPassword the access password to use to write to the tag
   * @param bank the Gen2 memory bank to write to
   * @param wordOffset the word address to start writing at
   * @param data the data to write - must be an even number of bytes
   */
  public void cmdHibikiWriteMultipleWords(int timeout, int accessPassword,
                                          Gen2.Bank bank, int wordOffset,
                                          byte[] data)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(TAG_CHIP_TYPE_HITACHI_HIBIKI);
    m.setu8(HITACHI_HIBIKI_CHIP_SUBCOMMAND_WRITE_MULTIPLE_WORDS);
    m.setu32(accessPassword);
    m.setu8(bank.rep);
    m.setu32(wordOffset);
    m.setu8(data.length);
    m.setbytes(data);
    sendTimeout(timeout, m);
  }

  /**
   * Erase a range of words on a Gen2 tag that supports the 
   * optional Erase Block command.
   *
   * @deprecated
   * @param bank the Gen2 memory bank to erase words in
   * @param address the word address to start erasing at
   * @param count the number of words to erase
   */
  public void cmdEraseBlockTagSpecific(int timeout, Gen2.Bank bank,
                                       int address, int count)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_ERASE_BLOCK_TAG_SPECIFIC);
    m.setu16(timeout);
    m.setu8(0); // chip type
    m.setu8(0); // option
    m.setu32(address);
    m.setu8(bank.rep);
    m.setu8(count);
    sendTimeout(timeout, m);
  }


  /**
   * Get a block of hardware version information. This information is
   * an opaque data block.
   *
   * @deprecated
   * @param option opaque option argument
   * @param flags opaque flags argument
   * @return the version block
   */
  public byte[] cmdGetHardwareVersion(int option, int flags)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_GET_HW_REVISION);
    m.setu8(option);
    m.setu8(flags);

    send(m);
    byte[] data = new byte[m.writeIndex - m.readIndex];
    m.getbytes(data, data.length);
    return data;
  }
  public String cmdGetSerialNumber(int option, int flags)
    throws ReaderException
  {
    byte[] serialNumber_byte = cmdGetHardwareVersion(0x00, 0x40);
    char[] serialNumber_char = new char[serialNumber_byte[3]];
    for (int i = 0; i < serialNumber_char.length; i++)
    {
      serialNumber_char[i] = (char)serialNumber_byte[i + 4];
    }
    return new String(serialNumber_char);
  }
    

  /**
   * Get the currently set Tx and Rx antenna port.
   *
   * @deprecated
   * @return a two-element array: {tx port, rx port}
   */
  public int[] cmdGetTxRxPorts()
    throws ReaderException
  
  {
    Message m;
    int tx, rx;

    m = sendOpcode(MSG_OPCODE_GET_ANTENNA_PORT);
    tx = m.getu8();
    rx = m.getu8();
    
    return new int[] {tx, rx};
  }

  /**
   * Representation of the device's antenna state.
   */
  public static class AntennaPort
  {
    /**
     * The number of physical antenna ports.
     */
    public int numPorts;
    /**
     * The current logical transmit antenna port.
     */
    public int txAntenna;
    /**
     * The current logical receive antenna port.
     */
    public int rxAntenna;
    /**
     * A list of physical antenna ports where an antenna has been detected.
     */
    public int portTerminatedList[];
  }

  /**
   * Get the current Tx and Rx antenna port, the number of physical
   * ports, and a list of ports where an antenna has been detected.
   *
   * @deprecated
   * @return an object containing the antenna port information
   */ 
  public AntennaPort cmdGetAntennaConfiguration()
    throws ReaderException
  {
    Message m = new Message();
    AntennaPort ap = new AntennaPort();
    int numTerminated;
    
    m.setu8(MSG_OPCODE_GET_ANTENNA_PORT);
    m.setu8(1);
    send(m);
    ap.txAntenna = m.getu8();
    ap.rxAntenna = m.getu8();
    ap.numPorts  = (m.writeIndex - m.readIndex);

    numTerminated = 0;
    for (int i = 0; i < ap.numPorts; i++)
    {
      if (m.data[m.readIndex + i] == 1)
      {
        numTerminated++;
      }
    }
    
    ap.portTerminatedList = new int[numTerminated];
    for (int i = 0, j = 0; i < ap.numPorts; i++)
    {
      if (m.data[m.readIndex + i] == 1)
      {
        ap.portTerminatedList[j] = i + 1;
        j++;
      }
    }

    return ap;
  }

  /**
   * Gets the search list of logical antenna ports.
   *
   * @deprecated
   * @return an array of 2-element arrays of integers interpreted as
   * (tx port, rx port) pairs. Example, representing a monostatic
   * antenna on port 1 and a bistatic antenna pair on ports 3 and 4:
   * {{1, 1}, {3, 4}}
   */
  public int[][] cmdGetAntennaSearchList()
    throws ReaderException 
  {
    Message m = new Message();
    int count;
    int[][] response;

    m.setu8(MSG_OPCODE_GET_ANTENNA_PORT);
    m.setu8(2);

    send(m);
    m.readIndex++; // Skip option byte

    count = (m.writeIndex - m.readIndex) / 2;
    response = new int[count][];
    for (int i=0, j=1; i < count; i++, j+=2)
    {
      response[i] = new int[2];
      response[i][0] = m.getu8(); // TX port
      response[i][1] = m.getu8(); // RX port
    }
        
    return response;
  }

  /**
   * Gets the transmit powers of each antenna port.
   *
   * @deprecated
   * @return an array of 3-element arrays of integers interpreted as
   * (tx port, read power in centidbm, write power in centidbm)
   * triples. Example, with read power levels of 30 dBm and write
   * power levels of 25 dBm : {{1, 3000, 2500}, {2, 3000, 2500}}
   */
  public int[][] cmdGetAntennaPortPowers()
    throws ReaderException 
  {
    Message m = new Message();
    int[][] response;
    int count;

    m.setu8(MSG_OPCODE_GET_ANTENNA_PORT);
    m.setu8(3);

    send(m);
    m.readIndex++; // Skip option byte

    count = (m.writeIndex - m.readIndex) / 5;
    response = new int[count][];
    for (int i=0, j=1 ; i < count; i++, j+=5)
    {
      response[i] = new int[3];
      response[i][0] = m.getu8();  // Antenna number
      response[i][1] = m.getu16(); // Read power
      response[i][2] = m.getu16(); // Write power
    }

    return response;
  }


  /**
   * Gets the transmit powers and settling time of each antenna port.
   *
   * @deprecated
   * @return an array of 4-element arrays of integers interpreted as
   * (tx port, read power in centidbm, write power in centidbm,
   * settling time in microseconds) tuples.  An example with two
   * antenna ports, read power levels of 30 dBm, write power levels of
   * 25 dBm, and 500us settling times:
   * {{1, 3000, 2500, 500}, {2, 3000, 2500, 500}}
   */
  public int[][] cmdGetAntennaPortPowersAndSettlingTime()
    throws ReaderException 
  {
    Message m = new Message();
    int[][] response;
    int count;

    m.setu8(MSG_OPCODE_GET_ANTENNA_PORT);
    m.setu8(4);

    send(m);
    m.readIndex++; // Skip option byte

    count = (m.writeIndex - m.readIndex) / 7;
    response = new int[count][];
    for (int i=0, j=1; i < count; i++, j+=7)
    {
      response[i] = new int[4];
      response[i][0] = m.getu8();  // Antenna number
      response[i][1] = m.getu16(); // Read power
      response[i][2] = m.getu16(); // Write power
      response[i][3] = m.getu16(); // Settling time
    }

    return response;
  }

  /**
   * Enumerate the logical antenna ports and report the antenna
   * detection status of each one.
   *
   * @deprecated
   * @return an array of 2-element arrays of integers which are
   * (logical antenna port, detected) pairs. An example, where logical
   * ports 1 and 2 have detected antennas and 3 and 4 do not:
   * {{1, 1}, {2, 1}, {3, 0}, {4, 0}}
   */
  public int[][] cmdAntennaDetect()
    throws ReaderException
  {
    Message m = new Message();
    int[][] response;
    int count;

    m.setu8(MSG_OPCODE_GET_ANTENNA_PORT);
    m.setu8(5);

    send(m);
    m.readIndex++; // Skip option byte

    count = (m.writeIndex - m.readIndex)/2;
    response = new int[count][];
    for (int i=0, j=1; i < count ; i++, j+=2)
    {
      response[i] = new int[2];
      response[i][0] = m.getu8();
      response[i][1] = m.getu8();
    }

    return response;
  }

  int[] getConnectedAntennas()
    throws ReaderException
  {
    int[] connectedAntennas;
    int count;

    try
    {
      // Try the new antenna-detect command
      int[][] antennas;
      int index, numPorts;

      antennas = cmdAntennaDetect();
      numPorts = antennas.length;
      count = 0;
      for (int i = 0; i < numPorts; i++)
      {
        if (antennas[i][1] != 0)
        {
          count++;
        }
      }
      connectedAntennas = new int[count];
      index = 0;
      for (int i = 0; i < numPorts; i++)
      {
        if (antennas[i][1] != 0)
        {
          connectedAntennas[index++] = antennas[i][0];
        }
      }
    }
    catch (ReaderCodeException re)
    {
      AntennaPort ap = cmdGetAntennaConfiguration();
      connectedAntennas = ap.portTerminatedList;
    }
    return connectedAntennas;
  }

  /**
   * Get the current global Tx power setting for read operations.
   *
   * @deprecated
   * @return the power setting, in centidbm
   */
  public int cmdGetReadTxPower()
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_GET_TX_READ_POWER);
    m.setu8(0);
    
    send(m);
    m.readIndex++; // Skip option byte

    return m.getu16();
  }

  /**
   * Get the current global Tx power setting for read operations, and the
   * minimum and maximum power levels supported.
   *
   * @deprecated
   * @return a three-element array: {tx power setting in centidbm,
   * maximum power, minimum power}. Example: {2500, 3000, 500}
   */
  public int[] cmdGetReadTxPowerWithLimits()
    throws ReaderException
  {
    Message m = new Message();
    int[] limits = new int[3];

    m.setu8(MSG_OPCODE_GET_TX_READ_POWER);
    m.setu8(1);
    send(m);
    m.readIndex++; // Skip option byte

    limits[0] = m.getu16();
    limits[1] = m.getu16();
    limits[2] = m.getu16();

    return limits;
  }

  /**
   * Get the current global Tx power setting for write operations.
   *
   * @deprecated
   * @return the power setting, in centidbm
   */
  public int cmdGetWriteTxPower()
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_GET_TX_WRITE_POWER);
    m.setu8(0);
    send(m);
    m.readIndex++; // Skip option byte

    return m.getu16();
  }

  /**
   * Get the current RFID protocol the device is configured to use.
   *
   * @deprecated
   * @return the current protocol
   */
  public TagProtocol cmdGetProtocol()
    throws ReaderException
  {
    Message m;

    m = sendOpcode(MSG_OPCODE_GET_TAG_PROTOCOL);
    return codeToProtocolMap.get(m.getu16());
  }

  /**
   * Get the current global Tx power setting for write operations, and the
   * minimum and maximum power levels supported.
   *
   * @deprecated
   * @return a three-element array: {tx power setting in centidbm,
   * maximum power, minimum power}. Example: {2500, 3000, 500}
   */
  public int[] cmdGetWriteTxPowerWithLimits()
    throws ReaderException
  {
    Message m = new Message();
    int[] limits = new int[3];

    m.setu8(MSG_OPCODE_GET_TX_WRITE_POWER);
    m.setu8(1);
    send(m);
    m.readIndex++; // Skip option byte

    limits[0] = m.getu16();
    limits[1] = m.getu16();
    limits[2] = m.getu16();
    return limits;
  }

  /**
   * Gets the frequencies in the current hop table
   *
   * @deprecated
   * @return an array of the frequencies in the hop table, in kHz
   */
  public int[] cmdGetFrequencyHopTable()
    throws ReaderException
  {
    Message m;
    int[] table;
    int i, off, tableLen;

    m = sendOpcode(MSG_OPCODE_GET_FREQ_HOP_TABLE);

    tableLen = (m.writeIndex - m.readIndex) / 4;
    table = new int[tableLen];
    for (i = 0, off = 0; i < tableLen; i++, off += 4)
    {
      table[i] = m.getu32();
    }
    return table;
  }

  /**
   * Gets the interval between frequency hops.
   *
   * @deprecated
   * @return the hop interval, in milliseconds
   */
  public int cmdGetFrequencyHopTime()
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_GET_FREQ_HOP_TABLE);
    m.setu8(1);
    send(m);

    return m.getu32();
  }

  /**
   * Gets the state of the device's GPIO input pins.
   *
   * @deprecated
   * @return an array of booleans representing the state of each pin
   * (index 0 corresponding to pin 1)
   */
  public boolean[] cmdGetGPIO()
    throws ReaderException
  {
    Message m;
    

    m = sendOpcode(MSG_OPCODE_GET_USER_GPIO_INPUTS);
    boolean[] gpi = new boolean[m.writeIndex-m.readIndex];
    for(int numGpio=0;numGpio<gpi.length;numGpio++)
    {
    gpi[numGpio] = (m.getu8() == 1);
    }
    return gpi;
  }

  /**
   * Gets the current region the device is configured to use.
   *
   * @deprecated
   * @return the region
   */
  public Reader.Region cmdGetRegion()
    throws ReaderException
  {
    Message m;

    m = sendOpcode(MSG_OPCODE_GET_REGION);
    return codeToRegionMap.get(m.getu8());
  }

  /**
   * Region-specific parameters that are supported on the device.
   */ 
  public enum RegionConfiguration
  {

    /**
     * Whether LBT is enabled.
     * <p>
     * Type: Boolean
     */ 
    LBTENABLED (0x40);

    int value;
    RegionConfiguration(int v)
    {
      value = v;
    }
    /**
     * internal use method
     * @return value
     */
    public int getValue()
    {
      return value;
    }
  }

  /**
   * Get the value of a region-specific configuration setting.
   *
   * @deprecated
   * @param key the setting
   * @return an object with the setting value. The type of the object
   * is dependant on the key; see the RegionConfiguration class for details;
   */
  public Object cmdGetRegionConfiguration(RegionConfiguration key)
    throws ReaderException
  {
    Message m = new Message();
    Object ret = null;

    m.setu8(MSG_OPCODE_GET_REGION);
    m.setu8(1);
    m.setu8(key.getValue());
    send(m);
    m.readIndex += 3; // Skip option, region, and key

    switch (key)
    {
    case LBTENABLED:
      ret = (Boolean) (m.getu8() == 1);
    }

    return ret;
  }

  /**
   * The device power mode, for use in the /reader/powerMode
   * parameter, {@link #cmdGetPowerMode}, and {@link #cmdSetPowerMode}.
   */
  public enum PowerMode
  {
      INVALID (-1),
      FULL (0),
      MINSAVE (1),
      MEDSAVE (2),
      MAXSAVE (3);

    int value;
    PowerMode(int v)
    {
      value = v;
    }

    static PowerMode getPowerMode(int p)
    {
      switch (p)
      {
      case -1: return INVALID;
      case 0: return FULL;
      case 1: return MINSAVE;
      case 2: return MEDSAVE;
      case 3: return MAXSAVE;
      default: return null;
      }
    }
  }

  /**
   * Gets the current power mode of the device.
   *
   * @deprecated
   * @return the power mode
   */
  public PowerMode cmdGetPowerMode()
    throws ReaderException
  {

    Message m = sendOpcode(MSG_OPCODE_GET_POWER_MODE);
    int mode = m.getu8();
    PowerMode p = PowerMode.getPowerMode(mode);
    if (p == null)
    {
      throw new ReaderParseException("Unknown power mode " + mode);
    }
    return p;
  }


  /**
   * The device user mode, for use in the /reader/userMode
   * parameter, {@link #cmdGetUserMode}, and {@link #cmdSetUserMode}.
   */
  public enum UserMode
  {
      NONE (0),
      PRINTER (1),
      PORTAL (3);
    int value;
    UserMode(int v)
    {
      value = v;
    }

    static UserMode getUserMode(int p)
    {
      switch (p)
      {
      case 0: return NONE;
      case 1: return PRINTER;
      case 3: return PORTAL;
      default: return null;
      }
    }
  }

  /**
   * Gets the current user mode of the device.
   *
   * @deprecated
   * @return the user mode
   */
  public UserMode cmdGetUserMode()
    throws ReaderException
  {

    Message m = sendOpcode(MSG_OPCODE_GET_USER_MODE);
    int mode = m.getu8();
    UserMode u = UserMode.getUserMode(mode);
    if (u == null)
    {
      throw new ReaderParseException("Unknown user mode " + mode);
    }
    return u;
  }
  
  /**
   * The device configuration keys for use in
   * {@link #cmdGetReaderConfiguration} and {@link #cmdSetReaderConfiguration}.
   * 
   */
  public enum Configuration
  {
    /**
     * Whether reads of the same tag EPC on distinct antennas are considered distinct tag reads.
     * <p>
     * Type: Boolean
     */
    UNIQUE_BY_ANTENNA (0),
      /**
       * Whether the reader operates in power-saving RF mode.
       * <p>
       * Type: Boolean
       */
      TRANSMIT_POWER_SAVE (1),
      /**
       * Whether the reader permits EPCs longer than 96 bits
       * <p>
       * Type: Boolean
       */
      EXTENDED_EPC (2),
      /**
       * A bitmask of the GPO pins that are used for antenna port
       * switching.
       * <p>
       * Type: Integer
       */
      ANTENNA_CONTROL_GPIO (3),
      /**
       * Whether to check for a connected antenna on each port before
       * transmitting.
       * <p>
       * Type: Boolean
       */
      SAFETY_ANTENNA_CHECK (4),
      /**
       * Whether to check for an over-temperature condition before
       * transmitting.
       * <p>
       * Type: Boolean
       */
      SAFETY_TEMPERATURE_CHECK (5),
      /**
       * In a set of reads of the same tag, whether to record the
       * metadata of the tag read with the highest RSSI value (as
       * opposed to the most recent).
       * <p> 
       * Type: Boolean
       */
      RECORD_HIGHEST_RSSI (6),
      /**
       * Whether reads of the same tag EPC with distinct tag memory
       * (in a cmdReadTagAndReadMultiple() operation) are considered
       * distinct.
       * <p>
       * Type: Boolean
       */
      UNIQUE_BY_DATA (8),
      /**
       * Whether RSSI values are reported in dBm, as opposed to
       * arbitrary uncalibrated units.
       * <p>
       * Type: Boolean
       */
      RSSI_IN_DBM (9);

    int value;
    Configuration(int v)
    {
      value = v;
    }
  }

  /**
   * Gets the value of a device configuration setting.
   *
   * @deprecated
   * @param key the setting
   * @return an object with the setting value. The type of the object
   * is dependant on the key; see the Configuration class for details.
   */
  public Object cmdGetReaderConfiguration(SerialReader.Configuration key)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_GET_READER_OPTIONAL_PARAMS);
    m.setu8(1);
    m.setu8(key.value);
    send(m);
    m.readIndex += 2;

    if (key == Configuration.ANTENNA_CONTROL_GPIO)
    {
      return (int)m.getu8();
    }
    else if (key== Configuration.UNIQUE_BY_ANTENNA || key == Configuration.UNIQUE_BY_DATA)
    {
      if (m.getu8() == 0)
      {
        return false;
      }
      else
      {
        return true;
      }
    }
    else
    {
      if (m.getu8() == 0)
      {
        return false;
      }
      else
      {
        return true;
      }
    }
  }


  /**
   * Interface for per-protocol parameter enumerations to implement.
   */
  public interface ProtocolConfiguration 
  {

    /**
     * Internal-use method.
     */
    int getValue();
  }

  /**
   * Gen2-specific parameters that are supported on the device.
   */ 
  public enum Gen2Configuration implements ProtocolConfiguration
  {
    /**
     * The Gen2 session value.
     * <p>
     * Type: Gen2.Session
     */
    SESSION (0),
    /**
     * The Gen2 target algorithm used for inventory operations.
     * <p>
     * Type: Gen2.Target
     */
      TARGET (1),
      /**
       * The Gen2 M value.
       * <p>
       * Type: Gen2.MillerM
       */
      TAGENCODING (2),
      /**
       * The Gen2 link frequency.
       * <p>
       * Type: Gen2.LinkFrequency
       */
      LINKFREQUENCY (0x10),

      /**
       * The Gen2 Tari value.
       * Type: Gen2.Tari
       */
      TARI (0x11),

      /**
       * The Gen2 Q algorithm used for inventory operations.
       * <p>
       * Type: Gen2.Q
       */
      Q (0x12);

    int value;
    Gen2Configuration(int v)
    {
      value = v;
    }
    /**
     * internal use method
     * @return value
     */
    public int getValue()
    {
      return value;
    }
  }

   /**
   * Operation Options for cmdSetUserProfile 
   */ 
  public enum SetUserProfileOption 
  {
    /**  Save operation */
     SAVE (0x01), 
    /**  Restore operation */
     RESTORE (0x02) ,
    /**  Verify operation */
     VERIFY (0x03),  
    /** Clear operation  */
     CLEAR (0x04);  
     int value;
    SetUserProfileOption(int v)
    {
      value = v;
    }
    /**
     * internal use method
     * @return value
     */
    public int getValue()
    {
      return value;
    }
  }
  
  /**
   * Congfig key for cmdSetUserProfile 
   */ 
  public enum ConfigKey 
  {
    /** All Configuration */
    ALL (0x01);  
     int value;
    ConfigKey(int v)
    {
      value = v;
    }
    /**
     * internal use method
     * @return value
     */
    public int getValue()
    {
      return value;
    }
  }
  
  /**
   * The config values for cmdSetUserProfile
   */ 
  public enum ConfigValue 
  {
   
    /** Firmware default configurations */
    FIRMWARE_DEFAULT(0x00),
    /** Custom configurations */
    CUSTOM_CONFIGURATION(0x01);  
     int value;
    ConfigValue(int v)
    {
      value = v;
    }
    /**
     * internal use method
     * @return value
     */
    public int getValue()
    {
      return value;
    }
  }
  /**
   * ISO18000-6B-specific parameters that are supported on the device.
   */ 
  public enum ISO180006BConfiguration implements ProtocolConfiguration
  {
    /**
     * The frequency of the tag data, in kHz.
     * <p>
     * Type: Integer
     */
    LINKFREQUENCY (0x10);

    int value;
    ISO180006BConfiguration(int v)
    {
      value = v;
    }
    /**
     * internal use method
     * @return value
     */
    public int getValue()
    {
      return value;
    }
  }
                                      
  /**
   * Gets the value of a protocol configuration setting.
   *
   * @deprecated
   * @param protocol the protocol of the setting
   * @param key the setting
   * @return an object with the setting value. The type of the object
   * is dependant on the key; see the ProtocolConfiguration class for details.
   */
  public Object cmdGetProtocolConfiguration(TagProtocol protocol,
                                     ProtocolConfiguration key)
    throws ReaderException
  {
    Message m = new Message();
    int[] retval;

    m.setu8(MSG_OPCODE_GET_PROTOCOL_PARAM);
    m.setu8(protocolToCodeMap.get(protocol));
    m.setu8(key.getValue());
    send(m);
    m.readIndex += 2; // Skip protocol and key

    if (protocol == TagProtocol.GEN2)
    {
      switch ((Gen2Configuration)key)
      {
      case SESSION:
        switch (m.getu8())
        {
        case 0:
          return Gen2.Session.S0;
        case 1:
          return Gen2.Session.S1;
        case 2:
          return Gen2.Session.S2;
        case 3:
          return Gen2.Session.S3;
        }
        break;
      case TARGET:
        switch(m.getu16())
        {
        case 0x0000:
          return Gen2.Target.AB;
        case 0x0001:
          return Gen2.Target.BA;
        case 0x0100:
          return Gen2.Target.A;
        case 0x0101:
          return Gen2.Target.B;
        }
        break;
      case TAGENCODING:
        switch (m.getu8())
        {
        case 0:
          return Gen2.TagEncoding.FM0;
        case 1:
          return Gen2.TagEncoding.M2;
        case 2:
          return Gen2.TagEncoding.M4;
        case 3:
          return Gen2.TagEncoding.M8;
        }
        break;
      case LINKFREQUENCY:
        switch (m.getu8())
        {
        case 0:
          return Gen2.LinkFrequency.LINK250KHZ.rep;
        case 2:
          return Gen2.LinkFrequency.LINK400KHZ.rep;
        case 3:
          return Gen2.LinkFrequency.LINK40KHZ.rep;
        case 4:
          return Gen2.LinkFrequency.LINK640KHZ.rep;

        }
      case TARI:
        switch (m.getu8())
        {
        case 0:
          return Gen2.Tari.TARI_25US;
        case 1:
          return Gen2.Tari.TARI_12_5US;
        case 2:
          return Gen2.Tari.TARI_6_25US;
        }
      case Q:
        int type = m.getu8();
        if (type == 0)
        {
          return new Gen2.DynamicQ();
        }
        else if (type == 1)
        {
          return new Gen2.StaticQ(m.getu8());
        }
      default:
        throw new IllegalArgumentException("Unknown " + protocol.toString() + 
                                           " parameter " + key.toString());
      }
    }
    else if (protocol == TagProtocol.ISO180006B)
    {
      switch ((ISO180006BConfiguration)key)
      {
      case LINKFREQUENCY:
        switch (m.getu8())
        {
        case 0:
          return Iso180006b.LinkFrequency.LINK40KHZ.rep;
        case 1:
          return Iso180006b.LinkFrequency.LINK160KHZ.rep;
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown " + protocol.toString() + 
                                           " parameter " + key.toString());
      }
    }
    else
    {
      throw new IllegalArgumentException("Protocol parameters not supported for protocol " + protocol.toString());
    }

    throw new ReaderParseException("Could not interpret protocol configuration response");
  }

  /**
   * The statistics avaliable for retrieval by cmdGetReaderStatistics.
   */
  public enum ReaderStatisticsFlag
  {
    /** Total time the port has been transmitting, in milliseconds. Resettable */
      RF_ON_TIME (1),
    /** Detected noise floor with transmitter off. Recomputed when requested, not resettable.  */
        NOISE_FLOOR(2),
    /** Detected noise floor with transmitter on. Recomputed when requested, not resettable.  */
      NOISE_FLOOR_TX_ON(8);

    int value;
    ReaderStatisticsFlag(int v)
    {
      value = v;
    }
  }


  public static class ReaderStatistics
  {
    public int numPorts;
    public int[] rfOnTime;
    public int[] noiseFloor;
    public int[] noiseFloorTxOn;
  }

  /**
   * Get the current per-port statistics.
   *
   * @deprecated
   * @param stats the set of statistics to gather
   * @return a ReaderStatistics structure populated with the requested per-port
   * values
   */
  public ReaderStatistics cmdGetReaderStatistics(Set<ReaderStatisticsFlag> stats)
    throws ReaderException
  {
    Message m = new Message();
    ReaderStatistics ps;
    int i, len, flagBits;
    int[] tmp;
    
    flagBits = 0;
    for (ReaderStatisticsFlag f : stats)
    {
      flagBits |= f.value;
    }

    m.setu8(MSG_OPCODE_GET_READER_STATS);
    m.setu8(0);
    m.setu8(flagBits);
    m.readIndex += 2; // skip option and flags

    ps = new ReaderStatistics();
    while (m.readIndex < m.writeIndex)
    {
      int statFlag = m.getu8();
      if (statFlag == ReaderStatisticsFlag.RF_ON_TIME.value)
      {
        len = m.getu8() / 4;
        tmp = new int[len];
        for (i = 0; i < len; i++)
        {
          tmp[i] = m.getu32();
        }
        ps.rfOnTime = tmp;
        ps.numPorts = len;
      }
      else if (statFlag == ReaderStatisticsFlag.NOISE_FLOOR.value)
      {
        len = m.getu8();
        tmp = new int[len];
        for (i = 0; i < len; i++)
        {
          tmp[i] = (byte)m.getu8(); // value is signed
        }
        ps.noiseFloor = tmp;
        ps.numPorts = len;
      }
      else if (statFlag == ReaderStatisticsFlag.NOISE_FLOOR_TX_ON.value)
      {
        len = m.getu8();
        tmp = new int[len];
        for (i = 0; i < len; i++)
        {
          tmp[i] = (byte)m.getu8(); // value is signed
        }
        ps.noiseFloorTxOn = tmp;
        ps.numPorts = len;
      }
      else
      {
        throw new ReaderParseException("Unkonwn reader statistics flag 0x" + 
                                       Integer.toHexString(statFlag));
      }
      
    }
    return ps;
  }


  /**
   * Get the list of RFID protocols supported by the device.
   *
   * @deprecated
   * @return an array of supported protocols
   */
  public TagProtocol[] cmdGetAvailableProtocols()
    throws ReaderException
  {
    Message m;
    TagProtocol[] protocols;
    TagProtocol p;
    int numProtocols, numKnownProtocols;
    int i, j, index;

    m = sendOpcode(MSG_OPCODE_GET_AVAILABLE_PROTOCOLS);
    numProtocols = (m.writeIndex - m.readIndex) / 2;
    numKnownProtocols = 0;
    index = m.readIndex;
    for (i = 0; i < numProtocols; i++)
    {
      p = codeToProtocolMap.get(m.getu16());
      if (p != TagProtocol.NONE)
      {
        numKnownProtocols++;
      }
    }
    protocols = new TagProtocol[numKnownProtocols];
    j = 0;
    m.readIndex = index;
    for (i = 0; i < numProtocols; i++)
    {
      p = codeToProtocolMap.get(m.getu16());
      if (p != TagProtocol.NONE)
      {
        protocols[j++] = p;
      }
    }

    return protocols;
  }


  /**
   * Get the list of regulatory regions supported by the device.
   *
   * @deprecated
   * @return an array of supported regions
   */
  public Reader.Region[] cmdGetAvailableRegions()
    throws ReaderException
  {
    Message m;
    Reader.Region[] regions;
    Reader.Region r;
    int numRegions, numKnownRegions;
    int i, j;

    m = sendOpcode(MSG_OPCODE_GET_AVAILABLE_REGIONS);
    numRegions = (m.writeIndex - m.readIndex);
    numKnownRegions = 0;
    int index = m.readIndex;
    for (i = 0; i < numRegions; i++)
    {
      r = codeToRegionMap.get(m.getu8());
      if (r != Reader.Region.NONE)
      {
        numKnownRegions++;
      }
    }
    regions = new Reader.Region[numKnownRegions];
    j = 0;
    m.readIndex = index;
    for (i = 0; i < numRegions; i++)
    {
      r = codeToRegionMap.get(m.getu8());
      if (r != Reader.Region.NONE)
      {
        regions[j++] = r;
      }
    }

    return regions;
  }

  /**
   * Get the current temperature of the device.
   *
   * @deprecated
   * @return the temperature, in degrees C
   */
  public int cmdGetTemperature()
    throws ReaderException
  {
    Message m;

    m = sendOpcode(MSG_OPCODE_GET_TEMPERATURE);
    return (byte)m.getu8(); // returned value is signed
  }

  /**
   * Sets the Tx and Rx antenna port. Port numbers range from 1-255.
   *
   * @deprecated
   * @param txPort the logical antenna port to use for transmitting
   * @param rxPort the logical antenna port to use for receiving
   */
  public void cmdSetTxRxPorts(int txPort, int rxPort)
    throws ReaderException 
  {
    if (txPort < 0 || txPort > 255)
    {
      throw new IllegalArgumentException("illegal txAnt " + txPort);
    }
    if (rxPort < 0 || rxPort > 255)
    {
      throw new IllegalArgumentException("illegal rxAnt " + rxPort);
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_SET_ANTENNA_PORT);
    m.setu8(txPort);
    m.setu8(rxPort);
    send(m);
  }

  /**
   * Sets the search list of logical antenna ports. Port numbers range
   * from 1-255.
   *
   * @deprecated
   * @param list the ordered search list. An array of 2-element arrays
   * of integers interpreted as (tx port, rx port) pairs. Example,
   * representing a monostatic antenna on port 1 and a bistatic
   * antenna pair on ports 3 and 4: {{1, 1}, {3, 4}}
   */
  public void cmdSetAntennaSearchList(int[][] list)
    throws ReaderException 
  {

    for (int i = 0 ; i < list.length; i++) 
    {
      if (list[i][0] < 0 || list[i][0] > 255)
      {
        throw new IllegalArgumentException("illegal tx port " + list[i][0]);
      }
      if (list[i][1] < 0 || list[i][1] > 255)
      {
        throw new IllegalArgumentException("illegal rx port " + list[i][1]);
      }
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_SET_ANTENNA_PORT);
    m.setu8(2);
    for (int i=0; i < list.length; i++)
    {
      m.setu8(list[i][0]);
      m.setu8(list[i][1]);
    }
    send(m);
  }

  /**
   * Sets the transmit powers of each antenna port. Note that setting
   * a power level to 0 will cause the corresponding global power
   * level to be used. Port numbers range from 1-255; power levels
   * range from 0-65535.
   *
   * @deprecated
   * @param list an array of 3-element arrays of integers interpreted as
   * (tx port, read power in centidbm, write power in centidbm)
   * triples. Example, with read power levels of 30 dBm and write
   * power levels of 25 dBm : {{1, 3000, 2500}, {2, 3000, 2500}}
   */ 
  public void cmdSetAntennaPortPowers(int[][] list)
    throws ReaderException
  {
    for (int i = 0 ; i < list.length; i++) 
    {
      if (list[i][0] < 0 || list[i][0] > 255)
      {
        throw new IllegalArgumentException("illegal tx port " + list[i][0]);
      }
      if (list[i][1] < 0 || list[i][1] > 65535)
      {
        throw new IllegalArgumentException("illegal read tx power " + list[i][1]);
      }
      if (list[i][2] < 0 || list[i][2] > 65535)
      {
        throw new IllegalArgumentException("illegal write tx power " + list[i][2]);
      }
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_SET_ANTENNA_PORT);
    m.setu8(3);
    for (int i=0, j=1; i < list.length; i++, j+=5)
    {
      m.setu8(list[i][0]);
      m.setu16(list[i][1]);
      m.setu16(list[i][2]);
    }
    send(m);
  }

  /**
   * Sets the transmit powers and settling times of each antenna
   * port. Note that setting a power level to 0 will cause the
   * corresponding global power level to be used. Port numbers range
   * from 1-255; power levels range from 0-65535; settling time ranges
   * from 0-65535.
   *
   * @deprecated
   * @param list an array of 4-element arrays of integers interpreted as
   * (tx port, read power in centidbm, write power in centidbm,
   * settling time in microseconds) tuples.  An example with two
   * antenna ports, read power levels of 30 dBm, write power levels of
   * 25 dBm, and 500us settling times:
   * {{1, 3000, 2500, 500}, {2, 3000, 2500, 500}}
   */ 
  public void cmdSetAntennaPortPowersAndSettlingTime(int[][] list)
    throws ReaderException
  {
    for (int i = 0 ; i < list.length; i++) 
    {
      if (list[i][0] < 0 || list[i][0] > 255)
      {
        throw new IllegalArgumentException("illegal tx port " + list[i][0]);
      }
      if (list[i][1] < 0 || list[i][1] > 65535)
      {
        throw new IllegalArgumentException("illegal read tx power " + list[i][1]);
      }
      if (list[i][2] < 0 || list[i][2] > 65535)
      {
        throw new IllegalArgumentException("illegal write tx power " + list[i][2]);
      }
      if (list[i][3] < 0 || list[i][3] > 65535)
      {
        throw new IllegalArgumentException("illegal settling time " + list[i][3]);
      }
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_SET_ANTENNA_PORT);
    m.setu8(4);
    for (int i=0, j=1; i < list.length; i++, j+=7)
    {
      m.setu8(list[i][0]);
      m.setu16(list[i][1]);
      m.setu16(list[i][2]);
      m.setu16(list[i][3]);
    }
    send(m);
  }

  /**
   * Set the current global Tx power setting for read operations.
   *
   * @deprecated
   * @param centidbm the power level
   */
  public void cmdSetReadTxPower(int centidbm)
    throws ReaderException
  {
    if (centidbm < 0 || centidbm > 65535)
    {
      throw new IllegalArgumentException("illegal power " + centidbm);
    }

    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_TX_READ_POWER);
    m.setu16(centidbm);
    send(m);
  }

  static int tagMetadataSetValue(Set<TagMetadataFlag> flags)
  {
    int value = 0;
    for (TagMetadataFlag flag : flags)
    {
      value |= tagMetadataFlagValues.get(flag);
    }
    return value;
  }


  // Cache the most recent bits->set mapping - a connection
  // to one module will almost always return the same bits.
  private int lastMetadataBits;
  private EnumSet<TagMetadataFlag> lastMetadataFlags;

  Set<TagMetadataFlag> tagMetadataSet(int bits)
  {

    if (bits == lastMetadataBits)
    {
      return lastMetadataFlags.clone();
    }

    EnumSet<TagMetadataFlag> metadataFlags = 
      EnumSet.noneOf(TagMetadataFlag.class);

    for (TagMetadataFlag f : TagMetadataFlag.values())
    {
      if (0 != (tagMetadataFlagValues.get(f) & bits))
      {
        metadataFlags.add(f);
      }
    }

    lastMetadataBits = bits;
    lastMetadataFlags = metadataFlags;

    return metadataFlags;
  }

  /**
   * Set the current RFID protocol for the device to use.
   *
   * @deprecated
   * @param protocol the protocol to use
   */ 
  public void cmdSetProtocol(TagProtocol protocol)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_TAG_PROTOCOL);
    m.setu16(protocolToCodeMap.get(protocol));
    send(m);
  }

  /**
   * Set the current global Tx power setting for write operations.
   *
   * @deprecated
   * @param centidbm the power level.
   */
  public void cmdSetWriteTxPower(int centidbm)
    throws ReaderException
  {
    if (centidbm < 0 || centidbm > 65535)
    {
      throw new IllegalArgumentException("illegal power " + centidbm);
    }
    
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_TX_WRITE_POWER);
    m.setu16(centidbm);
    send(m);
  }
  
  /**
   * Set the frequency hop table.
   *
   * @deprecated
   * @param table A list of frequencies, in kHz. The list may be at
   * most 62 elements.
   */
  public void cmdSetFrequencyHopTable(int[] table)
    throws ReaderException
  {
    Message m = new Message();
    int i, off;

    m.setu8(MSG_OPCODE_SET_FREQ_HOP_TABLE);
    for (i = 0, off = 0; i < table.length; i++, off += 4)
    {
      m.setu32(table[i]);
    }
    send(m);
  }

  /**
   * Set the interval between frequency hops. The valid range for this
   * interval is region-dependent.
   *
   * @deprecated
   * @param hopTime the hop interval, in milliseconds
   */
  public void cmdSetFrequencyHopTime(int hopTime)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_FREQ_HOP_TABLE);
    m.setu8(1);
    m.setu32(hopTime);
    send(m);
  }
  /**
   * set protocol license key
   * @param key  license key
   * @return supported protocol bit mask
   * @throws ReaderException
   */
  public List cmdSetProtocolLicenseKey(byte[] key) throws ReaderException
  {
    List list = new Vector<TagProtocol>();
    Message m=new Message();
    m.setu8(0x9E);
    m.setu8(0x01);
    m.setbytes(key);
    Message msg = send(m);
    msg.getu8();
    int numProtocol=(msg.writeIndex-msg.readIndex)/2;
    for(int i=0;i<numProtocol ;i++)
    {
      list.add(codeToProtocolMap.get(msg.getu16()));
    }
    return list;
   }

  /**
   * Set the state of a single GPIO pin
   *
   * @deprecated
   * @param gpio the gpio pin number
   * @param high whether to set the pin high
   */ 
  public void cmdSetGPIO(int gpio, boolean high)
    throws ReaderException
  {
    Message m = new Message();
    m.setu8(MSG_OPCODE_SET_USER_GPIO_OUTPUTS);
    m.setu8(gpio);
    m.setu8(high ? 1 : 0);
    send(m);
  }

  /**
   * Get direction of a single GPIO pin
   *
   * @deprecated
   * @param pin  GPIO pin number
   * @return true if output pin, false if input pin
   */
  public boolean cmdGetGPIODirection(int pin)
    throws ReaderException
  {
    boolean out;

    Message m = new Message();           
    m.setu8(MSG_OPCODE_SET_USER_GPIO_OUTPUTS);
    m.setu8(pin);
    send(m);
    out = (m.data[6] == 1);
    return out;
  }

  /**
   * Set direction of a single GPIO pin
   *
   * @deprecated
   * @param pin  GPIO pin number
   * @param out  true for output, false for input
   */
  public void cmdSetGPIODirection(int pin, boolean out)
    throws ReaderException
  {
    Message m = new Message();
    m.setu8(MSG_OPCODE_SET_USER_GPIO_OUTPUTS);
    m.setu8(1); // Option flag
    m.setu8(pin);
    m.setu8(out ? 1: 0);
    m.setu8(0); // New value if output
    send(m);
  }

  /** 
   * Get directions of all GPIO pins
   *
   * @deprecated
   * @param wantOut  false = get inputs, true = get outputs 
   * @return list of pins that are set in the requested direction
   */ 
  public int[] getGPIODirection(boolean wantOut)
    throws ReaderException
  {
    int[] retval;
    ArrayList pinList = new ArrayList();
    int pinIndex = 0;
    if (TMR_SR_MODEL_M6E == versionInfo.hardware.part1)
    {
      if ((byte)0xFF == gpioDirections)
      {
        /* Cache the current state */
        gpioDirections = 0;
        for (int pin = 1; pin <= 4 ; pin++)
        {
          if (cmdGetGPIODirection(pin))
          {
            gpioDirections =(byte)( gpioDirections | (1 << pin));
          }
        }
      }
      for (int pin = 1; pin <= 4 ; pin++)
      {
        boolean bitTest = ((gpioDirections >> pin & 1) == 1);
        if (wantOut == bitTest) 
        {
          pinList.add(new Integer(pin));
        }
      }
      retval = new int[pinList.size()];
      for (int i=0; i<pinList.size(); i++)
      {
        retval[i] = (Integer)pinList.get(i);
      }
    }
    else
    {
      retval = new int[] {1,2};
    }
    return retval;
  }

  /** 
   * Set directions of all GPIO pins
   *
   * @deprecated
   * @param wantOut  false = input, true = output 
   * @param pins GPIO pins to set to the desired direction.  All other pins implicitly set the other way.
  */ 
  public void setGPIODirection(boolean wantOut, int[] pins)
    throws ReaderException
  {
    byte newDirections;
    if (TMR_SR_MODEL_M6E == versionInfo.hardware.part1)
    {
      if (wantOut)
      {
        newDirections = 0;
      }
      else
      {
        newDirections = 0x1e;
      }

      for (int i = 0 ; i < pins.length ; i++)
      {
        int bit = 1 << pins[i];
        newDirections = (byte) (newDirections ^ bit); 
      }

      for (int pin = 1 ; pin <= 4 ; pin++)
      {
        int bit = 1 << pin;
        boolean out = (newDirections & bit) != 0;
        cmdSetGPIODirection(pin, out);
      }
      gpioDirections = newDirections;
    }
  }

  /**
   * Set the current regulatory region for the device. Resets region-specific 
   * configuration, such as the frequency hop table.
   *
   * @deprecated
   *
   * @param region the region to set
   */ 
  public void cmdSetRegion(Reader.Region region)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_REGION);
    m.setu8(regionToCodeMap.get(region));
    send(m);
  }

  public void cmdSetRegionLbt(Reader.Region region, boolean lbt)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_REGION);
    m.setu8(regionToCodeMap.get(region));
    m.setu8(lbt ? 1 : 0);
    send(m);
  }

  /**
   * Set the current power mode of the device.
   *
   * @deprecated
   * @param mode the mode to set
   */
  public void cmdSetPowerMode(PowerMode mode)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_POWER_MODE);
    m.setu8(mode.value);
    send(m);
  }

  /**
   * Set the current user mode of the device.
   *
   * @deprecated
   * @param mode the mode to set
   */
  public void cmdSetUserMode(UserMode mode)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_USER_MODE);
    m.setu8(mode.value);
    send(m);
  }

  /**
   * Sets the value of a device configuration setting.
   *
   * @deprecated
   * @param key the setting
   * @param value an object with the setting value. The type of the object
   * is dependant on the key; see the Configuration class for details.
   */
  public void cmdSetReaderConfiguration(SerialReader.Configuration key,
                                  Object value)
    throws ReaderException
  {
    Message m = new Message();
    int data;

    if (key == Configuration.ANTENNA_CONTROL_GPIO)
    {
      data = (Integer)value;
    }
    else if (key == Configuration.UNIQUE_BY_DATA || key==Configuration.UNIQUE_BY_ANTENNA)
    {
      data = ((Boolean)value) ? 0 : 1;
    }
    else
    {
      data = ((Boolean)value) ? 1 : 0;
    }

    m.setu8(MSG_OPCODE_SET_READER_OPTIONAL_PARAMS);
    m.setu8(1);
    m.setu8(key.value);
    m.setu8(data);
    send(m);
  }

  /**
   * Sets the value of a protocol configuration setting.
   *
   * @deprecated
   * @param protocol the protocol of the setting
   * @param key the setting
   * @param value an object with the setting value. The type of the object
   * is dependant on the key; see the ProtocolConfiguration class for details.
   */
  public void cmdSetProtocolConfiguration(TagProtocol protocol,
                                   ProtocolConfiguration key,
                                   Object value)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_PROTOCOL_PARAM);
    m.setu8(protocolToCodeMap.get(protocol));
    m.setu8(key.getValue());

    if (protocol == TagProtocol.GEN2)
    {
      switch ((Gen2Configuration)key)
      {
      case SESSION:
        m.setu8(((Gen2.Session)value).rep);
        break;
      case TARGET:
        switch ((Gen2.Target)value)
        {
        case A:
          m.setu16(0x0100);
          break;
        case B:
          m.setu16(0x0101);
          break;
        case AB:
          m.setu16(0x0000);
          break;
        case BA:
          m.setu16(0x0001);
          break;
        }
        break;
      case TAGENCODING:
        m.setu8(((Gen2.TagEncoding)value).rep);
        break;
      case LINKFREQUENCY:
        switch ((Integer)value)
        {
        case 250:
          m.setu8(0);
          break;
        case 400:
          m.setu8(2);
          break;
        case 40:
          m.setu8(3);
          break;
        case 640:
          m.setu8(4);
          break;
        default:
            throw new IllegalArgumentException("Unsupported BLF " + value.toString());
        }
        break;
       case TARI:
        switch ((Gen2.Tari)value)
        {
        case TARI_25US:
          m.setu8(0);
          break;
        case TARI_12_5US:
          m.setu8(1);
          break;
        case TARI_6_25US:
          m.setu8(2);
          break;
        }
        break;
      case Q:
        if (value instanceof Gen2.DynamicQ)
        {
          m.setu8(0);
        }
        else if (value instanceof Gen2.StaticQ)
        {
          m.setu8(1);
          m.setu8(((Gen2.StaticQ)value).initialQ);
        }
        else
          throw new IllegalArgumentException("Unknown Q algorithm " + value.toString());
        break;
      default:
        throw new IllegalArgumentException("Unknown " + protocol.toString() + 
                                           " parameter " + key.toString());
      }
    }
    else if (protocol == TagProtocol.ISO180006B)
    {
      switch ((ISO180006BConfiguration)key)
      {
      case LINKFREQUENCY:
        switch ((Integer)value)
        {
        case 40:
          m.setu8(0);
          break;
        case 160:
          m.setu8(1);
          break;
        default:
            throw new IllegalArgumentException("Unsupported BLF " + value.toString());

        }
      default:
        throw new IllegalArgumentException("Unknown " + protocol.toString() + 
                                           " parameter " + key.toString());
      }

    }
    else
    {
      throw new IllegalArgumentException("Protocol parameters not supported for protocol " + protocol.toString());
    }

    send(m);
  }

  /**
   * Setting user profile on the basis of option,key and value parameter
   * @param option Save,restore,verify and reset configuration
   * @param key  Which part of configuration to operate on
   * @param val Type of configuration value to use (default, custom...)
   */
  
  public void cmdSetUserProfile(SetUserProfileOption option, ConfigKey key, ConfigValue val)
  {
      try{

      Message m = new Message();
      m.setu8(MSG_OPCODE_SET_USER_PROFILE);
      m.setu8(option.getValue());
      m.setu8(key.getValue());
      m.setu8(val.getValue());
      Message msg;
      msg=send(m);
      if (option == SetUserProfileOption.RESTORE)
      {
        openPort();
      }
      }catch(Exception e){
          e.printStackTrace();
      }
  }
  /**
   * get save/restore configuration
   * @param data Byte array consiting of opcode option
   * @return  Byte array
   */
   public byte[] cmdGetUserProfile(byte data[])
        {
            try{
        Message m=new Message();

        m.setu8(MSG_OPCODE_GET_USER_PROFILE);
        for(int i=0;i<data.length;i++){
            m.setu8(data[i]);
        }
        
        Message msg;
        msg=send(m);
        byte[] response;
        int resLen=(msg.writeIndex-msg.readIndex);
        response = new byte[resLen];
        for (int i = 0; i < resLen; i++)
        {
         response[i] = (byte) msg.getu8();
        }
        String str=byteArrayToHexString(response);
        
        return response;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }

        }


  /**
   * Reset the per-port statistics.
   *
   * @deprecated
   *
   * @param stats the set of statistics to reset. Only the RF on time statistic
   * may be reset.
   */
  public void cmdResetReaderStatistics(Set<ReaderStatisticsFlag> stats)
    throws ReaderException
  {
    Message m = new Message();
    int flagBits;

    flagBits = 0;
    for (ReaderStatisticsFlag f : stats)
    {
      flagBits |= f.value;
    }

    m.setu8(MSG_OPCODE_GET_READER_STATS);
    m.setu8(1);
    m.setu8(flagBits);
  }

  /**
   * Set the operating frequency of the device.
   * Testing command.
   *
   * @param frequency the frequency to set, in kHz
   */ 
  public void cmdTestSetFrequency(int frequency)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_SET_OPERATING_FREQ);
    m.setu32(frequency);
    send(m);
  }


  /**
   * Turn CW transmission on or off.
   * Testing command.
   *
   * @param on whether to turn CW on or off
   */ 
  public void cmdTestSendCw(boolean on)
    throws ReaderException
  {
    Message m = new Message();

    m.setu8(MSG_OPCODE_TX_CW_SIGNAL);
    m.setu8(on ? 1 : 0);
    send(m);
  }


  /**
   * Turn on pseudo-random bit stream transmission for a particular
   * duration.  
   * Testing command.
   *
   * @param duration the duration to transmit the PRBS signal. Valid
   * range is 0-65535
   */ 
  public void cmdTestSendPrbs(int duration)
    throws ReaderException
  {
    if (duration < 0 || duration > 65535)
    {
      throw new IllegalArgumentException("illegal PRBS duration " + duration);
    }

    Message m = new Message();
    m.setu8(MSG_OPCODE_TX_CW_SIGNAL);
    m.setu8(2);
    m.setu16(duration);
    send(m);
  }

  // package-visible (non-public) constructor
  SerialReader(String serialDevice)
    throws ReaderException
  {
    this.serialDevice = serialDevice;

    serialListeners = new Vector<TransportListener>();
    initParams();
  }



  public SerialReader(SerialTransport st)
    throws ReaderException
  {
    this.st = st;
    serialListeners = new Vector<TransportListener>();
    initParams();
  }

  public SerialTransport getSerialTransport()
  {
    return st;
  }

  void initParams()
  {
    addParam("/reader/baudRate",
             Integer.class, baudRate, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 baudRate = (Integer)value;
                 if (connected && (st.getBaudRate() != baudRate))
                 {
                   cmdSetBaudRate(baudRate);
                   st.setBaudRate(baudRate);
                 }
                 return value;
                }
               public Object get(Object value)
               {
                 return baudRate;
               }
             });

    transportTimeout = 100;
    addParam("/reader/transportTimeout",
             Integer.class, transportTimeout, true,
             new SettingAction()
             {
               public Object set(Object value)
               {
                 transportTimeout = (Integer)value;
                 return value;
                }
               public Object get(Object value)
               {
                 return value;
               }
             });

    commandTimeout = 1000;
    addParam("/reader/commandTimeout",
             Integer.class, commandTimeout, true,
             new SettingAction()
             {
               public Object set(Object value)
               {
                 commandTimeout = (Integer)value;
                 return value;
                }
               public Object get(Object value)
               {
                 return value;
               }
             });
    addParam(
      "/reader/powerMode",
               PowerMode.class, null, true,
               new SettingAction()
               {
                 public Object set(Object value)
                   throws ReaderException
                 {
                   cmdSetPowerMode((PowerMode)value);
                   return value;
                 }
                 public Object get(Object value)
                   throws ReaderException
                 {
                   return cmdGetPowerMode();
                 }
               });

  }

  public void connect()
    throws ReaderException
  {
    boolean success;
    int program;
    AntennaPort ap;
    int tagopAntenna;
    Setting s;

    if (connected)
      return;
    int universalBaudRate= (Integer)paramGet("/reader/baudRate");
    // Serial baud rates to try, in the order we're most likely to find them
    int[] rates = { universalBaudRate, 115200, 9600, 921600, 19200, 38400, 57600, 230400, 460800};

    if (st == null && serialDevice != null)
    {
      st = new SerialTransportNative(serialDevice);
    }

    st.open();
      
    versionInfo = null;
    success = false;
    int numAttempts =0;
    
      for (int rate : rates)
      {
        try
        {
          st.setBaudRate(rate);
          baudRate = rate;
          st.flush();
          versionInfo = cmdVersion();
          protocolSet = EnumSet.noneOf(TagProtocol.class);
          for (TagProtocol t : versionInfo.protocols)
          {
            protocolSet.add(t);
          }
        }
        catch (ReaderCommException re)
        {
          continue;
        }
        catch (ReaderException re)
        {
          st.shutdown();
          throw re; // A error response to a version command is bad news
        }

        success = true;
        break;
       }
    if (success == false)
    {
      st.shutdown();
      throw new ReaderCommException("No response from reader at any baud rate.");
    }

   

    boot(region);

    addParam("/reader/antenna/portList",
             int[].class, null, false, 
             new ReadOnlyAction() 
             {
               public Object get(Object value)
               {
                 return ports.clone();
               }
             });
       
    addParam("/reader/gpio/inputList",
             int[].class, null, true, 
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 setGPIODirection(false,(int[])value);
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return getGPIODirection(false);
               }
             });

    addParam("/reader/gpio/outputList",
             int[].class, null, true, 
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 setGPIODirection(true,(int[])value);
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return getGPIODirection(true);
               }
             });

    addParam("/reader/antenna/connectedPortList",
             int[].class, null, false,
              new ReadOnlyAction()
              {
                public Object get(Object value)
                  throws ReaderException
                {
                  return getConnectedAntennas();
                }
             });

    addParam("/reader/gen2/accessPassword",
             Gen2.Password.class, null, true, 
             new SettingAction()
             {
               public Object set(Object value)
               {
                 if (value == null)
                   accessPassword = 0;
                 else
                   accessPassword = ((Gen2.Password)value).password;
                 return value;
               }
               public Object get(Object value)
               {
                 return value;
               }
             });
    addParam("/reader/gen2/writeMode",
             Gen2.WriteMode.class, Gen2.WriteMode.WORD_ONLY, true, null);

    addParam("/reader/radio/readPower",
             Integer.class, null, true,
             new SettingAction() 
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 cmdSetReadTxPower((Integer)value);
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return cmdGetReadTxPower();
               }
             });
               
    addParam("/reader/radio/writePower",
             Integer.class, null, true,
             new SettingAction() 
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 cmdSetWriteTxPower((Integer)value);
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return cmdGetWriteTxPower();
               }
    });
    addParam("/reader/radio/enablePowerSave",
             Boolean.class, null, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 cmdSetReaderConfiguration(Configuration.TRANSMIT_POWER_SAVE,value);
                 return value;

               }
               public Object get(Object value)
                 throws ReaderException
               {
                 value=cmdGetReaderConfiguration(Configuration.TRANSMIT_POWER_SAVE);
                 return value;
               }
    });

    addParam("/reader/version/serial",
             String.class, null, true,
             new ReadOnlyAction()
    {
        public Object get(Object value) throws ReaderException
               {
                 return (String)(cmdGetSerialNumber(0x00, 0x40));
               }
});
    class ProtocolConfigurationKeySettingAction implements SettingAction
    {
      TagProtocol protocol;
      ProtocolConfiguration key;

      ProtocolConfigurationKeySettingAction(TagProtocol p, ProtocolConfiguration k)
      {
        protocol = p;
        key = k;
      }

      public Object set(Object value)
        throws ReaderException
      {
        cmdSetProtocolConfiguration(protocol, key, value);
        return value;
      }

      public Object get(Object value)
        throws ReaderException
      {
        return cmdGetProtocolConfiguration(protocol, key);
      }
    }

    addParam("/reader/gen2/session",
             Gen2.Session.class, null, true,
             new ProtocolConfigurationKeySettingAction(
               TagProtocol.GEN2, Gen2Configuration.SESSION));

    addUnconfirmedParam("/reader/gen2/tagEncoding",
                        Gen2.TagEncoding.class, null, true,
                        new ProtocolConfigurationKeySettingAction(
                          TagProtocol.GEN2, Gen2Configuration.TAGENCODING));

    addUnconfirmedParam("/reader/gen2/q",
                        Gen2.Q.class, null, true,
                        new ProtocolConfigurationKeySettingAction(
                          TagProtocol.GEN2, Gen2Configuration.Q));

    addUnconfirmedParam("/reader/gen2/BLF",
                        Integer.class, null, true,
                        new ProtocolConfigurationKeySettingAction(
                          TagProtocol.GEN2, Gen2Configuration.LINKFREQUENCY));

    addUnconfirmedParam("/reader/gen2/tari",
                        Gen2.Tari.class, null, true,
                        new ProtocolConfigurationKeySettingAction(
                          TagProtocol.GEN2, Gen2Configuration.TARI));


    addUnconfirmedParam("/reader/iso180006b/BLF",
                        Integer.class, null, true,
                        new ProtocolConfigurationKeySettingAction(
                          TagProtocol.ISO180006B,
                          ISO180006BConfiguration.LINKFREQUENCY));
    
    addParam("/reader/region/id",
             Reader.Region.class, null, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 cmdSetRegion((Reader.Region)value);
                 region = (Reader.Region)value;
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return cmdGetRegion();
               }
             });

    addParam("/reader/region/lbt/enable",
             Boolean.class, null, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 int[] hopTable = (int[])paramGet("/reader/region/hopTable");
                 int hopTime = (Integer)paramGet("/reader/region/hopTime");
                 try
                 {
                   cmdSetRegionLbt(region, (Boolean)value);
                 }
                 catch (ReaderCodeException re)
                 {
                   throw new IllegalArgumentException(
                     "LBT may not be set in this region");
                 }
                 paramSet("/reader/region/hopTable", hopTable);
                 paramSet("/reader/region/hopTime", hopTime);
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 try
                 {
                   return cmdGetRegionConfiguration(
                          RegionConfiguration.LBTENABLED);
                 }
                 catch (ReaderCodeException re)
                 {
                   // In regions that don't support LBT, the command doesn't even work.
                   return false;
                 }
               }
             });

    addParam("/reader/region/supportedRegions",
             Reader.Region[].class, null, false,
             new ReadOnlyAction()
             {
               public Object get(Object value)
                 throws ReaderException
               {
                 return cmdGetAvailableRegions();
               }
             });

    addParam("/reader/region/hopTable",
             int[].class, null, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 cmdSetFrequencyHopTable((int[])value);
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return cmdGetFrequencyHopTable();
               }
             });

    addParam("/reader/region/hopTime",
             Integer.class, null, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 cmdSetFrequencyHopTime((Integer)value);
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return cmdGetFrequencyHopTime();
               }
             });
                      
    addParam("/reader/read/plan",
             ReadPlan.class, new SimpleReadPlan(), true, 
             new SettingAction() 
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 if (value instanceof SimpleReadPlan)
                 {
                   SimpleReadPlan srp = (SimpleReadPlan)value;
                   if (!protocolSet.contains(srp.protocol))
                   {
                     throw new IllegalArgumentException(
                       "Unsupported protocol " + srp.protocol + ".");
                   }
                 }
                 return value;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 return value;
               }
             });

    addParam("/reader/tagop/protocol",
             TagProtocol.class, TagProtocol.GEN2, true,
             new SettingAction() 
             {
               public Object set(Object value)
               {
                 TagProtocol p = (TagProtocol)value;
                 
                 if (!protocolSet.contains(p))
                 {
                   throw new IllegalArgumentException(
                     "Unsupported protocol " + p + ".");
                 }
                 return value;
               }
               public Object get(Object value)
               {
                 return value;
               }
    });

    ports = getAntennaPorts();
    portSet = new HashSet<Integer>();
    for (int p : ports)
    {
      portSet.add(p);
    }

    ap = cmdGetAntennaConfiguration();
    if (ap.portTerminatedList.length > 0)
    {
      tagopAntenna = ap.portTerminatedList[0];
    }
    else 
    {
      tagopAntenna = 0;
    }
    addParam("/reader/tagop/antenna",
             Integer.class, tagopAntenna, true,
             new SettingAction() 
             {
               public Object set(Object value)
               {
                 int i, a = (Integer)value;
                 for (i = 0 ; i < ports.length; i++)
                 {
                   if (a == ports[i])
                   {
                     break;
                   }
                 }
                 if (i == ports.length)
                 {
                   throw new IllegalArgumentException(
                     "Invalid antenna " + a + ".");
                 }
                 return value;
               }
               public Object get(Object value)
               {
                 return value;
               }
             });

    class ConfigurationKeySettingAction implements SettingAction
    {
      Configuration key;

      ConfigurationKeySettingAction(Configuration k)
      {
        key = k;
      }

      public Object set(Object value)
        throws ReaderException
      {
        cmdSetReaderConfiguration(key, value);
        return value;
      }

      public Object get(Object value)
        throws ReaderException
      {
        return cmdGetReaderConfiguration(key);
      }
    }

    addUnconfirmedParam("/reader/tagReadData/uniqueByAntenna",
                        Boolean.class, false, true,
                        new ConfigurationKeySettingAction(
                          Configuration.UNIQUE_BY_ANTENNA));

    addUnconfirmedParam("/reader/tagReadData/uniqueByData",
                        Boolean.class, false, true,
                        new ConfigurationKeySettingAction(
                          Configuration.UNIQUE_BY_DATA));

    addUnconfirmedParam("/reader/antenna/checkPort",
                        Boolean.class, false, true,
                        new ConfigurationKeySettingAction(
                          Configuration.SAFETY_ANTENNA_CHECK));
    addUnconfirmedParam("/reader/tagReadData/recordHighestRssi",
                        Boolean.class, false, true,
                        new ConfigurationKeySettingAction(
                          Configuration.RECORD_HIGHEST_RSSI));
    addUnconfirmedParam("/reader/tagReadData/reportRssiInDbm",
                        Boolean.class, false, true,
                        new ConfigurationKeySettingAction(
                          Configuration.RSSI_IN_DBM));
    addUnconfirmedParam(
      "/reader/antenna/portSwitchGpos",
               int[].class, null, true,
               new SettingAction()
               {
                 public Object set(Object value)
                   throws ReaderException
                 {
                   int val;
                   int[] list = (int[]) value;
                   if (list.length == 0)
                   {
                     val = 0;
                   }
                   else if (list.length == 1 && list[0] == 1)
                   {
                     val = 1;
                   }
                   else if (list.length == 1 && list[0] == 2)
                   {
                     val = 2;
                   }
                   else if (list.length == 2 && list[0] == 1 
                            && list[1] == 2)
                   {
                     val = 3;
                   }
                   else
                   {
                     throw new IllegalArgumentException(
                       "Illegal set of GPOs for antenna switch control");
                   }
                   cmdSetReaderConfiguration(
                     Configuration.ANTENNA_CONTROL_GPIO,
                     (Integer)val);
                   ports = getAntennaPorts();
                   portSet = new HashSet<Integer>();
                   for (int p : ports)
                   {
                     portSet.add(p);
                   }
                   // Update txRxMap
                   int[][] newmap = new int[ports.length][3];
                   for (int p=0;p<ports.length;p++)
                   {
                       for(int i=0;i<3;i++)
                       {
                         newmap[p][i]=ports[p];
                         System.out.println("new map ["+p+"]["+i+"]:"+newmap[p][i]);
                       }
                   }
                   // XXX check/invalidate current readPlan, tagopAntenna?

                   paramSet("/reader/antenna/txRxMap", newmap);
                   
                   return value;
                 }
                 public Object get(Object value)
                   throws ReaderException
                 {
                   int val;
                   int[] rv;
                   val = (Integer)cmdGetReaderConfiguration(
                     Configuration.ANTENNA_CONTROL_GPIO);
                   if (val == 0)
                   {
                     rv = new int[] {};
                   }
                   else if (val == 1)
                   {
                     rv = new int[] {1};
                   }
                   else if (val == 2)
                   {
                     rv = new int[] {2};
                   }
                   else if (val == 3)
                   {
                     rv = new int[] {1, 2};
                   }
                   else
                   {
                     throw new ReaderException("Unknown response " +
                                               "to config request");
                   }
                   return rv;
                 }
               });

    antennaPortMap = new HashMap<Integer,int[]>();
    antennaPortReverseMap = new HashMap<Integer,Integer>();
    antennaPortTransmitMap = new HashMap<Integer,Integer>();
    for (int port : ports)
    {
      antennaPortMap.put(port, new int[] {port, port});
      antennaPortReverseMap.put((port << 4) | port, port);
      antennaPortTransmitMap.put(port, port);
    }
    addParam("/reader/antenna/txRxMap",
             int[][].class, null, true,
             new SettingAction()
             {
               public Object set(Object value)
                 throws ReaderException
               {
                 antennaPortMap.clear();
                 antennaPortReverseMap.clear();
                 antennaPortTransmitMap.clear();
                 for (int[] triple : (int[][])value)
                 {
                   if (!portSet.contains(triple[1]))
                   {
                     throw new IllegalArgumentException(
                       "Invalid port number " + triple[1]);
                   }
                   if (!portSet.contains(triple[2]))
                   {
                     throw new IllegalArgumentException(
                       "Invalid port number " + triple[2]);
                   }

                   antennaPortMap.put(triple[0],
                                      new int[] {triple[1], triple[2]});
                   antennaPortReverseMap.put((triple[1] << 4) | triple[2],
                                             triple[0]);
                   antennaPortTransmitMap.put(triple[1], triple[0]);
                 }
                 // Mapping may have changed, invalidate current-antenna cache
                 currentAntenna = 0;
                 searchList = null;
                 return antennaPortMap;
               }
               public Object get(Object value)
                 throws ReaderException
               {
                 List<int[]> rvList = new Vector<int[]>();
                 for (int p : antennaPortMap.keySet())
                 {
                   int[] pair = antennaPortMap.get(p);
                   rvList.add(new int[] { p, pair[0], pair[1] });
                 }

                 return rvList.toArray(new int[rvList.size()][]);
               }
             });
             

      class PortParamSettingAction implements SettingAction
      {
        int paramColumn;

        PortParamSettingAction(int paramColumn)
        {
          this.paramColumn = paramColumn;
        }

        public Object set(Object value)
          throws ReaderException
        {
          boolean found;
          int[][] rpList = (int[][])value;
          setArrayColumn(portParamList, paramColumn, 0);
          setArrayColumnByPort(portParamList, paramColumn, rpList);
          cmdSetAntennaPortPowersAndSettlingTime(portParamList);
          return value;
        }
        public Object get(Object value)
          throws ReaderException
        {
          portParamList = cmdGetAntennaPortPowersAndSettlingTime();
          return extractNonzeroArrayColumn(portParamList, paramColumn);
        }


        void setArrayColumn(int[][] array, int column, int value)
        {
          for (int[] row : array)
            row[column] = value;
        }

        void setArrayColumnByPort(int[][] array, int column, int[][] pairs)
        {
          boolean found;
          for (int[] pair : pairs)
          {
            found = false;
            for (int[] row : array)
              if (pair[0] == row[0])
              {
                row[column] = pair[1];
                found = true;
                break;
              }
            if (!found)
            {
              throw new IllegalArgumentException(
                "Invalid port number " + pair[0]);
            }
          }
        }

        int[][] extractNonzeroArrayColumn(int[][] array, int column)
        {
          List<int[]> returnList = new Vector<int[]>();
          for (int[] row : array)
            if (row[column] != 0)
              returnList.add(new int[] {row[0],row[column]});
          return returnList.toArray(new int[returnList.size()][]);
        }
      }

      addUnconfirmedParam("/reader/radio/portReadPowerList",
               int[][].class, null, true,
               new PortParamSettingAction(1));
      addUnconfirmedParam("/reader/radio/portWritePowerList",
               int[][].class, null, true,
               new PortParamSettingAction(2));
      addUnconfirmedParam("/reader/antenna/settlingTimeList",
               int[][].class, null, true,
               new PortParamSettingAction(3));

    addUnconfirmedParam(
      "/reader/gen2/target",
               Gen2.Target.class, null, true,
               new SettingAction()
               {
                 public Object set(Object value)
                   throws ReaderException
                 {
                   cmdSetProtocolConfiguration(TagProtocol.GEN2,
                                               Gen2Configuration.TARGET,
                                               value);
                   return value;
                 }
                 
                 public Object get(Object value)
                   throws ReaderException
                 {
                   return cmdGetProtocolConfiguration(TagProtocol.GEN2,
                                                      Gen2Configuration.TARGET);
                   
                 }
               });


    addUnconfirmedParam(
      "/reader/radio/temperature",
               Integer.class, null, false,
               new ReadOnlyAction()
               {
                 public Object get(Object value)
                   throws ReaderException
                 {
                   return cmdGetTemperature();
                 }
               });

    connected = true;
  }

  void boot(Region region)
    throws ReaderException
  {
    int program;
    Setting s;
    String model;
    powerMode = PowerMode.INVALID; 

    try
    {
      program = cmdGetCurrentProgram();
      if ((program & 0x3) == 1)
      {
        cmdBootFirmware();
      }
      else if ((program & 0x3) != 2)
      {
        throw new ReaderException("Unknown current program 0x" 
                                  + Integer.toHexString(program));
      }
    }
    catch (ReaderCodeException re)
    {
      // The reader is probably in the M4e bootloader.
      try
      {
        cmdBootFirmware();
      }
      catch (ReaderCodeException re2)
      {
      }
    }

    // Read and set the power mode ASAP
    // so that it can be referenced by sendMessage
    powerMode = cmdGetPowerMode();

    if (versionInfo.hardware.equals(m4eType1) ||
        versionInfo.hardware.equals(m4eType2))
      model = "M4e";
    else
    {
      switch (versionInfo.hardware.part1)
      {
      case TMR_SR_MODEL_M5E: model = "M5e"; break;
      case TMR_SR_MODEL_M5E_COMPACT: model = "M5e Compact"; break;
      case TMR_SR_MODEL_M5E_EU: model = "M5e EU"; break;
      case TMR_SR_MODEL_M4E: model = "M4e"; break;
      case TMR_SR_MODEL_M6E: model = "M6e"; break;
      default: model = "Unknown"; break;
      }
    }

    paramSet("/reader/baudRate",baudRate);

    if (region != null)
      cmdSetRegion(region);

    setProtocol(TagProtocol.GEN2);

    try
    {
      cmdSetReaderConfiguration(Configuration.EXTENDED_EPC, true);
    }
    catch (ReaderException e)
    {
    }

    addParam("/reader/version/model",
             String.class, model, false, null);


    addParam("/reader/version/hardware",
             String.class, null, false,
             new ReadOnlyAction() 
             {
               public Object get(Object value)
                 throws ReaderException
               {
                 // This value doesn't change during runtime, so let
                 // the param system cache it.
                 if (value == null)
                 {
                   StringBuilder sb = new StringBuilder();
                   sb.append(versionInfo.hardware.toString());
                   try 
                   {
                     byte[] hwinfo = cmdGetHardwareVersion(0, 0);
                     sb.append("-");
                     for (byte b : hwinfo)
                       sb.append(String.format("%02x", b));
                   }
                   catch (ReaderCodeException re)
                   {
                     // The module throws an exception if there's no HW info programmed in.
                     // Not really an error here, just a lack of data.
                   }
                   value = sb.toString();
                 }
                 return value;
               }
             });         

    addParam("/reader/version/software",
             String.class,
             String.format("%s-%s-BL%s",
                           versionInfo.fwVersion.toString(),
                           versionInfo.fwDate.toString(),
                           versionInfo.bootloader.toString()),
             false, null);

    addParam("/reader/version/supportedProtocols",
             TagProtocol[].class, versionInfo.protocols, false,
             new ReadOnlyAction() 
             {
               public Object get(Object value)
               {
                 TagProtocol[] protos = (TagProtocol[])value;
                 TagProtocol[] protosCopy;
                 protosCopy = new TagProtocol[protos.length];
                 System.arraycopy(protos, 0, protosCopy, 0, 
                                  protos.length);
                 return protosCopy;
               }
             });

    addParam("/reader/radio/powerMin",
             Integer.class, null, false,
             new ReadOnlyAction()
             {
               public Object get(Object value)
                 throws ReaderException
               {
                 if (powerLimits == null)
                   powerLimits = cmdGetReadTxPowerWithLimits();
                 return powerLimits[2];
               }
             });

    addParam("/reader/radio/powerMax",
             Integer.class, null, false,
             new ReadOnlyAction()
             {
               public Object get(Object value)
                 throws ReaderException
               {
                 if (powerLimits == null)
                   powerLimits = cmdGetReadTxPowerWithLimits();
                 return powerLimits[1];
               }
             });

    addUnconfirmedParam(
      "/reader/userMode",
               UserMode.class, null, true,
                new SettingAction()
                {
                  public Object set(Object value)
                    throws ReaderException
                  {
                    cmdSetUserMode((UserMode)value);
                    return value;
                  }
                  public Object get(Object value)
                    throws ReaderException
                  {
                    return cmdGetUserMode();
                  }
               });

    useStreaming = model.equals("M6e");
    useStreaming = false;
  }

  public void destroy()
  {
    connected = false;
    try 
    {
      st.shutdown();
    } 
    catch (ReaderException re)
    {
      // Nothing to do here; we're trying to shut down.
    }
    st = null;
  }

  int[] getAntennaPorts()
    throws ReaderException
  {
    int[] newPorts;
    int numPorts;
    try
    {
      // Try the new antenna-detect command, which as a side effect
      // enumerates all valid antennas for us.
      int[][] antennas;
      antennas = cmdAntennaDetect();
      numPorts = antennas.length;
      newPorts = new int[numPorts];
      for (int i = 0; i < numPorts; i++)
        newPorts[i] = antennas[i][0];
    }
    catch (ReaderCodeException re)
    {
      AntennaPort ap = cmdGetAntennaConfiguration();
      numPorts = ap.numPorts;
      newPorts = new int[numPorts];
      for (int i = 0; i < numPorts; i++)
        newPorts[i] = i + 1;
    }
    return newPorts;
  }

  protected void setCurrentAntenna(int a)
    throws ReaderException
  {
    if (currentAntenna != a)
    {
      int[] ports = antennaPortMap.get(a);
      if (ports == null)
      {
        throw new IllegalArgumentException(
          String.format("No antenna number %d in /reader/antenna/txRxMap", a));
      }
      cmdSetTxRxPorts(ports[0], ports[1]);
      currentAntenna = a;
    }
  }

  protected void setSearchAntennaList(int[] list)
    throws ReaderException
  {
    if (!Arrays.equals(searchList, list))
    {
      int[][] pairList = new int[list.length][];
      int[] pair;
      for (int i = 0 ; i < list.length; i++)
      {
        pair = antennaPortMap.get(list[i]);
        if (pair == null)
        {
          throw new IllegalArgumentException(
            String.format("No antenna number %d in /reader/antenna/txRxMap",
                          list[i]));
        }
        pairList[i] = pair;
      }
      cmdSetAntennaSearchList(pairList);
      searchList = list.clone();
    }
  }

  protected void setProtocol(TagProtocol protocol)
    throws ReaderException
  {

    if (protocol != currentProtocol)
    {
      cmdSetProtocol(protocol);
      currentProtocol = protocol;
    }
  }

  void checkOpAntenna()
    throws ReaderException
  {
    int a;
    a = (Integer)paramGet("/reader/tagop/antenna");
    if (a == 0)
    {
      throw new ReaderException("No antenna detected or configured " 
                                + "for tag operations");
    }
    setCurrentAntenna(a);
  }

  void checkRegion()
    throws ReaderException
  {
    if (region == null)
    {
      throw new ReaderException("Region must be set before RF operation");
    }
  }

  // Fill in a new TagData object by parsing module data
  void metadataFromMessage(TagReadData t, Message m,
                          Set<TagMetadataFlag> meta)
  {

    t.metadataFlags = meta;

    if (meta.contains(TagMetadataFlag.READCOUNT))
    {
      t.readCount = m.getu8();
    }
    if (meta.contains(TagMetadataFlag.RSSI))
      t.rssi = (byte)m.getu8(); // keep the sign here
    if (meta.contains(TagMetadataFlag.ANTENNAID))
      t.antenna = m.getu8();
    if (meta.contains(TagMetadataFlag.FREQUENCY))
    {
      t.frequency = m.getu24();
    }
    if (meta.contains(TagMetadataFlag.TIMESTAMP))
    {
      t.readOffset = m.getu32();
    }
    if (meta.contains(TagMetadataFlag.PHASE))
    {
      t.phase = m.getu16();
    }
    if (meta.contains(TagMetadataFlag.PROTOCOL))
      t.readProtocol = codeToProtocolMap.get(m.getu8());
    if (meta.contains(TagMetadataFlag.DATA))
    {
      int dataBits = m.getu16();
      t.data = new byte[(dataBits + 7)/8];
      m.getbytes(t.data, t.data.length);
    }
    if (meta.contains(TagMetadataFlag.GPIO_STATUS))
    {
      byte gpioByte = (byte) m.getu8();
      int gpioNumber;
      switch (versionInfo.hardware.part1)
      {
        case TMR_SR_MODEL_M6E :
          gpioNumber = 4;
          break;
        case TMR_SR_MODEL_M5E :
          gpioNumber = 2;
          break;
        default:
          gpioNumber = 4;
          break ;
      }

    t.gpio = new GpioPin[gpioNumber];
    for (int i = 0; i < gpioNumber; i++)
    {
       (t.gpio)[i] = new GpioPin((i + 1), (((gpioByte>>i)&1)==1));
    }

    }
  }

  static Set<TagMetadataFlag> allMeta = EnumSet.of(
    TagMetadataFlag.READCOUNT,
    TagMetadataFlag.RSSI,
    TagMetadataFlag.ANTENNAID,
    TagMetadataFlag.FREQUENCY,
    TagMetadataFlag.PHASE,
    TagMetadataFlag.TIMESTAMP,
    TagMetadataFlag.DATA,
    TagMetadataFlag.PROTOCOL,
    TagMetadataFlag.GPIO_STATUS);

  static int allMetaBits = tagMetadataSetValue(allMeta);

  public TagReadData[] read(long timeout)
    throws ReaderException
  {
    List<TagReadData> tagvec;

    checkConnection();
    checkRegion();

    tagvec = new Vector<TagReadData>();

    readInternal(timeout, (ReadPlan)paramGet("/reader/read/plan"), tagvec);

    return tagvec.toArray(new TagReadData[tagvec.size()]);
  }

  void readInternal(long timeout, ReadPlan rp, List<TagReadData> tagvec)
    throws ReaderException
  {
    TagReadData[] trarray;
    int readTimeout;
    int numTags=0, count;
    long baseTime, now, endTime;
    int readAntenna;
    TagFilter readFilter;

    if (timeout < 0)
    {
      throw new IllegalArgumentException("timeout " + timeout 
                                         + "ms out of range");
    }
    HashMap<TagProtocol,TagFilter> protocolFilterPair=new HashMap<TagProtocol,TagFilter>();
    if (rp instanceof MultiReadPlan)
    {
      MultiReadPlan mrp = (MultiReadPlan)rp;
            
      List<TagReadData> tg=new Vector<TagReadData>();
      if(mrp.totalWeight==0)
      {
        for (ReadPlan r : mrp.plans)
        {
          SimpleReadPlan srp = (SimpleReadPlan)r;
          protocolFilterPair.put(srp.protocol, srp.filter);
        }
        setSearchAntennaList(prepForSearch((SimpleReadPlan)mrp.plans[0]));
        cmdClearTagBuffer();
        cmdMultiProtocolSearch((int)MSG_OPCODE_READ_TAG_ID_MULTIPLE,protocolFilterPair,TagMetadataFlag.ALL,
              ((useStreaming? READ_MULTIPLE_SEARCH_FLAGS_TAG_STREAMING : 0)|READ_MULTIPLE_SEARCH_FLAGS_SEARCH_LIST),(short)timeout,tagvec);
      }
      else
      {
        for (ReadPlan r : mrp.plans)
        {
          readInternal(timeout * r.weight / mrp.totalWeight, r, tagvec);
        }
      }
      return;
    }

    readFilter = null;
    if (!(rp instanceof SimpleReadPlan))
    {
      throw new UnsupportedOperationException("Unsupported read plan " +
                                              rp.getClass().getName());
    }

    SimpleReadPlan sp = (SimpleReadPlan)rp;

    setProtocol(sp.protocol);

    readFilter = sp.filter;
      
    setSearchAntennaList(prepForSearch(sp));

    now = System.currentTimeMillis();
    endTime = now + timeout;
    int tm=0;
    while (now < endTime)
    {
      readTimeout = ((endTime - now) < 65535) ? (int)(endTime - now) : 65535;

      cmdClearTagBuffer();
      baseTime = System.currentTimeMillis();
      Message m = new Message();
       int searchflag=AntennaSelection.CONFIGURED_LIST.value;
        if (useStreaming)
        {
            searchflag |= READ_MULTIPLE_SEARCH_FLAGS_TAG_STREAMING;
        }
        if (sp.Op != null)
        {
            searchflag |= READ_MULTIPLE_SEARCH_FLAGS_EMBEDDED_OP;
        }
        msgSetupReadTagMultiple(m, readTimeout,searchflag,
                                  readFilter, sp.protocol, TagMetadataFlag.ALL, 0);

        if(sp.Op!=null)  // embedded command
        {
          m.setu8(0x01);
           tm=m.writeIndex++; // record the index of the embedded command length
          if(sp.Op instanceof Gen2.ReadData)
          {
            msgAddGEN2DataRead(m, readTimeout, 0, ((Gen2.ReadData)sp.Op).Bank, ((Gen2.ReadData)sp.Op).WordAddress, ((Gen2.ReadData)sp.Op).Len);
          }
          else if(sp.Op instanceof Gen2.WriteData)
          {
            msgAddGEN2DataWrite(m, readTimeout, ((Gen2.WriteData)sp.Op).Bank, ((Gen2.WriteData)sp.Op).WordAddress);
          }
          else if(sp.Op instanceof Gen2.Lock)
          {
            msgAddGEN2LockTag(m, readTimeout, ((Gen2.Lock)sp.Op).Action.mask, ((Gen2.Lock)sp.Op).Action.action,((Gen2.Lock)sp.Op).AccessPassword);
          }
          else if(sp.Op instanceof Gen2.Kill)
          {
            msgAddGEN2KillTag(m, readTimeout, ((Gen2.Kill)sp.Op).KillPassword);
          }
          else if(sp.Op instanceof Gen2.BlockWrite)
          {
            msgAddGEN2BlockWrite(m, readTimeout, ((Gen2.BlockWrite)sp.Op).Bank,((Gen2.BlockWrite)sp.Op).WordPtr,((Gen2.BlockWrite)sp.Op).WordCount,((Gen2.BlockWrite)sp.Op).Data, accessPassword, readFilter);
          }
          else if(sp.Op instanceof Gen2.BlockPermaLock)
          {
            msgAddGEN2BlockPermaLock(m, readTimeout, ((Gen2.BlockPermaLock)sp.Op).ReadLock,((Gen2.BlockPermaLock)sp.Op).Bank,((Gen2.BlockPermaLock)sp.Op).BlockPtr,((Gen2.BlockPermaLock)sp.Op).BlockRange,((Gen2.BlockPermaLock)sp.Op).Mask, accessPassword, readFilter);
          }
          else
          {
            throw new ReaderException("FAULT_INVALID_OPCODE");
          }
          m.data[tm]=(byte)(m.writeIndex-tm-2);
        }
        if(useStreaming)
        { 
          sendMessage(readTimeout, m);
          while (true)
          {
            try
            {
            receiveMessage(readTimeout, m);
            }
            catch (ReaderCodeException re)
            {
              if (re.getCode() == FAULT_NO_TAGS_FOUND)
               return;
            }

            byte flags = (byte)m.getu8();
            int responseTypeIndex = (((byte)flags&(byte)0x10)==(byte)0x10) ? 10:8;
            byte responseByte=m.data[responseTypeIndex];
            if (responseByte==0)
                break;

            TagReadData t = new TagReadData();
            int searchFlags = m.getu16();

            Set<TagMetadataFlag> metadataFlags = tagMetadataSet(m.getu16());
            m.readIndex += 1; // skip response type
            metadataFromMessage(t, m, metadataFlags);
            t.antenna = antennaPortReverseMap.get(t.antenna);
            int epcLen = m.getu16() / 8;
            t.tag = parseTag(m, epcLen, sp.protocol);
            t.readBase = baseTime;
            tagvec.add(t);
            
          }
        }
        else //non streaming
        {
          count = 0;
          m.data[tm]=(byte)(m.writeIndex-tm-2);
          try
          {
          sendTimeout(readTimeout, m);
          }
          catch (ReaderCodeException re)
          {
            if (re.getCode() == FAULT_NO_TAGS_FOUND)
              return;
          }
          numTags= m.getu8at(8);
          while (count < numTags)
          {
            TagReadData tr[];
            tr = cmdGetTagBuffer(allMeta, false, sp.protocol);
            for (TagReadData t : tr)
            {
              t.readBase = baseTime;
              t.antenna = antennaPortReverseMap.get(t.antenna);
              tagvec.add(t);
              count++;
            }
          }
        }
        now = System.currentTimeMillis();
     }
    /*deduplication*/
    HashMap<String,TagReadData> map=new HashMap<String,TagReadData>();
    List<TagReadData> tagReads=new Vector();
    String key;
    byte i=(byte)(((Boolean)paramGet("/reader/tagReadData/uniqueByAntenna")?0x10:0x00)+((Boolean)paramGet("/reader/tagReadData/uniqueByData")?0x01:0x00));

    for (TagReadData tag : tagvec)
    {
      switch(i)
      {
        case 0x00:
          key = tag.epcString();
          break;
        case 0x01:
          key = tag.epcString() +";"+ byteArrayToHexString(tag.data);
          break;
        case 0x10:
          key = tag.epcString()+";"+tag.getAntenna();
          break;
        default:
          key = tag.epcString()+";"+ tag.getAntenna() +";"+ byteArrayToHexString(tag.data);
          break;
        }

        if (!map.containsKey(key))
        {
          map.put(key, tag);

        }
        else  //see the tag again
        {
          map.get(key).readCount = map.get(key).getReadCount() + tag.getReadCount() ;
          if ((Boolean)paramGet("/reader/tagReadData/recordHighestRssi"))
          {
            if(tag.getRssi()> map.get(key).getRssi())
            {
              int tmp= map.get(key).getReadCount();
              map.put(key, tag);
              map.get(key).readCount=tmp;
            }
          }
        }
      }
      tagvec.clear();
      tagvec.addAll(map.values());
    }

  private int[] prepForSearch(SimpleReadPlan sp) throws ReaderException
  {
        int[] antennaList;
    if (sp.antennas.length == 0)
    {
      int[] portList = getConnectedAntennas();
      // We have a list of raw ports, and need to convert it into a
      // list of antennas, based on the current antenna map. We
      // clearly can't detect whether an antenna port is part of a
      // mono or bistatic configuration; we'll just look it up in
      // the map as a transmit port and use that.
      Vector<Integer> antennaVec = new Vector<Integer>();
      for (int txport : portList)
      {
        Integer a = antennaPortTransmitMap.get(txport);
        if (a != null)
          antennaVec.add(a);
      }
      // can't use Vector.toArray() here because we need int[], not Integer[]
      antennaList = new int[antennaVec.size()];
      for (int i = 0; i < antennaList.length; i++)
        antennaList[i] = antennaVec.elementAt(i);
    }
    else
    {
      antennaList = sp.antennas;
    }
    return antennaList;
  }

   public void writeTag(TagFilter oldID, TagData newID)
    throws ReaderException 
  {

    if (oldID != null)
    {
      throw new UnsupportedOperationException(
        "filtering while writing EPC is not supported by this embedded module");
    }
    else
    {
      checkConnection();
      checkRegion();
      checkOpAntenna();
      cmdWriteTagEpc(commandTimeout, newID.epcBytes(), false);
    }
  }

  // kill tag
  public void killTag(TagFilter target, TagAuthentication auth)
    throws ReaderException
  {
    TagProtocol protocol;

    protocol = (TagProtocol)paramGet("/reader/tagop/protocol");
    
    if (TagProtocol.GEN2 != protocol)
    {
      throw new UnsupportedOperationException("Tag killing only supported for Gen2");
    }

    if (auth == null)
    {
      throw new IllegalArgumentException("killTag requires tag authentication");
    }
    if (auth instanceof Gen2.Password)
    {
      Gen2.Password pw = (Gen2.Password)auth;
      checkConnection();
      checkRegion();
      checkOpAntenna();
      setProtocol(protocol);
      cmdKillTag(commandTimeout, pw.password, target);
    }
    else
    {
      throw new UnsupportedOperationException("Unsupported authentication " +
                                              auth.getClass().getName());
    }
  }

  public byte[] readTagMemBytes(TagFilter target,
                                int bank, int address, int count)
    throws ReaderException
  {
    TagProtocol protocol;
    TagReadData tr;
    byte[] bytes;

    checkConnection();
    checkRegion();
    checkOpAntenna();
    protocol = (TagProtocol)paramGet("/reader/tagop/protocol");
    setProtocol(protocol);

    if (TagProtocol.GEN2 == protocol)
    {
      // gen2 devices address and read in words - round address down and
      // length up if necessary
      int wordCount, wordAddress;
      wordAddress = address / 2;
      wordCount = (count + 1 + (address % 2) ) / 2;

      tr = cmdGen2ReadTagData(commandTimeout, TagMetadataFlag.emptyMetadata,
                              Gen2.Bank.getBank(bank), 
                              wordAddress, wordCount,
                              accessPassword, target);

      bytes = tr.data;

      if ((wordAddress * 2 == address) && (wordCount * 2 == count))
        return bytes;
      else
      {
        byte[] adjustBytes = new byte[count];
        System.arraycopy(bytes, address % 2, adjustBytes, 0, count);
        return adjustBytes;
      }
    }
    else if (TagProtocol.ISO180006B == protocol)
    {
      byte[] result = new byte[count];
      int offset = 0;

      while (count > 0)
      {
        int readSize = 8;
        if (readSize > count)
        {
          readSize = count;
        }
        byte[] readData = 
          cmdIso180006bReadTagData(commandTimeout, address, readSize, target);
        System.arraycopy(readData, 0, result, offset, readSize);
        count -= readSize;
        offset += readSize;
      }
      return result;
    }
    else
    {
      throw new UnsupportedOperationException("Protocol " + protocol + 
                                              " not supported for data reading");
    }
  }

  public short[] readTagMemWords(TagFilter target, 
                                 int bank, int address, int count)
    throws ReaderException
  {
    TagReadData tr;
    byte[] bytes;
    short[] words;

    bytes = readTagMemBytes(target, bank, 2 * address, 2 * count);
    words = new short[bytes.length / 2];
    for (int i = 0; i < words.length; i++)
      words[i] = (short)((bytes[2*i] << 8) | (bytes[2*i + 1] & 0xff));

    return words;
  }

  public void writeTagMemBytes(TagFilter target,
                               int bank, int address, byte[] data)
    throws ReaderException
  {
    TagProtocol protocol;

    checkConnection();
    checkRegion();
    checkOpAntenna();
    protocol = (TagProtocol)paramGet("/reader/tagop/protocol");
    setProtocol(protocol);
    if (TagProtocol.GEN2 == protocol)
    {
      // Unlike a read operation, this pretty much has to have even parameters.
      if ((address % 2) != 0)
      {
        throw new IllegalArgumentException("Byte write address must be even");
      }
      if ((data.length % 2) != 0)
      {
        throw new IllegalArgumentException("Byte write length must be even");
      }

      switch ((Gen2.WriteMode)paramGet("/reader/gen2/writeMode"))
                {
                    case WORD_ONLY:
                        {
                            cmdGen2WriteTagData(commandTimeout, Gen2.Bank.getBank(bank), address / 2,
                          data, accessPassword, target);
                            break;
                        }
                    case BLOCK_ONLY:
                        {
                            blockWrite(target, Gen2.Bank.getBank(bank),address ,(byte) (data.length/2), data);
                            break;

                        }
                    case BLOCK_FALLBACK:
                        {
                            try
                            {
                                blockWrite(target, Gen2.Bank.getBank(bank),address ,(byte) (data.length/2), data);
                            }
                            catch (ReaderCodeException e)
                            {
                                if((e.getCode()==0x406))
                                {
                                  cmdGen2WriteTagData(commandTimeout, Gen2.Bank.getBank(bank), address / 2,
                                    data, accessPassword, target);
                                }
                                else
                                {
                                    throw e;
                                }
                            }
                            break;
                        }
                    default: break;
                }


    
    }
    else if (TagProtocol.ISO180006B == protocol)
    {
      cmdIso180006bWriteTagData(commandTimeout, address, data, target);
    }
    else
    {
      throw new UnsupportedOperationException("Protocol " + protocol + 
                                              " not supported for data writing");
    }
  }

  public void writeTagMemWords(TagFilter target,
                               int bank, int address, short[] data)
    throws ReaderException
  {
    byte[] bytes;

    bytes = new byte[data.length * 2];
    for (int i = 0; i < data.length; i++)
    {
      bytes[i * 2]     = (byte)((data[i] >> 8) & 0xff);
      bytes[i * 2 + 1] = (byte)((data[i] >> 0) & 0xff);
    }

    writeTagMemBytes(target, bank, 2 * address, bytes);
  }

  public void lockTag(TagFilter target, TagLockAction lock)
    throws ReaderException
  {
    TagProtocol protocol;
    
    checkConnection();
    checkRegion();
    checkOpAntenna();
    protocol = (TagProtocol)paramGet("/reader/tagop/protocol");
    setProtocol(protocol);

    if (TagProtocol.GEN2 == protocol)
    {
      if (!(lock instanceof Gen2.LockAction))
      {
        throw new UnsupportedOperationException("Unsupported lock action " + 
                                                lock.getClass().getName());
      }

      Gen2.LockAction g2l = (Gen2.LockAction)lock;
      cmdGen2LockTag(commandTimeout, g2l.mask, g2l.action, accessPassword,
                     target);
    }
    else if (TagProtocol.ISO180006B == protocol)
    {
      if (!(lock instanceof Iso180006b.LockAction))
      {
        throw new UnsupportedOperationException("Unsupported lock action " + 
                                                lock.getClass().getName());
      }

      Iso180006b.LockAction i18kl = (Iso180006b.LockAction)lock;
      cmdIso180006bLockTag(commandTimeout, i18kl.address, target);
    }
    else
    {
      throw new UnsupportedOperationException("Protocol " + protocol + 
                                              " not supported for tag locking");
    }
  }

   public void blockWrite(TagFilter target, Gen2.Bank bank, int wordPtr, byte wordCount, byte[] data)
   throws ReaderException
   {
      int timeout=(Integer)paramGet("/reader/commandTimeout");
      Gen2.Bank bankobj = bank;
      Gen2.Password pwobj = (Gen2.Password)paramGet("/reader/gen2/accessPassword");
      int password = 0x0000;
      //int password = pwobj.password;
      cmdBlockWrite(timeout,bankobj, wordPtr, wordCount,  data, password, target);

   }

   public byte[] blockPermaLock(TagFilter target, byte readLock, Gen2.Bank bank, int blockPtr, byte blockRange, byte[] mask)
   throws ReaderException
   {
      int timeout=(Integer)paramGet("/reader/commandTimeout");
      Gen2.Bank bankobj = bank;
      Gen2.Password pwobj = (Gen2.Password)paramGet("/reader/gen2/accessPassword");
      //int password = pwobj.password;
      int password=0x0000;
      return (cmdBlockPermaLock(timeout, readLock,bankobj, blockPtr, blockRange, mask, password, target));

   }

      public void cmdBlockWrite(int timeout,Gen2.Bank memBank, int wordPtr, byte wordCount, byte[] data, int accessPassword, TagFilter target)
      throws ReaderException
      {
            Message m=new Message();
            int optByte;
             m.setu8(MSG_OPCODE_WRITE_TAG_SPECIFIC); //opcode
            m.setu16(timeout);
            m.setu8(0x00);//chip type
            optByte = m.writeIndex++;
            m.setu8(0x00);//block write opcode
            m.setu8(0xC7);//block write opcode
            filterBytes(TagProtocol.GEN2, m,optByte, target,accessPassword, true);
            m.data[optByte] = (byte)(0x40|(m.data[optByte]));//option
            m.setu8(0x00);//Write Flags
            m.setu8(memBank.rep);
            m.setu32(wordPtr);
            m.setu8(wordCount);
            m.setbytes(data,0,data.length);
            send(m);
        }

      public byte[] cmdBlockPermaLock(int timeout,byte readLock, Gen2.Bank memBank, int blockPtr, byte blockRange,byte[] mask, int accessPassword, TagFilter target)
      throws ReaderException
      {
            Message m=new Message();
            int optByte;
            m.setu8(MSG_OPCODE_ERASE_BLOCK_TAG_SPECIFIC);
            m.setu16(timeout);
            m.setu8(0x00);//chip type
            optByte = m.writeIndex++;
            m.setu8(0x01);
            filterBytes(TagProtocol.GEN2, m,optByte, target,accessPassword, true);
            m.data[optByte] = (byte)(0x40|(m.data[optByte]));//option
            m.setu8(0x00);//RFU
            m.setu8(readLock);
            m.setu8(memBank.rep);
            m.setu32(blockPtr);
            m.setu8(blockRange);
            if (readLock==0x01)
            {
              m.setbytes(mask);
            }
            Message msg=send(m);
            if(readLock==0)
            {
                byte[] returnData=new byte[(msg.data[1]-2)];
                System.arraycopy(msg.data, 7, returnData, 0, (msg.data[1]-2));
                return returnData;
            }
            else
                return null;
        }


  public GpioPin[] gpiGet()
    throws ReaderException
  {
    boolean[] rawGpis;
    GpioPin[] state;

    checkConnection();

    rawGpis = cmdGetGPIO();
    state = new GpioPin[rawGpis.length];
    for (int i = 0 ; i < rawGpis.length; i++)
      state[i] = new GpioPin(i + 1, rawGpis[i]);
    return state;
  }

  public void gpoSet(GpioPin[] state)
    throws ReaderException
  {

    checkConnection();

    for (GpioPin gp : state)
      cmdSetGPIO(gp.id, gp.high);
  }

  int readInt(InputStream fwStr)
    throws IOException
  {
    byte[] intbuf = new byte[4];
    int r, ret;

    r = 0;
    while (r < 4)
    {
      ret = fwStr.read(intbuf, r, 4 - r);
      r += ret;
    }

    return
        ((intbuf[0] & 0xff) << 24)
      | ((intbuf[1] & 0xff) << 16)
      | ((intbuf[2] & 0xff) << 8)
      | (intbuf[3] & 0xff);
    
  }

  public synchronized void firmwareLoad(InputStream fwStr)
    throws ReaderException, IOException
  {
    int header1, header2;
    int sector, len, address, ret;
    byte[] buf;

    checkConnection();

    header1 = readInt(fwStr);
    header2 = readInt(fwStr);

    // Check the magic numbers for correct magic
    if((header1 != 0x544D2D53) || (header2 != 0x5061696B))
    {
      throw new IllegalArgumentException("Stream does not contain reader firmware");
    }

    sector = readInt(fwStr);
    len = readInt(fwStr);

    if (sector != 2)
    {
      throw new IllegalArgumentException("Only application firmware can be loaded");
    }
    
    // Move the reader into the bootloader so flash operations work
    // XXX drop down to 9600 before going into the bootloader. Some
    // XXX versions of M5e preserve the baud rate, and some go back to 9600;
    // XXX this way we know what the speed is either way.
    cmdSetBaudRate(9600);
    st.setBaudRate(9600);
    try
    {
      cmdBootBootloader();
    }
    catch (ReaderCodeException ex)
    {
      // Invalid Opcode (101h) okay -- means "already in bootloader"
      if (0x101 != ex.getCode())
      {
        // Other errors are real
        throw ex;
      }
    }

    // Wait a moment for the bootloader to come back up. This seems to
    // take longer on M5e firmware versions that reset themselves
    // more thoroughly.
    try
    {
      Thread.sleep(200);
    }
    catch (InterruptedException ie)
    {
    }

    cmdSetBaudRate(115200);
    st.setBaudRate(115200);

    cmdEraseFlash(2, 0x08959121);

    address = 0;
    buf = new byte[240];
    while (len > 0)
    {
      ret = fwStr.read(buf, 0, 240);
      if (ret == -1)
      {
        throw new IllegalArgumentException(
          "Stream did not contain full length of firmware");
      }
      cmdWriteFlash(2, address, 0x02254410, ret, buf, 0);
      address += ret;
      len -= ret;
    }
    
    // Boot the application and re-initialize parameters that may be changed
    // by the new firmware
    boot(region);
  }

  void filterBytes(TagProtocol t, Message m, int optIndex, TagFilter target,
                   int password, boolean usePassword)

  {
    if (TagProtocol.GEN2 == t)
    {
      filterBytesGen2(m, optIndex, target, password, usePassword);
    }
    else if (TagProtocol.ISO180006B == t)
    {
      filterBytesIso180006b(m, optIndex, target);
    }
  }

  void filterBytesGen2(Message m, int optIndex, TagFilter target,
                       int password, boolean usePassword)
  {
    if (target == null)
    {
      m.data[optIndex] = 0;
      return;
    }

    if (usePassword)
    {
      m.data[optIndex] = 0x05; // Password only
      m.setu32(password);
    }

    if (target instanceof TagData)
    {
      TagData t = (TagData)target;

      m.data[optIndex] = 1;
      m.setu8(t.epc.length * 8);
      m.setbytes(t.epc);
    }
    else if (target instanceof Gen2.Select)
    {
      Gen2.Select s = (Gen2.Select)target;

      if (s.bank == Gen2.Bank.EPC)
        m.data[optIndex] = 0x04;
      else
        m.data[optIndex] = (byte)s.bank.rep;

      if (s.invert == true)
        m.data[optIndex] |= 0x08;

      m.setu32(s.bitPointer);
      if (s.bitLength > 255)
      {
        m.data[optIndex] |= 0x20;
        m.setu16(s.bitLength);
      }
      else
      {
        m.setu8(s.bitLength);
      }
      m.setbytes(s.mask);
    }
    else
    {
      throw new UnsupportedOperationException("Unknown select type " + 
                                              target.getClass().getName());
    }
  }

  void filterBytesIso180006b(Message m, int optIndex, TagFilter target)
  {

    if (optIndex != -1)
    {
      m.data[optIndex] = 1;
    }

    if (null == target)
    {
      // Set up a match-anything filter, since it isn't the default.
      m.setu8(ISO180006B_SELECT_OP_EQUALS);
      m.setu8(0);  // address
      m.setu8(0);  // mask - don't compare anything
      m.setu32(0); // dummy tag ID bytes 0-3, not compared
      m.setu32(0); // dummy tag ID bytes 4-7, not compared
    }
    else if (target instanceof Iso180006b.Select)
    {
      Iso180006b.Select sel = (Iso180006b.Select)target;
      
      if (false == sel.invert)
      {
        m.setu8(sel.op.rep);
      }
      else
      {
        m.setu8(sel.op.rep | ISO180006B_SELECT_OP_INVERT);
      }
      m.setu8(sel.address);
      m.setu8(sel.mask);
      m.setbytes(sel.data);
    }
    else if (target instanceof TagData)
    {
      TagData t = (TagData)target;

      if (t.epc.length > 8)
      {
        throw new IllegalArgumentException("Can't select on more than 8 bytes");
      }

      // Convert the byte count to a MSB-based bit mask
      int mask = (0xff00 >> t.epc.length) & 0xff;

      m.setu8(ISO180006B_SELECT_OP_EQUALS);
      m.setu8(0); // Address - EPC is at the start of memory
      m.setu8(mask);
      m.setbytes(t.epc);
      // Pad EPC data to 8 bytes
      for (int i = t.epc.length ; i < 8 ; i++)
      {
        m.setu8(0);
      }
    }
    else
    {
      throw new UnsupportedOperationException("Unknown select type " + 
                                              target.getClass().getName());
    }

  }

   public static String byteArrayToHexString(byte in[])
  {

    byte ch = 0x00;

    int i = 0;

    if (in == null || in.length <= 0)

        return null;



    String pseudo[] = {"0", "1", "2",
    "3", "4", "5", "6", "7", "8",
    "9", "A", "B", "C", "D", "E",
    "F"};

    StringBuffer out = new StringBuffer(in.length * 2);

    while (i < in.length) {

        ch = (byte) (in[i] & 0xF0); // Strip off high nibble

        ch = (byte) (ch >>> 4);
        // shift the bits down

        ch = (byte) (ch & 0x0F);
        // must do this is high order bit is on!

        out.append(pseudo[ (int) ch]); // convert the nibble to a String Character

        ch = (byte) (in[i] & 0x0F); // Strip off low nibble

        out.append(pseudo[ (int) ch]); // convert the nibble to a String Character

        i++;

    }

    String rslt = new String(out);

    return rslt;

}
   /** execute a TagOp */
   public Object executeTagOp(TagOp tagOP,TagFilter target) throws ReaderException
   {
            if (tagOP instanceof Gen2.Kill)
            {
                killTag(target, new Gen2.Password(((Gen2.Kill)tagOP).KillPassword));
                return null;
            }
            else if ( tagOP instanceof Gen2.Lock)
            {
                lockTag(target, new Gen2.LockAction(((Gen2.Lock)tagOP).Action));
                return null;
            }
            else if (tagOP instanceof Gen2.WriteTag)
            {
                writeTag(target, ((Gen2.WriteTag)tagOP).Epc);
                return null;
            }
            else if (tagOP instanceof Gen2.ReadData)
            {
                return readTagMemWords(target, ((Gen2.ReadData)tagOP).Bank.rep, ((Gen2.ReadData)tagOP).WordAddress, ((Gen2.ReadData)tagOP).Len);
            }
           
            else if (tagOP instanceof Gen2.WriteData)
            {
                writeTagMemWords(target, ((Gen2.WriteData)tagOP).Bank.rep, ((Gen2.WriteData)tagOP).WordAddress,((Gen2.WriteData)tagOP).Data) ;
                return null;
            }
            else if (tagOP instanceof Gen2.BlockWrite)
            {
                blockWrite(target, ((Gen2.BlockWrite)tagOP).Bank, ((Gen2.BlockWrite)tagOP).WordPtr, ((Gen2.BlockWrite)tagOP).WordCount, ((Gen2.BlockWrite)tagOP).Data);
                return null;
            }
            else if (tagOP instanceof Gen2.BlockPermaLock)
            {
                return blockPermaLock(target, ((Gen2.BlockPermaLock)tagOP).ReadLock, ((Gen2.BlockPermaLock)tagOP).Bank, ((Gen2.BlockPermaLock)tagOP).BlockPtr, ((Gen2.BlockPermaLock)tagOP).BlockRange, ((Gen2.BlockPermaLock)tagOP).Mask);
            }
            else
            {
                return null;
            }
   }
   
   public void cmdMultiProtocolSearch(int opcode,HashMap<TagProtocol,TagFilter> protocolFilterPair, TagMetadataFlag metadataFlags, int antennas, int timeout,List<TagReadData> collectedTags)
           throws ReaderException
   {
       Message m=new Message();
       m.setu8(MSG_OPCODE_MULTI_PROTOCOL_TAG_OP);
       m.setu16(timeout);
       m.setu8(0x11);
       int metadataBits = allMetaBits;
       m.setu16(metadataBits);
       m.setu8(opcode);
       m.setu16(0x0000);
       
       TagProtocol tagProtocol=TagProtocol.NONE;
      
       byte[] data;

       short subTimeout =(short) (timeout / protocolFilterPair.keySet().size());
       for(TagProtocol protocol :protocolFilterPair.keySet())
       {
         if(protocol.equals(TagProtocol.GEN2))
         {
         m.setu8(PROT_GEN2);
         } 
         else if(protocol.equals(TagProtocol.ISO180006B))
         {
         m.setu8(PROT_ISO180006B);
         }
         else if(protocol.equals(TagProtocol.IPX64))
         {
         m.setu8(PROT_IPX64);
         }
         else if(protocol.equals(TagProtocol.IPX256))
         {
         m.setu8(PROT_IPX256);
         }
         else if(protocol.equals(TagProtocol.ISO180006B_UCODE))
         {
         m.setu8(PROT_UCODE);
         }
         int subLen=m.writeIndex++;
         switch(opcode)
         {
           case MSG_OPCODE_READ_TAG_ID_SINGLE:
           {
             data=msgSetupReadTagSingle(metadataFlags, protocolFilterPair.get(protocol), protocol, (short)subTimeout);
             break;
           }
           case MSG_OPCODE_READ_TAG_ID_MULTIPLE:
           {
             msgSetupReadTagMultiple(m,subTimeout, antennas, protocolFilterPair.get(protocol), protocol, metadataFlags,0);
             break; 
           }
           default:
             throw new ReaderException("Operation not supported"+opcode);
         }
         m.data[subLen]=(byte)(m.writeIndex-subLen-2);
         
       }
       
       Message response;
       int tagsFound;
       if(opcode==MSG_OPCODE_READ_TAG_ID_SINGLE)
       {
           response=sendTimeout(timeout,m);
           tagsFound=response.getu8at(9);
           m.readIndex=13; // the start of the tag responses
           for (int i =0;i<m.readIndex;i++)
           {
               int subBegin=m.readIndex;
               if(m.readIndex==response.data.length-2) // reach the crc
               {
                   break;
               }
               int subResponseLen=response.data[m.readIndex+1];
               m.readIndex=m.readIndex+4+3; // points to the start of metadata , ignore option and metaflags for read tag single
               TagReadData read=new TagReadData();
               metadataFromMessage(read, m, tagMetadataFlagValues.keySet());
               read.antenna = antennaPortReverseMap.get(read.antenna);
               int crcLen=2;
               int epcLen = subResponseLen+4-(m.readIndex-subBegin)-crcLen;
               read.tag = parseTag(m, epcLen, tagProtocol);
               byte epc[]=new byte[epcLen];
               byte crc[] = new byte[crcLen];
               System.arraycopy(response.data,m.readIndex,epc,0,epcLen);
               System.arraycopy(response.data,m.readIndex+epcLen,crc,0,crcLen);
               TagData tag=null;
               switch(read.readProtocol)
               {
                   default:
                       tag=new TagData(epc,crc);
                       break;
                   case GEN2 :
                       tag=new Gen2.TagData(epc,crc);
                       break;
                   case ISO180006B:
                   case ISO180006B_UCODE:
                       tag=new Iso180006b.TagData(epc,crc);
                       break;
               }
               read.tag=tag;
               collectedTags.add(read);
               m.readIndex+=epcLen+crcLen;  // point to the next protocol tag response
           }
       }

       if(opcode==MSG_OPCODE_READ_TAG_ID_MULTIPLE)
       {
           if(useStreaming)
           {
               sendMessage(timeout, m);
               while(true)
               {
                   opCode=opcode;  // Change what receiveMessage expects to see
                   try
                   {
                   receiveMessage(timeout, m);
                   }
                   catch (ReaderCodeException re)
                   {
                     if (re.getCode() == FAULT_NO_TAGS_FOUND)
                       return;
                   }

                   if(m.data[2]==0x2F)
                   {
                       break;
                   }
                   TagReadData t=new TagReadData();
                   t.readBase=System.currentTimeMillis();
                   m.readIndex=5+6; //start of metadata
                   metadataFromMessage(t, m, tagMetadataFlagValues.keySet());
                   if(t.antenna!=0)
                   {
                       t.antenna = antennaPortReverseMap.get(t.antenna);
                   }
                   int epcLen = m.getu16() / 8;
                   t.tag = parseTag(m, epcLen, t.readProtocol);
                   collectedTags.add(t);
               }
           }

       
       else
       {
          int count=0;
          try
          {
          response=sendTimeout(timeout+transportTimeout, m);
          }
          catch (ReaderCodeException re)
          {
            if (re.getCode() == FAULT_NO_TAGS_FOUND)
              return;
          }
          int numTags= m.getu32at(9);
          while (count < numTags)
          {
            TagReadData tr[];
            tr = cmdGetTagBuffer(tagMetadataFlagValues.keySet(), false, tagProtocol);
            for (TagReadData t : tr)
            {
              t.readBase = System.currentTimeMillis();
              t.antenna = antennaPortReverseMap.get(t.antenna);
              collectedTags.add(t);
              count++;
            }
          }
       }
     }
   }





           
      
   
   public byte[] msgSetupReadTagSingle(TagMetadataFlag metadataFlags, TagFilter filter,TagProtocol protocol, short timeout)
   {
       Message m = new Message();
       int optByte;
       m.setu8(MSG_OPCODE_READ_TAG_ID_SINGLE);
       m.setu16(timeout);
       optByte=m.writeIndex++;
       filterBytes(protocol, m,optByte, filter,0, true);
       m.data[optByte] |= (byte)(SINGULATION_FLAG_METADATA_ENABLED );
       int metadatabits=tagMetadataFlagValues.get(metadataFlags);
       m.setu16(metadatabits);
       return m.data;
   }

   void openPort() throws ReaderException
   {

    int[] rates = {baudRate, 9600, 115200, 921600, 19200, 38400, 57600, 230400, 460800};
    boolean success=false;
    versionInfo = null;
    for (int rate : rates)
    {
        st.setBaudRate(rate);
        baudRate = rate;
        st.flush();
        try
        {
        versionInfo = cmdVersion();
        }
         catch (ReaderCommException re)
         {
          continue;
         }
         catch (ReaderException re)
         {
          st.shutdown();
        throw re; // A error response to a version command is bad news
      }
      success = true;
      break;
    }

    if (success == false)
    {
      st.shutdown();
      throw new ReaderCommException("No response from reader at any baud rate.");
    }
}


}

