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

/**
 * This class is a namespace for ISO 18000-6B-specific subclasses of
 * generic Mercury API classes, data structures, constants, and
 * convenience functions.
 */
public class Iso180006b
{

  // non-public, this class is never instantiated
  Iso180006b() {}


  public enum LinkFrequency
  {
      LINK40KHZ (0),
      LINK160KHZ (1);

    int rep;
    LinkFrequency(int rep)
    {
      this.rep = rep;
    }
  }

/**
 * This class extends {@link TagData} to represent the details of an 
 * ISO 18000-6B RFID tag.
 */
  public static class TagData extends com.thingmagic.TagData
  {

    @Override
    boolean checkLen(int epcbytes)
    {
      if (epcbytes != 8)
      {
        return false;
      }
      return true;
    }

    public TagProtocol getProtocol()
    {
      return TagProtocol.ISO180006B;
    }

    /**
     * Construct an ISO 18000-6B tag data from a byte array.
     *
     * @param bEPC EPC bytes. Must be 8 bytes
     */
    public TagData(byte[] bEPC)
    {
      super(bEPC);
    }

    /**
     * Construct an ISO 18000-6B tag data from a byte array.
     *
     * @param bEPC EPC bytes. Must be 8 bytes
     * @param crc CRC bytes
     */
    public TagData(byte[] bEPC, byte[] crc)
    {
      super(bEPC, crc);
    }

    /**
     * Construct an ISO 18000-6B tag data from a hexadecimal string.
     *
     * @param sEPC Hex string. Must be 8 bytes (16 hex digits)
     */
    public TagData(String sEPC)
    {
      super(sEPC);
    }

    /**
     * Construct an ISO 18000-6B tag data from a hexadecimal string.
     *
     * @param sEPC Hex string. Must be 8 bytes (16 hex digits)
     * @param sCRC Hex string. Must be 2 bytes (4 hex digits)
     */
    public TagData(String sEPC, String sCRC)
    {
      super(sEPC, sCRC);
    }

  }


  /**
   * The arguments to a Reader.lockTag() method for ISO18000-6B tags. 
   * Represents the address of a byte of memory to lock
   */
  public static class LockAction
    extends TagLockAction
  {
    int address;

    /**
     * Construct a new LockAction from an address.
     *
     * @param address the address of the byte to lock. Must be in the range 0-255
     */
    public LockAction(int address)
    {
      if (address < 0 || address > 255)
      {
        throw new IllegalArgumentException("ISO18000 memory address out of range");
      }
      this.address = address;
    }
  }

  /**
   * Operations that can be performed as part of a Group Select operation
   */
  public enum SelectOp
  {
        /** Select if tag data matches op data */
      EQUALS      (0),
        /** Select if tag data does not match op data */
      NOTEQUALS   (1),
        /** Select if tag data is less than op data */
      LESSTHAN    (2),
        /** Select if tag data is greater than op data */
      GREATERTHAN (3);

    int rep; /* air protocol representation of the value */
    SelectOp(int rep)
    {
      this.rep = rep;
    }

  }
  public static class Select implements TagFilter
  {

    /**
     * Whether to invert the selection (deselect tags that meet the
     * comparison and vice versa).
     */
    public boolean invert;

    /**
     * The operation to compare the tag data to the provided data.
     */
    SelectOp op;

    /**
     * The address of the tag data to compare the the provided data.
     */ 
    int address;

    /**
     * A bitmask of which of the eight provided bytes to compare.
     */
    byte mask;
    
    /**
     * The data to compare. Exactly eight bytes.
     */
    byte[] data;

    public Select(boolean invert, SelectOp op,
                  int address, byte mask, byte[] data)
    {
      this.invert = invert;
      this.op = op;
      this.address = address;
      this.mask = mask;
      this.data = data.clone();
      if (this.data.length != 8)
      {
        throw new IllegalArgumentException("ISO180006B select data must be 8 bytes");
      }
    }

    public boolean matches(com.thingmagic.TagData t)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString()
    {
      StringBuilder dataHex = new StringBuilder(data.length * 2);

      for (byte b : data)
      {
        dataHex.append(String.format("%02X", b));
      }
      
      return String.format("Iso180006b.Select:[%s%s,%d,%d,%s]",
                           invert ? "Invert," : "",
                           op, address, mask, 
                           dataHex.toString());
    }

  }
}
