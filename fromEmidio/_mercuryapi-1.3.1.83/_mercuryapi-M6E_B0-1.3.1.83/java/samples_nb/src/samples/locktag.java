/**
 * Sample program that sets an access password on a tag and locks its EPC.
 */

// Import the API
package samples;
import com.thingmagic.*;

public class locktag
{
static SerialPrinter serialPrinter;
  static StringPrinter stringPrinter;
  static TransportListener currentListener;

  static void usage()
  {
    System.out.printf("Usage: demo reader-uri <command> [args]\n" +
                      "  (URI: 'tmr:///COM1' or 'tmr://astra-2100d3/' " +
                      "or 'tmr:///dev/ttyS0')\n\n" +
                      "Available commands:\n");
    System.exit(1);
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
  public static void main(String argv[])
  {
    // Program setup
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

    // Create Reader object, connecting to physical device
    try
    {

      TagReadData[] tagReads;

      r = Reader.create(argv[nextarg]);
      if (trace)
      {
        setTrace(r, new String[] {"on"});
      }
      r.connect();
      r.paramSet("/reader/region/id", Reader.Region.NA);

      // Find a tag to work on
      tagReads = r.read(500);
      if (tagReads.length == 0)
      {
        System.out.println("No tags found to work on\n");
        return;
      }
      SerialReader sr=(SerialReader)r;
      TagData t = tagReads[0].getTag();
      Gen2.WriteData write0 = new Gen2.WriteData(Gen2.Bank.RESERVED, 2, new short[] { 0x0, 0x0 });
      sr.executeTagOp(write0, t);

      
      System.out.println("Set access password of " + t.toString() +
                         " to 0x88887777");

      // Set the access password in the API so that the lock command can succeed
      r.paramSet("/reader/gen2/accessPassword", new Gen2.Password(0x88887777));

      Gen2.Lock lock0 = new Gen2.Lock(0x88887777, new Gen2.LockAction(Gen2.LockAction.EPC_LOCK));
      sr.executeTagOp(lock0, t);
      System.out.println("Locked EPC of tag " + t.toString());

      // Shut down reader
      r.destroy();
  
    } 
    catch (ReaderException re)
    {
      System.out.println("Error: " + re.getMessage());
    }
  }
}
