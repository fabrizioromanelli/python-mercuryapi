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
package samples;
import com.thingmagic.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.TreeMap;
import java.util.EnumSet;
import java.util.Vector;
import java.util.List;
public class demo
{

  static void usage()
  {
    System.out.printf("Usage: demo reader-uri <command> [args]\n" +
                      "  (URI: 'tmr:///COM1' or 'tmr://astra-2100d3/' " +
                      "or 'tmr:///dev/ttyS0')\n\n" +
                      "Available commands:\n");
    for (String s : commandTable.keySet())
    {
      System.out.printf("  %s\n", s);
    }
    System.exit(1);
  }

  interface CommandFunc
  {
    void function(Reader r, String args[]) throws ReaderException;
  }

  static class Command
  {
    final CommandFunc f;
    final int minArgs;
    final String doc, usage;

    Command(String doc, String usage, int n, CommandFunc f)
    {
      this.f = f;
      this.minArgs = n;
      this.doc = doc;
      this.usage = usage;
    }

    Command(CommandFunc f, int argc, String shortDoc, String... docLines)
    {
      StringBuilder sb;
      this.f = f;
      this.minArgs = argc;
      this.doc = shortDoc;
      
      sb = new StringBuilder();
      for (String s : docLines)
      {
        sb.append(s);
        sb.append("\n");
      }
      this.usage = sb.toString();
    }

  }

  static TreeMap<String, Command> commandTable;

