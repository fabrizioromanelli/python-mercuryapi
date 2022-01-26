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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.ArrayList;

/**
 * This class is a namespace for Gen2-specific subclasses of generic
 * Mercury API classes, Gen2 data structures, constants, and Gen2
 * convenience functions.
 */
public class Gen2
{

  // non-public
  Gen2() { }

/**
 * This class extends {@link TagData} to represent the details of a Gen2 RFID tag.
 */
  public static class TagData extends com.thingmagic.TagData 
  {
    final byte[] pc;

    public TagData(byte[] bEPC)
    {
      super(bEPC);

      pc = new byte[2];
      pc[0] = (byte)((epc.length) << 3);
      pc[1] = 0;
    }

    public TagData(byte[] bEPC, byte[] newPC)
    {
      super(bEPC);

      pc = newPC.clone();
    }

    public TagData(byte[] bEPC, byte[] crc, byte[] newPC)
    {
      super(bEPC, crc);

      pc = newPC.clone();
    }

    public TagData(String sEPC)
    {
      super(sEPC);

      pc = new byte[2];
      pc[0] = (byte)((epc.length) << 3);
      pc[1] = 0;
    }

    public TagData(String sEPC, String sCrc)
    {
      super(sEPC,sCrc);

      pc = new byte[2];
      pc[0] = (byte)((epc.length) << 3);
      pc[1] = 0;
    }

    public TagProtocol getProtocol()
    {
      return TagProtocol.GEN2;
    }

    public byte[] pcBytes()
    {
      return pc.clone();
    }

    @Override
    boolean checkLen(int epcbytes)
    {
      if (epcbytes < 0)
      {
        return false;
      }
      if (epcbytes > 62)
      {
        return false;
      }
      if ((epcbytes & 1) != 0)
      {
        return false;
      }
    return true;
    }

    public String toString()
    {
      return String.format("GEN2:%s", epcString());
    }

  }

  /**
   * Gen2 memory banks
   */
  public enum Bank
  {
    RESERVED (0),
      EPC (1),
      TID (2),
      USER (3);
    
    int rep;
    Bank(int rep)
    {
      this.rep = rep;
    }

    public static Bank getBank(int v)
    {
      switch(v)
      {
      case 0: return RESERVED;
      case 1: return EPC;
      case 2: return TID;
      case 3: return USER;
      }
      throw new IllegalArgumentException("Invalid Gen2 bank " + v);
    }
  }

  /**
   * Gen2 session values
   */
  public enum Session
  {
    /**
     * Session 0
     */
    S0 (0),
      /**
       * Session 1
       */
      S1 (1),
      /**
       * Session 2
       */
      S2 (2),
      /**
       * Session 3
       */
      S3 (3);

    int rep; /* Gen2 air protocol representation of the value */
    Session(int rep)
    {
      this.rep = rep;
    }
    
  }
  
  /**
   * Target algorithm options
   */
  public enum Target
  {
    /**
     * Search target A
     */
    A,
    /**
     * Search target B
     */
      B,
    /**
     * Search target A until exhausted, then search target B
     */
      AB,
    /**
     * Search target B until exhausted, then search target A
     */
      BA;
  }

  /**
   * Miller M values
   */
  public enum TagEncoding
  {
     /**
      * FM0
      */
    FM0(0),
    /**
     * M=2
     */
    M2 (1),
      /**
       * M=4
       */
      M4 (2),
      /**
       * M=8
       */
      M8 (3);

    int rep; /* Gen2 air protocol representation of the value */
    TagEncoding(int rep)
    {
      this.rep = rep;
    }
  }

  /**
   * Divide Ratio values
   */
  public enum DivideRatio
  {
    /** 8 */
    DR8 (0),
      /** 64/3 */
      DR64_3 (1);

    int rep; /* Gen2 air protocol representation of the value */
    DivideRatio(int rep)
    {
      this.rep = rep;
    }
  }

  /**
   * TRext
   */ 
  public enum TrExt
  {
    NOPILOTTONE (0),
      PILOTTONE (1);

    int rep; /* Gen2 air protocol representation of the value */
    TrExt(int rep)
    {
      this.rep = rep;
    }
  }
  /**
   * Mode for write operation
   */
  public enum WriteMode
  {
    /* use standard write only */
    WORD_ONLY,
    /* use block write only */
    BLOCK_ONLY,
    /* use BlockWrite first, if fail, use standard write */
    BLOCK_FALLBACK;

  }

