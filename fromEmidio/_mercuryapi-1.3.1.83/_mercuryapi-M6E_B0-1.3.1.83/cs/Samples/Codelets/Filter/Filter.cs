using System;
using System.Collections.Generic;
using System.Text;
using ThingMagic;
using System.Threading;

// Sample program that demonstrates different types and uses of TagFilter objects.

namespace Filter
{
    class Program
    {
        static void Main(string[] args)
        {
            // Program setup
            if (args.Length != 1)
            {
                Console.WriteLine("Please provide reader URL, such as:\n"
                                 + "tmr:///com4\n"
                                 + "tmr://my-reader.example.com\n");
                Environment.Exit(1);
            }

            // Create Reader object, connecting to physical device
            try
            {
                Reader r;
                TagReadData[] tagReads, filteredTagReads;
                TagFilter filter;

                r = Reader.Create(args[0]);
                
                //r.Transport += delegate(Object sender, TransportListenerEventArgs e)
                //    {
                //        Console.WriteLine(String.Format(
                //            "{0}: {1} (timeout={2:D}ms)",
                //            e.Tx ? "TX" : "RX",
                //             BytesToString(e.Data, "", ""),
                //            e.Timeout
                //            ));
                //    };

                r.Connect();
                r.ParamSet("/reader/enableStreaming",false);
                r.ParamSet("/reader/region/id", Reader.Region.NA);
                Console.WriteLine(r.ParamGet("/reader/read/plan").ToString());
                // Read the tags in the field
                tagReads = r.Read(500);

                // A TagData object may be used as a filter, for example to
                // perform a tag data operation on a particular tag.
                filter = tagReads[0].Tag;

                // zero out the kill password of the selected tag
                Console.WriteLine("Writing 0x00000000 to kill password of tag {0}\n",
                                  filter.ToString());
                try
                {
                    r.WriteTagMemWords(filter, 0, 0, new ushort[] { 0, 0 });
                }
                catch (ReaderCodeException)
                {
                    // The tag password might not be writable.
                }

                // Filter objects that apply to multiple tags are most useful in
                // narrowing the set of tags that will be read. This is
                // performed by setting a read plan that contains a filter.

                // A TagData with a short EPC will filter for tags whose EPC
                // starts with the same sequence.
                filter = new TagData("AA");
                int[] antennas = new int[] { };
                ReadPlan filteredReadPlan =
                  new SimpleReadPlan(antennas, TagProtocol.GEN2, filter, 1000);
                r.ParamSet("/reader/read/plan", filteredReadPlan);
                Console.WriteLine("Reading tags that begin with {0}\n",
                                  filter.ToString());
                filteredTagReads = r.Read(500);
                foreach (TagReadData tr in filteredTagReads)
                    Console.WriteLine(tr.ToString());

                // A filter can also be an explicit Gen2 Select operation.  For
                // example, this filter matches all Gen2 tags where bits 8-19 of
                // the TID are 0x003 (that is, tags manufactured by Alien
                // Technology).
                // Note: Selection on any memory bank other than EPC is not valid
                // on RQL readers.
                filter = new Gen2.Select(false, Gen2.Bank.TID, 8, 12, new byte[] { 0, 0x30 });
                filteredReadPlan = new SimpleReadPlan(null, TagProtocol.GEN2, filter, 1000);
                r.ParamSet("/reader/read/plan", filteredReadPlan);
                Console.WriteLine("Reading tags with a TID manufacturer of 0x003\n"+filteredReadPlan.ToString());
                filteredTagReads = r.Read(500);
                foreach (TagReadData tr in filteredTagReads)
                    Console.WriteLine(tr.ToString());

                // Filters can also be used to match tags that have already been
                // read. This form can only match on the EPC, as that's the only
                // data from the tag's memory that is contained in a TagData
                // object.
                // Note that this filter has invert=true. This filter will match
                // tags whose bits do not match the selection mask.
                // Also note the offset - the EPC code starts at bit 32 of the
                // EPC memory bank, after the StoredCRC and StoredPC.
                filter = new Gen2.Select(true, Gen2.Bank.EPC, 32, 2, new byte[] { (byte)0xC0 });
                Console.WriteLine("Post-filtering for tags without the top two bits of EPC set\n");
                foreach (TagReadData tr in tagReads) // unfiltered tag reads from the first example
                    if (filter.Matches(tr.Tag))
                        Console.WriteLine(tr.ToString());

                // Shut down reader
                r.Destroy();
            }
            catch (ReaderException re)
            {
                Console.WriteLine("Error: " + re.ToString());
            }
        }
    }
}