  static
  {
    Command c;

    commandTable = new TreeMap<String, Command>();

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        syncRead(r, args);
                      }
    },
      0,
      "Search for tags",
      "read [timeout]",
      "timeout -- Number of milliseconds to search.  Defaults to a reasonable average number.",
      "",
      "read",
      "read 3000");
    commandTable.put("read", c);
    commandTable.put("read-sync", c);

    c = new Command("Demonstrate asynchronous search",
                    "<time in ms>", 0,
                    new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        asyncRead(r, args);
                      }
                    });
    commandTable.put("read-async", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        memReadBytes(r, args);
                      }
    }, 
      3,
      "Read tag memory bytewise",
      "readmembytes bank addr len [target]",
      "bank -- Memory bank",
      "addr -- Byte address within memory bank",
      "len -- Number of bytes to read",
      "target -- Tag whose memory to read",
      "",
      "readmembytes 1 4 12",
      "readmembytes 1 4 12 EPC:E2003411B802011083257015");
    commandTable.put("readmembytes", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        memReadWords(r, args);
                      }
    },
      3,
      "Read tag memory wordwise",
      "readmemwords bank addr len [target]",
      "bank -- Memory bank",
      "addr -- Word address within memory bank",
      "len -- Number of Words to read",
      "target -- Tag whose memory to read",
      "",
      "readmemwords 1 2 6",
      "readmemwords 1 2 6 EPC:E2003411B802011083257015");
    commandTable.put("readmemwords", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        memWriteBytes(r, args);
                      }
    },
      3,
      "Write tag memory bytewise",
      "writemembytes bank addr data [target]",
      "bank -- Memory bank",
      "addr -- Byte address within memory bank",
      "data -- Bytes to write",
      "target -- Tag whose memory to write",
      "",
      "writemembytes 0 0 bytes:12345678",
      "writemembytes 0 0 bytes:12345678 EPC:044D00000000040007010062");
    commandTable.put("writemembytes", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        memWriteWords(r, args);
                      }
    },
      3,
      "Write tag memory wordwise",
      "writememwords bank addr data [target]", 
      "bank -- Memory bank",
      "addr -- Word address within memory bank",
      "data -- Words to write",
      "target -- Tag whose memory to write",
      "",
      "writememwords 0 0 words:12345678",
      "writememwords 0 0 words:12345678 EPC:044D00000000040007010062"
      );
    commandTable.put("writememwords", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        tagWrite(r, args);
                      }
    },
      1,
      "Write tag EPC",
      "writeepc epc [target]",
      "epc -- EPC to write to tag",
      "target -- Tag to write",
      "",
      "writeepc EPC:0123456789ABCDEF01234567",
      "writeepc EPC:4B4F6C095977225FC81B6A0F EPC:0123456789ABCDEF01234567");
    commandTable.put("writeepc", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        tagWritePw(r, args);
                      }
    },
      1,
      "Set Gen2 tag's access password",
      "setaccpw password [target]",
      "password -- 32-bit access password",
      "target -- Tag to modify.  If none given, uses first available tag",
      "",
      "setaccpw 0x12345678",
      "setaccpw 0x12345678 EPC:044D00000000040007010062"
      );
    commandTable.put("setaccpw", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        tagWriteKillPw(r, args);
                      }
    },
      2,
      "Set Gen2 tag's kill password", 
      "setkillpw password [target]",
      "password -- 32-bit kill password",
      "target -- Tag to modify.  If none given, uses first available tag",
      "",
      "setkillpw 0x12345678",
      "setkillpw 0x12345678 EPC:044D00000000040007010062");
    commandTable.put("setkillpw", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        tagKill(r, args);
                      }
    },
      1,
      "Kill tag",
      "kill password [target]",
      "password -- Kill password",
      "target -- Tag to kill",
      "",
      "kill 0x12345678",
      "kill 0x12345678 EPC:044D00000000040007010062"
      );
    commandTable.put("kill", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        tagLock(r, args);
                      }
    },
      1,
      "Lock (or unlock) tag memory",
      "lock actionlist [target]",
      "actionlist -- List of lock actions to apply to tag",
      "target -- Tag to act on",
      "",
      "lock Gen2.LockAction:EPC_LOCK,EPC_PERMALOCK,USER_UNLOCK",
      "lock Gen2.LockAction:EPC_LOCK,EPC_PERMALOCK,USER_UNLOCK EPC:044D00000000040007010062");
    commandTable.put("lock", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        setParam(r, args);
                      }
    },
      2,
      "Set reader parameter",
      "set name value",
      "name -- Name of reader parameter",
      "value -- Value to assign to reader parameter",
      "",
      "set gen2Session 1",
      "set singulation EPC:1234567890ABCDEF1234567",
      "set singulation null",
      "set readPlan [1,2]",
      "set readPlan AutoReadPlan:");
    commandTable.put("set", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        getParam(r, args);
                      }
    },
      0,
      "Read reader parameter",
      "get [name...]", 
      "With no arguments, gets all reader parameters",
      "With one or more arguments, gets each named parameter",
      "",
      "get",
      "get model",
      "get softwareVersion supportedProtocols");
    commandTable.put("get", c);

    c = new Command("List available reader parameters (names which can be used with get and set commands)",
                    "", 0,
                    new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        getParamList(r, args);
                      }
                    });
    commandTable.put("params", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        loadFw(r, args);
                      }
    },
      1,
      "Reload reader firmware",
      "loadfw filename",
      "filename -- Name of firmware file to load",
      "",
      "loadfw M5eApp_1.0.37.85-20081001-0412.sim");
    commandTable.put("loadfw", c);


    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        setGPO(r, args);
                      }
    },
      2,
      "Write output pin",
      "set-gpio pin value",
      "pin -- Pin number",
      "value -- Pin state: True or 1 to set pin high, False or 0 to set pin low",
      "",
      "set-gpio 1 1",
      "set-gpio 1 0"
      );
    commandTable.put("set-gpio", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        getGPI(r, args);
                      }
    },
      0,
      "Read input pins", 
      "get-gpio",
      "",
      "",
      "",
      "get-gpio");
    commandTable.put("get-gpio", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        int[] ports = ((SerialReader)r).cmdGetTxRxPorts();
                        System.out.printf("Tx port %d, Rx port %d\n",
                                          ports[0], ports[1]);
                      }
    },
      0,
      "Get Tx/Rx port (serial reader only)", 
      "cmdGetTxRxPorts",
      "",
      "Get the number of the tx and rx ports",
      "",
      "cmdGetTxRxPorts");
    commandTable.put("cmdGetTxRxPorts", c);


    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        TagProtocol t = ((SerialReader)r).cmdGetProtocol();
                        System.out.printf("Protocol: %s\n", t.toString());
                      }
    },
      0,
      "Get tag protocol (serial reader only)", 
      "cmdGetProtocol",
      "",
      "Get the current tag protocol",
      "",
      "cmdGetProtocol");
    commandTable.put("cmdGetProtocol", c);

    c = new Command(new CommandFunc()
    {
      public void function(Reader r, String args[])
        throws ReaderException
      {
        SerialReader.ReaderStatistics ps;
        ps = ((SerialReader)r).cmdGetReaderStatistics(
          EnumSet.of(SerialReader.ReaderStatisticsFlag.RF_ON_TIME,
                     SerialReader.ReaderStatisticsFlag.NOISE_FLOOR,
                     SerialReader.ReaderStatisticsFlag.NOISE_FLOOR_TX_ON));
        if (ps.rfOnTime != null)
        {
          System.out.printf("RF on time: %s\n",
                            unparseValue(ps.rfOnTime));
        }
        if (ps.noiseFloor != null)
        {
          System.out.printf("Noise floor: %s\n",
                            unparseValue(ps.noiseFloor));
        }
        if (ps.noiseFloorTxOn != null)
        {
          System.out.printf("Noise floor with TX on %s\n",
                            unparseValue(ps.noiseFloorTxOn));
        }
        
      }
    },
      0,
      "Get reader port statistics (serial reader only)",
      "cmdGetReaderStatistics",
      "",
      "Get the reader statistics",
      "",
      "cmdGetReaderStatistics");
    commandTable.put("cmdGetReaderStatistics", c);
    
    c = new Command(new CommandFunc()
    {
      public void function(Reader r, String args[])
        throws ReaderException
      {
        SerialReader sr = (SerialReader)r;
        Gen2.TagData tagSelect = new Gen2.TagData("BCBCBCBCBCBCBCBCBCBCBCBC");

        TagReadData tagData =
          sr.cmdGen2ReadTagData(500,EnumSet.of(TagReadData.TagMetadataFlag.PROTOCOL,
                                           TagReadData.TagMetadataFlag.FREQUENCY,
                                           TagReadData.TagMetadataFlag.READCOUNT,
                                           TagReadData.TagMetadataFlag.ANTENNAID,
                                           TagReadData.TagMetadataFlag.RSSI,
                                           TagReadData.TagMetadataFlag.TIMESTAMP),
                            Gen2.Bank.RESERVED,
                            0,
                            2,
                            0,
                            tagSelect);
        System.out.println(tagData.toString());
        System.out.print("Tag data:");
        for (byte b : tagData.getData())
          System.out.printf(" %02x", b);
        System.out.println("");
      }
    },
      0,
      "Test read data with metadata",
      "cmdReadDataMeta");
    commandTable.put("cmdReadDataMeta", c);
    
      

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        ((SerialReader)r).cmdResetReaderStatistics(
                          EnumSet.of(SerialReader.ReaderStatisticsFlag.RF_ON_TIME));
                                     
                      }
    },
      0,
      "Reset reader port statistics (serial reader only)",
      "cmdResetReaderStatistics",
      "",
      "Reset the reader statistics",
      "",
      "cmdResetReaderStatistics");
    commandTable.put("cmdResetReaderStatistics", c);



    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        killMultiple(r, args);
                      }
    },
      3,
      "Search for tags and kill each one found",
      "killmultiple timeout accesspassword killpassword [target]",
      "timeout -- number of milliseconds to search",
      "accesspassword - Gen2 access password",
      "killpassword - Gen2 kill password",
      "target - Tag(s) to kill",
      "",
      "killmultiple 1000 0x11111111 0x22222222",
      "killmultiple 1000 0x11111111 0x22222222 Gen2Select:EPC,32,1,80");
    commandTable.put("killmultiple", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        lockMultiple(r, args);
                      }
    },
      4,
      "Search for tags and lock each one found",
      "lockmultiple timeout accesspassword mask action [target]",
      "timeout -- number of milliseconds to search",
      "accesspassword - Gen2 access password",
      "mask - Gen2 lock mask",
      "mask - Gen2 lock action",
      "",
      "lockmultiple 1000 0x11111111 0x0002 0x0000",
      "lockmultiple 1000 0x11111111 0x0002 0x0000 EPC:DEADBEEFCAFEBABE");
    
    commandTable.put("lockmultiple", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        writeMultiple(r, args);
                      }
    },
      4,
      "Search for tags and write data to each one found",
      "writemultiple timeout accesspassword bank addr data [target]",
      "timeout -- number of milliseconds to search",
      "accesspassword - Gen2 access password",
      "bank -- Memory bank",
      "addr -- Byte address within memory bank",
      "data -- Bytes to write",
      "target -- Tag whose memory to write",
      "",
      "writemultiple 1000 0x11111111 0 0 bytes:12345678",
      "writemultiple 1000 0x11111111 0 0 bytes:12345678 EPC:044D00000000040007010062");
    commandTable.put("writemultiple", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        readMultiple(r, args);
                      }
    },
      4,
      "Search for tags and read data from each one found",
      "readmultiple timeout accesspassword bank addr count [target]",
      "timeout -- number of milliseconds to search",
      "accesspassword - Gen2 access password",
      "bank -- Memory bank",
      "addr -- Byte address within memory bank",
      "count -- Number of bytes to read",
      "target -- Tag whose memory to read",
      "",
      "readmultiple 1000 0x11111111 0 0 4",
      "readmultiple 1000 0x11111111 0 0 4 EPC:044D00000000040007010062");
    commandTable.put("readmultiple", c);



    // Internal demo program functions
    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                      {
                        help(r, args);
                      }
    },
      0,
      "Print program usage instructions",
      "help [command]",
      "With no arguments, prints a list of available commands.",
      "With a command name, prints detailed help for that command.");
    commandTable.put("help", c);

    c = new Command("Echo text",
                    "", 0,
                    new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                      {
                        echo(r, args);
                      }
                    });
    commandTable.put("echo", c);


    c = new Command("Print value",
                    "<value>", 1,
                    new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                      {
                        printValue(r, args);
                      }
                    });
    commandTable.put("print", c);

    c = new Command("Trace communications",
                    "<on|off>", 1,
                    new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                      {
                        setTrace(r, args);
                      }
                    });
    commandTable.put("trace", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        savedConfiguration(r, args);
                      }
    },
      0,
      "set/get User Profile test"
       );
    commandTable.put("testsaveconfig", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testBlockWrite(r, args);
                      }
    },
      0,
      "test for block write functionality"
      );
    commandTable.put("testblockwrite", c);

