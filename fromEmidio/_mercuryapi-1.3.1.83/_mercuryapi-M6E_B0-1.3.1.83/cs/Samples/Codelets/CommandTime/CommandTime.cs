using System;
using System.Collections.Generic;
using System.Text;
using ThingMagic;

namespace CommandTime
{
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length != 1)
            {
                Console.WriteLine("Please provide reader URL, such as:\n"
                       + "tmr:///com4\n"
                       + "tmr://my-reader.example.com\n");
                Environment.Exit(1);      
            }
            try
            {
                Reader r;
                TagReadData[] tagReads;                

                r = Reader.Create(args[0]);
                r.Connect();
                r.ParamSet("/reader/enableStreaming", false);
                r.ParamSet("/reader/region/id", Reader.Region.NA);

                // Read tags, timestamping before and after
                DateTime start = DateTime.Now;
                tagReads = r.Read(1000);
                DateTime end = DateTime.Now;
                TimeSpan timeElapsed = end - start;
                Console.WriteLine("1000ms read took " +
                                   (timeElapsed.TotalMilliseconds) +
                                   " milliseconds and found " +
                                   tagReads.Length +
                                   " tags.");


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