  /**
   * Algorithm choices for Q - superclass
   */
  public abstract static class Q
  {
  }

  /**
   * Dynamic Q algorithm
   */
  public static class DynamicQ extends Q
  {
    @Override
    public String toString()
    {
      return String.format("DynamicQ");
    }
  }

  /**
   * Static initial Q algorithm
   */
  public static class StaticQ extends Q
  {
    /**
     * The initial Q value to use
     */
    public int initialQ;

    /**
     * Create a static Q algorithim instance with a particular value.
     * 
     * @param initialQ the q value
     */
    public StaticQ(int initialQ)
    {
      this.initialQ = initialQ;
    }

    @Override
    public String toString()
    {
      return String.format("StaticQ(%d)", initialQ);
    }
  }

  public enum LinkFrequency
  {
    LINK40KHZ (40),
    LINK250KHZ (250),
    LINK400KHZ (400),
    LINK640KHZ (640);
    int rep;
    LinkFrequency(int rep)
    {
        this.rep=rep;
    }
  }

  public enum Tari
  {
    TARI_25US,
      TARI_12_5US,
      TARI_6_25US;
  }

  /**
   * Representation of a Gen2 Select operation
   */
  public static class Select implements TagFilter
  {
    /**
     * Whether to invert the selection (deselect tags that meet the
     * comparison and vice versa).
     */
    public boolean invert;

    /**
     * The memory bank in which to compare the mask
     */
    public Bank bank;
    
    /**
     * The location (in bits) at which to begin comparing the mask
     */
    public int bitPointer;
    /**
     * The length (in bits) of the mask
     */
    public int bitLength;
    /**
     * The mask value to compare with the specified region of tag
     * memory, MSB first
     */
    public byte[] mask; 

    public Select(boolean invert, Bank bank, int bitPointer,
                  int bitLength, byte[] mask)
    {
      this.invert = invert;
      if (bank == Bank.RESERVED)
      {
        throw new IllegalArgumentException("Gen2 Select may not operate on reserved memory bank");
      }
      this.bank = bank;
      this.bitPointer = bitPointer;
      this.bitLength = bitLength;
      this.mask =(mask==null) ? null: mask.clone();
    }

    public boolean matches(com.thingmagic.TagData t)
    {
      boolean match = true;
      int i, bitAddr;

      if (bank != Bank.EPC)
      {
        throw new UnsupportedOperationException(
          "Can't match against non-EPC memory");
      }

      i = 0;
      bitAddr = bitPointer;
      // Matching against the CRC and PC does not have defined
      // behavior; see section 6.3.2.11.1.1 of Gen2 version 1.2.0.
      // We choose to let it match, because that's simple.
      bitAddr -= 32;
      if (bitAddr < 0)
      {
        i -= bitAddr;
        bitAddr = 0;
      }

      for (; i < bitLength; i++, bitAddr++)
      {
        if (bitAddr > (t.epc.length*8))
        {
          match = false;
          break;
        }
        // Extract the relevant bit from both the EPC and the mask.
        if (((t.epc[bitAddr/8] >> (7-(bitAddr&7)))&1) !=
            ((mask[i/8] >> (7-(i&7)))&1))
        {
          match = false;
          break;
        }
      }
      if (invert)
      {
        match = match ? false : true;
      }
      return match;
    }

    @Override
    public String toString()
    {
      StringBuilder maskHex = new StringBuilder(mask.length * 2);

      for (byte b : mask)
      {
        maskHex.append(String.format("%02X", b));
      }
      
      return String.format("Gen2.Select:[%s%s,%d,%d,%s]",
                           invert ? "Invert," : "",
                           bank, bitPointer, bitLength, 
                           maskHex.toString());
    }

  }

  /**
   * Individual Gen2 lock bits.
   */
  public class LockBits
  {
      public static final int
      USER_PERM    = 1 << 0,
      USER         = 1 << 1,
      TID_PERM     = 1 << 2,
      TID          = 1 << 3,
      EPC_PERM     = 1 << 4,
      EPC          = 1 << 5,
      ACCESS_PERM  = 1 << 6,
      ACCESS       = 1 << 7,
      KILL_PERM    = 1 << 8,
      KILL         = 1 << 9;
  }