c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testBlockPermalock(r, args);
                      }
    },
      0,
      "testing blockpermalock functionality"
      );
    commandTable.put("testblockpermalock", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testEmbeddedWrite(r, args);
                      }
    },
      0,
      "testing functionality for embedded tagop"
      );
    commandTable.put("testembeddedwrite", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testPowerSave(r, args);
                      }
    },
      0,
      "testing power save option"
     );
    commandTable.put("testpowersave", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testMultiProtocolSearch(r, args);
                      }
    },
      0,
      "testing multiprotocol search"
      );
    commandTable.put("testmultiprotocolsearch", c);

     c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testWriteMode(r, args);
                      }
    },
      0,
      "test for write mode"
      );
    commandTable.put("testwritemode", c);

     c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testRegion(r, args);
                      }
    },
      0,
      "testing writemode parameter functioning"
      );
    commandTable.put("testregion", c);

    c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testBaud(r, args);
                      }
    },
      0,
      "test for sync baud rate"
       );
    commandTable.put("testbaud", c);

     c = new Command(new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                        throws ReaderException
                      {
                        testMultiProtocolReadPlan(r, args);
                      }
    },
      0,
      "testing multiprotocol read plan"
      );
    commandTable.put("testmultiprotocolreadplan", c);
    /*
    c = new Command("",
                    "",
                    new CommandFunc()
                    {
                      public void function(Reader r, String args[])
                      {
                        f(r, args);
                      }
                    });
    commandTable.put("", c);
    */

  }

  public static void main(String argv[])
    throws ReaderException
  {
    TagFilter target;

    Reader r;
    int nextarg;
    boolean trace;

    r = null;
    target = null;
    trace = false;

    nextarg = 0;

    if (argv.length < 1)
      usage();

    if (argv[nextarg].equals("-v"))
    {
      trace = true;
      nextarg++;
    }
    
    if (argv.length == (nextarg + 1)
        || argv[nextarg].equals("shell") 
        || argv[nextarg].equals("script"))
    {
      BufferedReader cmdIn;
      boolean echo;

      if (argv.length > nextarg && argv[nextarg].equals("script"))
      {
        FileInputStream f;

        echo = false;
        try
        {
          f = new FileInputStream(argv[nextarg + 1]);
          cmdIn = new BufferedReader(new InputStreamReader(f));
        }
        catch (java.io.FileNotFoundException fnfe)
        {
          System.err.printf("Could not find file %s\n ", argv[nextarg + 1]);
          return;
        }
        nextarg+=2;
      }
      else
      {
        cmdIn = new BufferedReader(new InputStreamReader(System.in));
        echo = true; // We'd really like isatty(3) here, but no such luck.
      }


      r = Reader.create(argv[nextarg]);
      if (trace)
      {
        setTrace(r, new String[] {"on"});
      }
      r.connect();
      r.paramSet("/reader/region/id", Reader.Region.NA);

      try
      {
        while (true)
        {
          String line, args[];
          if (echo)
            System.out.printf(">>> ");
          
          line = cmdIn.readLine();
          if (line == null)
          {
            if (echo)
              System.out.printf("\n");
            break;
          }
          // split on # removes comments
          if (line.length() == 0)
            continue;
          args = line.split("#"); // Permit comments
          if (args.length == 0)
            continue;
          args = args[0].split("\\s+");
          if(args.length == 0 || args[0].length() == 0)
            continue;

          try
          {
            runCommand(r, args);
          }
          catch (Exception e)
          {
            System.err.printf("Exception: %s\n", e);
            e.printStackTrace();
          }
        }
      }
      catch (java.io.IOException ie)
      {
        System.err.printf("%s\n", ie);
      }
    }
    else
    {
      String args[] = new String[argv.length-nextarg-1];
      r = Reader.create(argv[nextarg]);
      if (trace)
      {
        setTrace(r, new String[] {"on"});
      }
      r.connect();
      r.paramSet("/reader/region/id", Reader.Region.NA);
      System.arraycopy(argv, nextarg+1, args, 0, argv.length - nextarg - 1);
      runCommand(r, args);
    }

    r.destroy();
  }


  static void runCommand(Reader r, String argv[])
    throws ReaderException
  {
    String command = argv[0];

    Command cmd = commandTable.get(command);
    if (cmd != null)
    {
      if (cmd.minArgs > argv.length-1)
      {
        System.err.printf("Not enough arguments to command \"%s\". Try \"help\"\n", command);
        return;
      }
      String args[] = new String[argv.length - 1];
      System.arraycopy(argv, 1, args, 0, argv.length - 1);
      cmd.f.function(r, args);
    }
    else
    {
      System.out.printf("No command \"%s\". Try \"help\"\n", command);
    }
  }

  public static void syncRead(Reader r, String args[]) throws ReaderException
  {
    TagReadData[] t;
    int timeout;
    SerialReader sr=(SerialReader)r;
    if (args.length > 0)
      timeout = Integer.decode(args[0]);
    else
      timeout = 1000; // Is there a per-reader default value?

    try
    {
      t = r.read(timeout);
    }
    catch (ReaderException re)
    {
      System.out.printf("Error reading tags: %s\n", re.getMessage());
      return;
    }
    for (int i = 0; i < t.length; i++)
    {
      for ( Reader.GpioPin pin : t[i].getGpio() )
        System.out.printf(" "+pin.toString());
        //System.out.printf("PIN:"+"{"+pin.id+"}"+"{"+(pin.high?"H":"L")+"}");
      System.out.printf("%s\n", t[i]);
    }
  }

  static class sampleListener
    implements ReadListener, ReadExceptionListener
  {

    public void tagRead(Reader r, TagReadData t)
    {
      System.out.printf("%s\n", t);
    }

    public void tagReadException(Reader r, ReaderException re)
    {
      System.out.printf("Error reading: %s\n", re.getMessage());
    }
  }

  public static void asyncRead(Reader r, String args[])
  {
    int timeout;
    sampleListener l = new sampleListener();

    if (args.length > 0)
      timeout = Integer.decode(args[0]);
    else
      timeout = 1000; // Is there a per-reader default value?

    r.addReadListener(l);
    r.addReadExceptionListener(l);

    r.startReading();
    try
    {
      Thread.sleep(timeout);
      r.stopReading();
    }
    catch (InterruptedException e) {}

    r.removeReadListener(l);
    r.removeReadExceptionListener(l);
  }

  // Produce a TagFilter object or null from a string.
  // Accepted syntaxes:
  //   0x111122223333444455556666
  //   777788889999AAAABBBBCCCC
  //   EPC:DDDDEEEEFFFF000011112222
  //   GEN2:333344445555666677778888
  //   any
  static TagFilter parseEPC(String arg)
  {

    if (arg.equalsIgnoreCase("any"))
      return null;
    else if (arg.substring(0,4).equalsIgnoreCase("epc:"))
      return new TagData(arg.substring(4));
    else if (arg.substring(0,5).equalsIgnoreCase("gen2:"))
      return new Gen2.TagData(arg.substring(5));
    else
      return new TagData(arg);
  }


  public static void memReadBytes(Reader r, String args[])
  {
    TagFilter target;
    int bank, address, count;
    byte[] values;

    bank = Integer.decode(args[0]);
    address = Integer.decode(args[1]);
    count = Integer.decode(args[2]);

    if (args.length > 3)
      target = (TagFilter)parseValue(args[3]);
    else
      target = null;

    try
    {
      values = r.readTagMemBytes(target, bank, address, count);
      System.out.printf("bytes:");
      for (int i = 0; i < values.length; i++)
      {
        System.out.printf("%02x", values[i]);
      }
      System.out.printf("\n");
    }
    catch (ReaderException re)
    {
      System.out.printf("Error reading memory of tag: %s\n",
                        re.getMessage());
    }
  }

  public static void memReadWords(Reader r, String args[])
  {
    TagFilter target;
    int bank, address, count;
    short[] values;

    bank = Integer.decode(args[0]);
    address = Integer.decode(args[1]);
    count = Integer.decode(args[2]);

    if (args.length > 3)
      target = (TagFilter)parseValue(args[3]);
    else
      target = null;

    try
    {
      values = r.readTagMemWords(target, bank, address, count);
      System.out.printf("words:");
      for (int i = 0; i < values.length; i++)
      {
        System.out.printf("%04x", values[i]);
      }
      System.out.printf("\n");
    }
    catch (ReaderException re)
    {
      System.out.printf("Error reading memory of tag: %s\n",
                        re.getMessage());
    }
  }


  public static void memWriteBytes(Reader r, String args[])
  {
    TagFilter target;
    int bank, address;
    byte[] values;

    bank = Integer.decode(args[0]);
    address = Integer.decode(args[1]);
    values = (byte[])parseValue(args[2]);

    if (args.length > 3)
      target = (TagFilter)parseValue(args[3]);
    else
      target = null;

    try
    {
      r.writeTagMemBytes(target, bank, address, values);
    }
    catch (ReaderException re)
    {
      System.out.printf("Error writing memory of tag: %s\n",
                        re.getMessage());
    }
  }


  public static void memWriteWords(Reader r, String args[])
  {
    TagFilter target;
    int bank, address;
    short[] values;

    bank = Integer.decode(args[0]);
    address = Integer.decode(args[1]);
    values = (short[])parseValue(args[2]);

    if (args.length > 3)
      target = (TagFilter)parseValue(args[3]);
    else
      target = null;

    try
    {
      r.writeTagMemWords(target, bank, address, values);
    }
    catch (ReaderException re)
    {
      System.out.printf("Error writing memory of tag: %s\n",
                        re.getMessage());
    }
  }

  public static void tagWrite(Reader r, String args[])
  {
    TagFilter target;
    TagData t;

    t = (TagData) parseValue(args[0]);

    if (args.length > 1)
      target = (TagFilter)parseValue(args[1]);
    else
      target = null;

    try
    {
      r.writeTag(target, t);
    }
    catch (ReaderException re)
    {
      System.out.printf("Error writing tag ID: %s\n",
                        re.getMessage());
    }

  }


  public static void tagWritePw(Reader r, String args[])
  {
    TagFilter target;
    int pw;

    pw = Integer.decode(args[0]);

    if (args.length > 1)
      target = (TagFilter)parseValue(args[1]);
    else
      target = null;

    try
    {
      r.writeTagMemWords(target, 0, 2, new short[] {(short)(pw >> 16), 
                                                    (short)(pw & 0xffff)});
    }
    catch (ReaderException re)
    {
      System.out.printf("Error writing tag access password: %s\n",
                        re.getMessage());
    }
  }


  public static void tagWriteKillPw(Reader r, String args[])
  {
    TagFilter target;
    int pw;

    pw = Integer.decode(args[0]);

    if (args.length > 1)
      target = (TagFilter)parseValue(args[1]);
    else
      target = null;

    try
    {
      r.writeTagMemWords(target, 0, 0, new short[] {(short)(pw >> 16), 
                                                    (short)(pw & 0xffff)});
    }
    catch (ReaderException re)
    {
      System.out.printf("Error writing tag access password: %s\n",
                        re.getMessage());
    }
  }

  public static void tagKill(Reader r, String args[])
    throws ReaderException
  {
    TagFilter target;
    int pw;

    pw = Integer.decode(args[0]);

    if (args.length > 1)
      target = (TagFilter)parseValue(args[1]);
    else
      target = null;

    r.killTag(target, new Gen2.Password(pw));
  }


  public static void tagLock(Reader r, String args[])
    throws ReaderException
  {
    TagFilter target;
    TagLockAction la;

    la = (TagLockAction)parseValue(args[0]);

    if (args.length > 1)
      target = (TagFilter)parseValue(args[1]);
    else
      target = null;

    r.lockTag(target, la);
  }


  static String unparseValue(Object o)
  {

    if (o == null)
      return "null";
    
    if (o.getClass().isArray())
    {
      int l = Array.getLength(o);
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (int i = 0; i < l; i++)
      {
        sb.append(unparseValue(Array.get(o, i)));
        if (i + 1 < l)
          sb.append(",");
      }
      sb.append("]");
      return sb.toString();
    }
    else if (o instanceof SimpleReadPlan)
    {
      SimpleReadPlan s = (SimpleReadPlan) o;

      return String.format("SimpleReadPlan:[%s,%s,%s,%d]",
                           unparseValue(s.protocol),
                           unparseValue(s.antennas),
                           unparseValue(s.filter),
                           s.weight);
    }
    else if (o instanceof TagProtocol)
    {
      return String.format("TagProtocol:%s", o);
    }
    else if (o instanceof String)
    {
      return "string: " + (String)o;
    }

    return o.toString();
  }

  public static void getParam(Reader r, String args[])
    throws ReaderException
  {
    Object val;

    if (args.length == 0) // get everything
    {
      String[] params = r.paramList();
      java.util.Arrays.sort(params);
      args = params;
    }

    for (String param : args)
    {
      try {
        val = r.paramGet(param);
      } catch (IllegalArgumentException ie)
      {
        System.err.printf("No parameter named %s\n", param);
        continue;
      }

      System.out.printf("%s: %s\n", param, unparseValue(val));
    }
  }

  static byte[] parseHexBytes(String s)
  {
    int len = s.length();
    byte[] bytes = new byte[len/2];
    
    for (int i = 0; i < len; i+=2)
      bytes[i/2] = (byte)Integer.parseInt(
        s.substring(i, i + 2), 16);

    return bytes;
  }


  static Object parseValue(String s)
  {
    
    s = s.toLowerCase();

    if (s.equals("null"))
      return null;
    else if (s.equals("true"))
      return new Boolean(true);
    else if (s.equals("false"))
      return new Boolean(false);

    // Array of ints or arrays of ints
    if (s.startsWith("[") && s.endsWith("]"))
    {
      String strings[];

      if (s.indexOf('[',1) != -1)
      {
        int intArrs[][];
        Vector<int[]> intArrVec;
        int start, end;
        intArrVec = new Vector<int[]>();

        start = s.indexOf('[', 1);
        while (start != -1)
        {
          int[] ints;
          end = s.indexOf(']', start);
          ints = (int[])parseValue(s.substring(start,end + 1));
          intArrVec.add(ints);
          start = s.indexOf('[', end);
        }
        return intArrVec.toArray(new int[intArrVec.size()][]);
      }
      else
      {
        int ints[];

        if (s.length() > 2)
          strings = s.substring(1, s.length() - 1).split(",");
        else
          strings = new String[0];

        ints = new int[strings.length];
        for (int i = 0; i < strings.length; i++)
        {
          ints[i] = Integer.decode(strings[i]);
        }
        return ints;
      }
    }

    // Byte array
    if (s.startsWith("bytes:"))
    {
      return parseHexBytes(s.substring(6));
    }

    // Word array
    if (s.startsWith("words:"))
    {
      int len = s.length() - 6;
      short[] words = new short[len/4];
      for (int i = 0; i < len; i+=4)
        words[i/4] = (short)Integer.parseInt(
          s.substring(6 + i, 6 + i + 4), 16);
      return words;
    }

    // "Gen2.LockAction:mask,action"
    if (s.startsWith("gen2.lockaction:"))
    {
      String sub = s.substring(16);
      try
      {
        String actStrings[];
        actStrings = sub.split(",");
        return new Gen2.LockAction(Integer.decode(actStrings[0]),
                                   Integer.decode(actStrings[1]));
      }
      catch (NumberFormatException e)
      {
      }

      try
      {
        return Gen2.LockAction.parse(sub);
      }
      catch (Exception e)
      {
      }
    }

    if (s.startsWith("gen2target:"))
    {
      return Enum.valueOf(Gen2.Target.class,
                          s.substring(11));
    }

    // "SimpleReadPlan:[]"
    // "SimpleReadPlan:[1,2]"
    // "SimpleReadPlan:[TagProtocol:GEN2,[1,2]]"
    // "SimpleReadPlan:[TagProtocol:GEN2,[1,2],Gen2.Select:EPC,16,32,DEADBEEF]"
    // "SimpleReadPlan:[TagProtocol:GEN2,[1,2],EPC:E200]"
    if (s.startsWith("simplereadplan:"))
    {
      int cindex;
      String sub;
      
      sub = s.substring(15);
      if (!sub.startsWith("[tagprotocol"))
        return new SimpleReadPlan((int[]) parseValue(sub), TagProtocol.GEN2);

      TagProtocol t;
      int[] antennas;
      TagFilter filter = null;
      int weight = 1000;

      cindex = sub.indexOf(',');        
        
      t = (TagProtocol) parseValue(sub.substring(1, cindex));

      sub = sub.substring(cindex + 1, sub.length() - 1);

      cindex = sub.indexOf(']');
      antennas = (int[]) parseValue(sub.substring(0, cindex + 1));
      
      cindex = sub.indexOf(',', cindex);
      if (cindex != -1)
      {
        sub = sub.substring(cindex + 1);
        
        if (sub.startsWith("epc:"))
          filter = parseEPC(sub);
        else
          filter = (TagFilter) parseValue(sub);
      }

      return new SimpleReadPlan(antennas, t, filter, weight);
    }
    
    // "MultiReadPlan:[
    if (s.startsWith("multireadplan:"))
    {
      String sub, newsub;
      Vector<ReadPlan> rps = new Vector<ReadPlan>();
      int len, count, first, i;
      char at;
      
      sub = s.substring(15);
      while (true)
      {
        // Find the matching end-bracket of the subplan
        len = sub.length();
        count = 1;
        i = sub.indexOf('[') + 1;
        while (count != 0)
        {
          at = sub.charAt(i);
          
          if (at == '[')
            count++;
          else if (at == ']')
            count--;
          i++;
        }
        newsub = sub.substring(0, i);
        
        rps.add((ReadPlan) parseValue(newsub));

        i = sub.indexOf(',',i);
        if (i == -1)
          break;
        
        sub = sub.substring(i+1);
      }
      return new MultiReadPlan(rps.toArray(new ReadPlan[rps.size()]));
    }
    

    if (s.startsWith("tagprotocol:"))
    {
      return TagProtocol.getProtocol(s.substring(12));
    }

    // "Gen2.Select:~EPC,16,32,DEADBEEF"
    if (s.startsWith("gen2.select:"))
    {
      boolean invert = false;
      String[] strings = s.substring(12).split(",");
      if (strings[0].startsWith("~"))
      {
        strings[0] = strings[0].substring(1);
        invert = true;
      }
      return new Gen2.Select(invert,
                             Enum.valueOf(Gen2.Bank.class,
                                          strings[0].toUpperCase()),
                             (int)Integer.decode(strings[1]),
                             (int)Integer.decode(strings[2]),
                             parseHexBytes(strings[3]));
    }


    try
    {
      Integer i = (Integer)(int)(long)Long.decode(s);
      return i;
    }
    catch (NumberFormatException ne)
    {
    }

    try
    {
      TagFilter f = parseEPC(s);
      return f;
    }
    catch (Exception e)
    {
    }

    return s;
  }

  public static void setParam(Reader r, String args[])
    throws ReaderException
  {
    Object v;


    // Special-case some difficult-to-parse types.
    if (args[0].equals("/reader/tagop/protocol"))
    {
      v = Enum.valueOf(TagProtocol.class, args[1]);
    }
    else if (args[0].equals("/reader/region/id"))
    {
      v = Enum.valueOf(Reader.Region.class, args[1]);
    }
    else if (args[0].equals("/reader/gen2/session"))
    {
      v = Enum.valueOf(Gen2.Session.class, args[1]);
    }
    else if (args[0].equals("/reader/gen2/millerm"))
    {
      v = Enum.valueOf(Gen2.TagEncoding.class, args[1]);
    }
    else if (args[0].equals("/reader/gen2/target"))
    {
      v = Enum.valueOf(Gen2.Target.class, args[1]);
    }
    else if (args[0].equals("/reader/iso180006b/linkFrequency"))
    {
      v = Enum.valueOf(Iso180006b.LinkFrequency.class, args[1]);
    }
    else if (args[0].equals("/reader/userMode"))
    {
      v = Enum.valueOf(SerialReader.UserMode.class, args[1]);
    }
    else if (args[0].equals("/reader/gen2/accessPassword"))
    {
      v = new Gen2.Password((Integer)parseValue(args[1]));
    }
    else
      v = parseValue(args[1]);


    r.paramSet(args[0], v);
  }

  public static void getParamList(Reader r, String args[])
    throws ReaderException
  {
    String[] params;

    params = r.paramList();
    java.util.Arrays.sort(params);
    for (String p : params)
    {
      System.out.printf("%s\n", p);
    }
  }

  public static void loadFw(Reader r, String args[])
  {
    FileInputStream f;

    try
    {
      f = new FileInputStream(args[0]);
      r.firmwareLoad(f);
      f.close();
    }
    catch (java.io.FileNotFoundException fnfe)
    {
      System.err.printf("Could not find file %s\n ", args[0]);
    }
    catch (java.io.IOException ie)
    {
      System.err.printf("IO error: %s\n", ie.getMessage());
    }
    catch (ReaderException re)
    {
      System.err.printf("Reader error: %s\n", re.getMessage());
    }
  }


  public static void help(Reader r, String args[])
  {

    if (args.length == 0)
    {
      for (String s : commandTable.keySet())
      {
        System.out.printf("%s -- %s\n", s, commandTable.get(s).doc);
      }

      System.out.printf("Type \"help\" followed by command name for more documentation.\n");
    }
    else
    {
      Command cmd;

      cmd = commandTable.get(args[0]);
      if (cmd == null)
      {
        System.out.printf("No command \"%s\". Try \"help\"\n", args[0]);
      }
      else
      {
        System.out.printf("%s\n%s\n", cmd.doc, cmd.usage);
      }
    }

  }

  public static void printValue(Reader r, String args[])
  {
    Object o;

    o = parseValue(args[0]);
    System.out.printf("%s\n", unparseValue(o));
  }

  public static void echo(Reader r, String args[])
  {
    
    for (String a : args)
    {
      System.out.printf("%s ", a);
    }
    System.out.printf("\n");
  }

  public static void getGPI(Reader r, String args[])
    throws ReaderException
  {
    Reader.GpioPin[] state;

    state = r.gpiGet();
    for (Reader.GpioPin gp : state)
      System.out.printf("Pin %d: %s\n", gp.id, gp.high ? "High" : "Low");
  }

  static boolean parseBool(String boolString)
  {
    String s = boolString.toLowerCase();

    if (s.equals("true") || s.equals("high") || s.equals("1"))
      return true;
    else if (s.equals("false") || s.equals("low") || s.equals("0"))
      return false;

    throw new IllegalArgumentException();
  }


  public static void setGPO(Reader r, String args[])
    throws ReaderException
  {
    boolean b;

    r.gpoSet(new Reader.GpioPin[] {new Reader.GpioPin((Integer)parseValue(args[0]), 
                                                      parseBool(args[1]))});
  }


  public static void killMultiple(Reader r, String args[])
    throws ReaderException
  {
    TagFilter target;
    int accesspw, killpw, timeout;
    int[] ret;
    SerialReader sr;
    
    sr = (SerialReader)r;
    
    timeout = Integer.decode(args[0]);
    accesspw = (int)(long)Long.decode(args[1]);
    killpw = (int)(long)Long.decode(args[2]);

    if (args.length > 3)
      target = (TagFilter)parseValue(args[3]);
    else
      target = null;
    
    sr.cmdClearTagBuffer();
    ret = sr.
      cmdReadTagAndKillMultiple(timeout, SerialReader.AntennaSelection.
                                CONFIGURED_ANTENNA,
                                target, accesspw, killpw);
    System.out.printf("%d tags found, %d killed, %d not killed\n",
                      ret[0], ret[1], ret[2]);
  }

  public static void lockMultiple(Reader r, String args[])
    throws ReaderException
  {
    TagFilter target;
    int accesspw, timeout, action, mask;
    int[] ret;
    SerialReader sr;
    
    sr = (SerialReader)r;

    timeout = Integer.decode(args[0]);
    accesspw = (int)(long)Long.decode(args[1]);
    mask = Integer.decode(args[2]);
    action = Integer.decode(args[3]);

    if (args.length > 4)
      target = (TagFilter)parseValue(args[4]);
    else
      target = null;

    sr.cmdClearTagBuffer();
    ret = sr.
      cmdReadTagAndLockMultiple(timeout, SerialReader.AntennaSelection.
                                CONFIGURED_ANTENNA,
                                target, accesspw, mask, action);
    System.out.printf("%d tags found, %d locked, %d not locked\n",
                      ret[0], ret[1], ret[2]);
  }


  public static void writeMultiple(Reader r, String args[])
    throws ReaderException
  {
    TagFilter target;
    int timeout, accesspw, bank, address;
    byte[] values;
    int[] ret;
    SerialReader sr;
    
    sr = (SerialReader)r;

    timeout = Integer.decode(args[0]);
    accesspw = (int)(long)Long.decode(args[1]);
    bank = Integer.decode(args[2]);
    address = Integer.decode(args[3]);
    values = (byte[])parseValue(args[4]);

    if (args.length > 5)
      target = (TagFilter)parseValue(args[5]);
    else
      target = null;

    sr.cmdClearTagBuffer();
    ret = sr.
      cmdReadTagAndDataWriteMultiple(timeout, SerialReader.AntennaSelection.
                                     CONFIGURED_ANTENNA,
                                     target, accesspw, Gen2.Bank.getBank(bank), 
                                     address, values);
    
    System.out.printf("%d tags found, %d written, %d not written\n",
                      ret[0], ret[1], ret[2]);
  }

  public static void readMultiple(Reader r, String args[])
    throws ReaderException
  {
    TagFilter target;
    int timeout, accesspw, bank, address, count;
    int[] ret;
    SerialReader sr;
    TagReadData[] tr;
    
    sr = (SerialReader)r;

    timeout = Integer.decode(args[0]);
    accesspw = (int)(long)Long.decode(args[1]);
    bank = Integer.decode(args[2]);
    address = Integer.decode(args[3]);
    count =  Integer.decode(args[4]);

    if (args.length > 5)
      target = (TagFilter)parseValue(args[5]);
    else
      target = null;

    sr.cmdClearTagBuffer();
    ret = sr.
      cmdReadTagAndDataReadMultiple(timeout, SerialReader.AntennaSelection.
                                    CONFIGURED_ANTENNA,
                                    target, accesspw, Gen2.Bank.getBank(bank), 
                                    address, count);
    
    System.out.printf("%d tags found, %d read from, %d not read from\n",
                      ret[0], ret[1], ret[2]);
    tr = sr.cmdGetTagBuffer(EnumSet.of(TagReadData.TagMetadataFlag.DATA),
                            false, TagProtocol.GEN2);

    for (TagReadData t: tr)
    {
      System.out.printf("EPC:%s Data:", t.getTag().epcString());
      for (byte b : t.getData())
        System.out.printf("%02x", b);
      System.out.printf("\n");
    }
    
    
  }

   public static void savedConfiguration(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     sr.cmdSetProtocol(TagProtocol.GEN2);
     sr.cmdSetUserProfile(SerialReader.SetUserProfileOption.SAVE,SerialReader.ConfigKey.ALL,SerialReader.ConfigValue.CUSTOM_CONFIGURATION);
     byte data[]={0x06};
     byte response[];
     response=sr.cmdGetUserProfile(data);
     String str=sr.byteArrayToHexString(response);
     System.out.println(str);
  }
   
  public static void testBlockWrite(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     byte data1[]= {1,2,3,4};
     Gen2.BlockWrite tagop=new Gen2.BlockWrite(Gen2.Bank.USER,0,(byte)2,data1);
     Gen2.Select select=new Gen2.Select(false,Gen2.Bank.EPC,0,0,null);
     sr.executeTagOp(tagop,select);
  }

  public static void testBlockPermalock(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     byte data1[]= {};
     Gen2.BlockPermaLock tagop=new Gen2.BlockPermaLock(Gen2.Bank.USER,(byte)0,0,(byte)1,null);
     Gen2.Select select=new Gen2.Select(false,Gen2.Bank.EPC,0,0,null);
     byte[] data=(byte[])sr.executeTagOp(tagop,select);
     for(int i=0;i<data.length;i++)
      {
          System.out.println(data[i]);
      }
  }
