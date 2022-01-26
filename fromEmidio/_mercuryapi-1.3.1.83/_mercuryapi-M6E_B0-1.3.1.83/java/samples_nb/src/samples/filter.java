/**
 * Sample program that demonstrates different types and uses of
 * TagFilter objects.
 */

// Import the API
package samples;
import com.thingmagic.*;

public class filter
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

      TagReadData[] tagReads,filteredTagReads;
      TagFilter filter;
      r = Reader.create(argv[nextarg]);
      if (trace)
      {
        setTrace(r, new String[] {"on"});
      }
      r.connect();
      r.paramSet("/reader/region/id", Reader.Region.NA);

      // Read the tags in the field
      tagReads = r.read(500);

      // A TagData object may be used as a filter, for example to
      // perform a tag data operation on a particular tag.
      filter = tagReads[0].getTag();

      // zero out the kill password of the selected tag
      System.out.printf("Writing 0x00000000 to kill password of tag %s\n",
                        filter.toString());
      try
      {
        r.writeTagMemWords(filter, 0, 0, new short[] {0, 0});
      }
      catch (ReaderCodeException re)
      {
        // The tag password might not be writable.
      }

      // Filter objects that apply to multiple tags are most useful in
      // narrowing the set of tags that will be read. This is
      // performed by setting a read plan that contains a filter.
      
      // A TagData with a short EPC will filter for tags whose EPC
      // starts with the same sequence.
      filter = new TagData("8E");
      ReadPlan filteredReadPlan = 
        new SimpleReadPlan(null, TagProtocol.GEN2, filter, 1000);
      r.paramSet("/reader/read/plan", filteredReadPlan);
      System.out.printf("Reading tags that begin with %s\n",
                        filter.toString());
      filteredTagReads = r.read(500);
      for (TagReadData tr : filteredTagReads)
        System.out.println(tr.toString());

      // A filter can also be an explicit Gen2 Select operation.  For
      // example, this filter matches all Gen2 tags where bits 8-19 of
      // the TID are 0x003 (that is, tags manufactured by Alien
      // Technology).
      filter = 
        new Gen2.Select(false, Gen2.Bank.TID, 8, 12,
                        new byte[] {0, 0x30});
      filteredReadPlan = 
        new SimpleReadPlan(null, TagProtocol.GEN2, filter, 1000);
      r.paramSet("/reader/read/plan", filteredReadPlan);
      System.out.printf("Reading tags with a TID manufacturer of 0x003\n",
                        filter.toString());
      filteredTagReads = r.read(500);
      for (TagReadData tr : filteredTagReads)
        System.out.println(tr.toString());

      // Filters can also be used to match tags that have already been
      // read. This form can only match on the EPC, as that's the only
      // data from the tag's memory that is contained in a TagData
      // object.
      // Note that this filter has invert=true. This filter will match
      // tags whose bits do not match the selection mask.
      // Also note the offset - the EPC code starts at bit 32 of the
      // EPC memory bank, after the StoredCRC and StoredPC.
      filter = 
        new Gen2.Select(true, Gen2.Bank.EPC, 32, 2,
                        new byte[] {(byte)0xC0});
      System.out.printf("Post-filtering for tags without the top two bits of EPC set\n");
      for (TagReadData tr : tagReads) // unfiltered tag reads from the first example
        if (filter.matches(tr.getTag()))
          System.out.println(tr.toString());

      // Shut down reader
      r.destroy();
    } 
    catch (ReaderException re)
    {
      System.out.println("Error: " + re.getMessage());
    }
  }
}