  /**
   * The arguments to a Reader.lockTag() method for Gen2 tags. 
   * Represents the "action" and "mask" bits.
   */
  public static class LockAction
    extends TagLockAction
  {

    /**
     * Lock Kill Password (readable and writable when access password supplied)
     */
    public static final LockAction KILL_LOCK = 
      new LockAction(LockBits.KILL | LockBits.KILL_PERM,
                     LockBits.KILL);
    /**
     * Unlock Kill Password (always readable and writable)
     */
    public static final LockAction KILL_UNLOCK = 
      new LockAction(LockBits.KILL | LockBits.KILL_PERM,
                     0);
    /**
     * Permanently Lock Kill Password (never readable or writable again)
     */
    public static final LockAction KILL_PERMALOCK = 
      new LockAction(LockBits.KILL | LockBits.KILL_PERM,
                     LockBits.KILL | LockBits.KILL_PERM);
    /**
     * Permanently Unlock Kill Password (always readable and writable, may never be locked again)
     */
    public static final LockAction KILL_PERMAUNLOCK = 
      new LockAction(LockBits.KILL | LockBits.KILL_PERM,
                     LockBits.KILL_PERM);

    /**
     * Lock Access Password (readable and writable when access password supplied)
     */
    public static final LockAction ACCESS_LOCK = 
      new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM,
                     LockBits.ACCESS);
    /**
     * Unlock Access Password (always readable and writable)
     */
    public static final LockAction ACCESS_UNLOCK = 
      new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM,
                     0);
    /**
     * Permanently Lock Access Password (never readable or writable again)
     */
    public static final LockAction ACCESS_PERMALOCK = 
      new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM,
                     LockBits.ACCESS | LockBits.ACCESS_PERM);
    /**
     * Permanently Unlock Access Password (always readable and writable, may never be locked again)
     */
    public static final LockAction ACCESS_PERMAUNLOCK = 
      new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM,
                     LockBits.ACCESS_PERM);

    /**
     * Lock EPC Memory (always readable, writable when access password supplied)
     */
    public static final LockAction EPC_LOCK = 
      new LockAction(LockBits.EPC | LockBits.EPC_PERM,
                     LockBits.EPC);
    /**
     * Unlock EPC Memory (always readable and writable)
     */
    public static final LockAction EPC_UNLOCK = 
      new LockAction(LockBits.EPC | LockBits.EPC_PERM,
                     0);
    /**
     * Permanently Lock EPC Memory (always readable, never writable again)
     */
    public static final LockAction EPC_PERMALOCK = 
      new LockAction(LockBits.EPC | LockBits.EPC_PERM,
                     LockBits.EPC | LockBits.EPC_PERM);
    /**
     * Permanently Unlock EPC Memory (always readable and writable, may never be locked again)
     */
    public static final LockAction EPC_PERMAUNLOCK = 
      new LockAction(LockBits.EPC | LockBits.EPC_PERM,
                     LockBits.EPC_PERM);
    /**
     * Lock TID Memory (always readable, writable when access password supplied)
     */
    public static final LockAction TID_LOCK = 
      new LockAction(LockBits.TID | LockBits.TID_PERM,
                     LockBits.TID);
    /**
     * Unlock TID Memory (always readable and writable)
     */
    public static final LockAction TID_UNLOCK = 
      new LockAction(LockBits.TID | LockBits.TID_PERM,
                     0);
    /**
     * Permanently Lock TID Memory (always readable, never writable again)
     */
    public static final LockAction TID_PERMALOCK = 
      new LockAction(LockBits.TID | LockBits.TID_PERM,
                     LockBits.TID | LockBits.TID_PERM);
    /**
     * Permanently Unlock TID Memory (always readable and writable, may never be locked again)
     */
    public static final LockAction TID_PERMAUNLOCK = 
      new LockAction(LockBits.TID | LockBits.TID_PERM,
                     LockBits.TID_PERM);
    /**
     * Lock User Memory (always readable, writable when access password supplied)
     */
    public static final LockAction USER_LOCK = 
      new LockAction(LockBits.USER | LockBits.USER_PERM,
                     LockBits.USER);
    /**
     * Unlock User Memory (always readable and writable)
     */
    public static final LockAction USER_UNLOCK = 
      new LockAction(LockBits.USER | LockBits.USER_PERM,
                     0);
    /**
     * Permanently Lock User Memory (always readable, never writable again)
     */
    public static final LockAction USER_PERMALOCK = 
      new LockAction(LockBits.USER | LockBits.USER_PERM,
                     LockBits.USER | LockBits.USER_PERM);
    /**
     * Permanently Unlock User Memory (always readable and writable, may never be locked again)
     */
    public static final LockAction USER_PERMAUNLOCK = 
      new LockAction(LockBits.USER | LockBits.USER_PERM,
                     LockBits.USER_PERM);


    final short mask, action;

    /**
     * Construct a new LockAction from a combination of other LockActions.
     *
     * @param actions lock actions to combine. If a data field is
     * repeated, the last one takes precedence; e.g.,
     * Gen2.LockAction.USER_LOCK, Gen2.LockAction.USER_UNLOCK turns
     * into Gen2.LockAction.USER_UNLOCK.
     */
    public LockAction(LockAction... actions)
    {
      short _mask, _action;

      _mask = 0;
      _action = 0;

      for (LockAction la : actions)
      {
        // Union mask
        _mask |= la.mask;
        // Overwrite action
        _action &= ~la.mask;
        _action |= (la.action & la.mask);
      }

      this.mask = _mask;
      this.action = _action;
    }

    /**
     * Convert the string representation into a LockAction object.
     *
     * @param value A string containing the name to convert. May be
     * the name of one of the predefined constants, or a
     * comma-separated list of the names.
     */
    public static LockAction parse(String value)
    {
      List<LockAction> actions = new Vector<LockAction>();

      initNameToLockAction();

      for (String v : value.toUpperCase().split(","))
      {
        if (nameToLockAction.containsKey(v))
        {
          actions.add(nameToLockAction.get(v));
        }
        else
        {
          throw new IllegalArgumentException("Unknown Gen2.LockAction " + v);
        }
      }
      return new LockAction(actions.toArray(new LockAction[actions.size()]));
    }

    private static Map<String,LockAction> nameToLockAction;
    private static Set<Map.Entry<String,LockAction>> nameToLockActionEntries;

    private static void initNameToLockAction() 
    {
      if (nameToLockAction != null)
      {
        return;
      }

      nameToLockAction = new HashMap<String,LockAction>(20);
      nameToLockAction.put("KILL_LOCK", KILL_LOCK);
      nameToLockAction.put("KILL_UNLOCK", KILL_UNLOCK);
      nameToLockAction.put("KILL_PERMALOCK", KILL_PERMALOCK);
      nameToLockAction.put("KILL_PERMAUNLOCK", KILL_PERMAUNLOCK);

      nameToLockAction.put("ACCESS_LOCK", ACCESS_LOCK);
      nameToLockAction.put("ACCESS_UNLOCK", ACCESS_UNLOCK);
      nameToLockAction.put("ACCESS_PERMALOCK", ACCESS_PERMALOCK);
      nameToLockAction.put("ACCESS_PERMAUNLOCK", ACCESS_PERMAUNLOCK);

      nameToLockAction.put("EPC_LOCK", EPC_LOCK);
      nameToLockAction.put("EPC_UNLOCK", EPC_UNLOCK);
      nameToLockAction.put("EPC_PERMALOCK", EPC_PERMALOCK);
      nameToLockAction.put("EPC_PERMAUNLOCK", EPC_PERMAUNLOCK);

      nameToLockAction.put("TID_LOCK", TID_LOCK);
      nameToLockAction.put("TID_UNLOCK", TID_UNLOCK);
      nameToLockAction.put("TID_PERMALOCK", TID_PERMALOCK);
      nameToLockAction.put("TID_PERMAUNLOCK", TID_PERMAUNLOCK);

      nameToLockAction.put("USER_LOCK", USER_LOCK);
      nameToLockAction.put("USER_UNLOCK", USER_UNLOCK);
      nameToLockAction.put("USER_PERMALOCK", USER_PERMALOCK);
      nameToLockAction.put("USER_PERMAUNLOCK", USER_PERMAUNLOCK);

      nameToLockActionEntries = nameToLockAction.entrySet();
    }

    /**
     * Construct a new LockAction from an explicit Gen2 action and mask
     * integer values. Only the bottom ten bits of each value are used.
     *
     * @param mask bitmask of which bits to set
     * @param action the bit values to set
     */
    public LockAction(int mask, int action)
    {
      this.mask = (short)(mask & 0x03ff);
      this.action = (short)(action & 0x03ff);
    }


    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      boolean next = false;

      initNameToLockAction();

      for (Map.Entry<String,LockAction> kv : nameToLockActionEntries)
      {
        String name = kv.getKey();
        LockAction value = kv.getValue();
        
        if (((mask & value.mask) == value.mask) &&
            ((action & value.mask) == value.action))
        {
          if (next)
          {
            sb.append(",");
          }
          sb.append(name);
          next = true;
        }
      }
      return sb.toString();
    }
    
  }

  /**
   * Stores a 32-bit Gen2 password for use as an access or kill password.
   */
  public static class Password extends TagAuthentication
  {
    int password;

    /**
     * Create a new Password object.
     */ 
    public Password(int password)
    {
      this.password = password;
    }
  }
  /** Embedded Tag operation : Write Data */
  public static class WriteData extends TagOp
  {
      /** Gen2  memory bank to write to */
      public Gen2.Bank Bank;

      /** word address to start writing at */
      public int WordAddress;

      /** Data to write */
      public short[] Data;
      /** Constructor to initialize the parameter of Write Data
       *
       * @param bank Memory Bank to Write
       * @param wordAddress Write starting address
       * @param data the data to write
       */
      public WriteData( Gen2.Bank bank,int wordAddress, short[] data)
      {
       this.Bank=bank;
       this.WordAddress=wordAddress;
       this.Data=data;
      }

  }

   /** Embedded Tag operation : Read Data */
  public static class ReadData extends TagOp
  {
      /** Gen2  memory bank to write to */
      public Gen2.Bank Bank;

      /** word address to start writing at */
      public int WordAddress;

      /** Number of words to read */
      public int Len;
      /** Constructor to initialize the parameter of Read Data
       *
       * @param bank Memory Bank to read
       * @param wordAddress read starting address
       * @param length the lenght of data to read
       */
      public ReadData( Gen2.Bank bank,int wordAddress, byte length)
      {
       this.Bank=bank;
       this.WordAddress=wordAddress;
       this.Len=length;
      }

  }

   /** Embedded Tag operation : Lock */
  public static class Lock extends TagOp
  {
      /** Access Password */
      public int AccessPassword;

     
      /** New values of each bit specified in the mask */
      public LockAction Action;
      /** Constructor to initialize parameters of lock
       *
       * @param accessPassword The access password
       * @param mask Bitmask indicating which lock bits to change
       * @param action The lock Action
       */
      public Lock( int accessPassword,LockAction action)
      {
       this.AccessPassword=accessPassword;
      
       this.Action=action;
      }

  }

   /** Embedded Tag operation : Kill */
  public static class Kill extends TagOp
  {
      /**  Kill Password to use to kill the tag */
      public int KillPassword;


      /** Constructor to initialize parameters of Kill
       *
       * @param accessPassword The access password
      */
      public Kill( int killPassword)
      {
       this.KillPassword=killPassword;
      }
    }

  
   /** Write a new id to a tag */
  public static class WriteTag extends TagOp
  {
      /**  the new tag id to write  */
      public TagData Epc;


      /** Constructor to initialize parameters of WriteTag
       *
       * @param epc The tagid to write
       */
      public WriteTag( TagData epc)
      {
       this.Epc=epc;
      }
  }

 
   /** Block Write */
  public static class  BlockWrite extends TagOp
  {
      /**  the tag memory bank to write to  */
      public Gen2.Bank Bank;

      /** the word address to start writing to */
      public int WordPtr;

      /** the number words to write */
      public byte WordCount;

      /** the bytes to write */
      public byte[] Data;

      /** Constructor to initialize parameters of Block
       *
       * @param bank The tag memory bank to write to
       * @param wordPtr Word Address to start writing to
       * @param wordCount The length of the data to write in words
       * @param data  Data to write
       */
      public BlockWrite(Gen2.Bank bank, int wordPtr, byte wordCount , byte[] data)
      {
       this.Bank=bank;
       this.WordPtr=wordPtr;
       this.WordCount=wordCount;
       this.Data=data;
      }
    }

     /** Block PermaLock */
  public static class BlockPermaLock extends TagOp
  {
      /**  the tag memory bank to write to  */
      public Gen2.Bank Bank;
      /** Read or Lock ?? */
      public byte ReadLock;
      /** the starting word address to lock */
      public int BlockPtr;

      /** the number 16 blocks */
      public byte BlockRange;

      /** the Mask */
      public byte[] Mask;

      /** Constructor to initialize parameters of Block
       *
       * @param bank The tag memory bank to write to
       * @param blockPtr Starting Address of the blocks to operate
       * @param blockRange Number of 16 blocks
       * @param mask  The mask
       */
      public BlockPermaLock(Gen2.Bank bank,byte readLock, int blockPtr, byte blockRange , byte[] mask)
      {
       this.Bank=bank;
       this.ReadLock=readLock;
       this.BlockPtr=blockPtr;
       this.BlockRange=blockRange;
       this.Mask=mask;
      }
  }
}