public static void testEmbeddedWrite(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     byte data1[]= {1,2,3,4};
     Gen2.BlockWrite tagop=new Gen2.BlockWrite(Gen2.Bank.USER,0,(byte)2,data1);
     Gen2.Select select=new Gen2.Select(false,Gen2.Bank.EPC,0,0,null);
     SimpleReadPlan rp=new SimpleReadPlan(null, TagProtocol.GEN2,null , tagop, 1000);

     sr.executeTagOp(tagop,select);
     System.out.println("strt readplan");
     sr.paramSet("/reader/read/plan",rp);
     sr.read(1000);
  }
public static void testMultiProtocolSearch(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     //sr.paramSet("/reader/enableStreaming", false);
     //System.out.println((Integer)sr.paramGet("/reader/antenna/connectedPortList"));
     ReadPlan rp[]=new ReadPlan[2];
     SimpleReadPlan plan1 = new SimpleReadPlan(null, TagProtocol.GEN2, null,null,10);
     SimpleReadPlan plan2 = new SimpleReadPlan(null, TagProtocol.ISO180006B, null,null,10);
     rp[0]=plan1;
     rp[1]=plan2;
     MultiReadPlan testMultiReadPlan = new MultiReadPlan(rp);
     sr.paramSet("/reader/read/plan", testMultiReadPlan);
     TagReadData []xyx= sr.read(1000);
     for(TagReadData trd : xyx)
     {
         trd.getTag().getProtocol().toString();
         System.out.println(trd.getTag().getProtocol().toString()+": "+trd.toString());
     }
  }

public static void testMultiProtocolReadPlan(Reader r, String args[])
    throws ReaderException
  {
     ReadPlan rp[]=new ReadPlan[4];
     SimpleReadPlan plan1 = new SimpleReadPlan(null, TagProtocol.GEN2, null,null,0);
     SimpleReadPlan plan2 = new SimpleReadPlan(null, TagProtocol.ISO180006B, null,null,0);
     SimpleReadPlan plan3 = new SimpleReadPlan(null, TagProtocol.IPX256, null,null,0);
     SimpleReadPlan plan4 = new SimpleReadPlan(null, TagProtocol.IPX64, null,null,0);
     rp[0]=plan1;
     rp[1]=plan2;
     rp[2]=plan3;
     rp[3]=plan4;
     MultiReadPlan testMultiReadPlan = new MultiReadPlan(rp);
     r.paramSet("/reader/read/plan", testMultiReadPlan);
     TagReadData[] t;
     try
     {
       t = r.read(1000);
     }
     catch (ReaderException re)
     {
       System.out.printf("Error reading tags: %s\n", re.getMessage());
       return;
     }
    for(TagReadData trd : t)
     {
        // trd.getTag().getProtocol().toString();
         System.out.println(trd.getTag().getProtocol().toString()+": "+trd.toString());
     }

  }


public static void testPowerSave(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
      //  Boolean flag =new boolean(true);
     Boolean flag  =(Boolean) sr.paramGet("/reader/radio/enablePowerSave");
     System.out.println(flag);
     sr.paramSet("/reader/radio/enablePowerSave", true);
     flag=(Boolean) sr.paramGet("/reader/radio/enablePowerSave");
     System.out.println(flag);
     sr.paramSet("/reader/radio/enablePowerSave", false);
     flag =(Boolean) sr.paramGet("/reader/radio/enablePowerSave");
     System.out.println(flag);

}

public static void testWriteMode(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     short data[]={0xFF,0xFF};
     System.out.println("demo:"+data.length);
     sr.paramSet("/reader/gen2/writeMode", Gen2.WriteMode.WORD_ONLY);
    // Gen2.WriteTagMemWords write;
    // write = new Gen2.WriteTagMemWords(0,0,new short[]{1,2});
    // sr.executeTagOp(write, null);
    // sr.paramSet("/reader/gen2/writeMode", Gen2.WriteMode.BLOCK_FALLBACK);
     //write = new Gen2.WriteTagMemWords(0,0,new short[]{3,4});
     //sr.executeTagOp(write, null);
     //sr.paramSet("/reader/gen2/writeMode", Gen2.WriteMode.BLOCK_ONLY);
    // write = new Gen2.WriteTagMemWords(0,0,new short[]{5,6});
     //sr.executeTagOp(write, null);
  }

  public static void testRegion(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     sr.cmdSetRegion(Reader.Region.EU);
  }

  public static void testBaud(Reader r, String args[])
    throws ReaderException
  {
     SerialReader sr;
     sr = (SerialReader)r;
     System.out.println("Changing to 115200");
     sr.paramSet("/reader/baudrate", 115200);
     sr.cmdSetUserProfile(SerialReader.SetUserProfileOption.SAVE,SerialReader.ConfigKey.ALL,SerialReader.ConfigValue.CUSTOM_CONFIGURATION);
     System.out.println(sr.paramGet("/reader/baudrate"));
     System.out.println("Changing to 9600");
     sr.paramSet("/reader/baudrate", 9600);
     sr.cmdSetUserProfile(SerialReader.SetUserProfileOption.RESTORE, SerialReader.ConfigKey.ALL, SerialReader.ConfigValue.CUSTOM_CONFIGURATION);
    sr.paramGet("/reader/baudrate");
     byte data[]={0x06};
     byte response[];
     response=sr.cmdGetUserProfile(data);
     String str=sr.byteArrayToHexString(response);
     System.out.println(str);
     System.out.println("Restored user profile");
     System.out.println(sr.paramGet("/reader/baudrate"));

   
     
  }


  public static void setTrace(Reader r, String args[])
  {
    boolean b;
    SerialReader sr;

    if (args[0].toLowerCase().equals("on"))
    {
      if (currentListener == null)
      {
        if (r instanceof SerialReader)
        {
          if (serialPrinter == null)
            serialPrinter = new SerialPrinter();
          currentListener = serialPrinter;
        }
        else if (r instanceof RqlReader)
        {
          if (stringPrinter == null)
            stringPrinter = new StringPrinter();
          currentListener = stringPrinter;
        }
        else
        {
          System.out.println("Reader does not support message tracing");
          return;
        }
        r.addTransportListener(currentListener);
      }
    }
    else if (currentListener != null)
    {
      r.removeTransportListener(currentListener);
      currentListener = null;
    }
  }

  static class SerialPrinter implements TransportListener
  {
    public void message(boolean tx, byte[] data, int timeout)
    {
      System.out.print(tx ? "Sending: " : "Received:");
      for (int i = 0; i < data.length; i++)
      {
        if (i > 0 && (i & 15) == 0)
          System.out.printf("\n         ");
        System.out.printf(" %02x", data[i]);
      }
      System.out.printf("\n");
    }
  }

  static class StringPrinter implements TransportListener
  {
    public void message(boolean tx, byte[] data, int timeout)
    {
      System.out.println((tx ? "Sending:\n" : "Receiving:\n") +
                         new String(data));
    }
  }


  static SerialPrinter serialPrinter;
  static StringPrinter stringPrinter;
  static TransportListener currentListener;

}
