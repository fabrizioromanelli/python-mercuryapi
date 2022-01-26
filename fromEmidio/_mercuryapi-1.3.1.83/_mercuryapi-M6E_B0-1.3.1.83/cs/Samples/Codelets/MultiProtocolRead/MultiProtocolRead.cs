using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace MultiProtocolRead
{
    class Program
    {
        static void Main(string[] args)
        {
            // Program setup
            if (1 != args.Length)
            {
                Console.WriteLine(String.Join("\r\n", new string[] {
                    "Please provide reader URL, such as:",
                    "tmr:///com4",
                    "tmr://my-reader.example.com",
                }));
                Environment.Exit(1);
            }

            try
            {
                // Create Reader object, connecting to physical device.
                // Wrap reader in a "using" block to get automatic
                // reader shutdown (using IDisposable interface).
                using (Reader r = Reader.Create(args[0]))
                {
                    r.Connect();
                    r.ParamSet("/reader/region/id", Reader.Region.NA);

                    List<ReadPlan> readPlans = new List<ReadPlan>();
                    SimpleReadPlan plan1 = new SimpleReadPlan(null, TagProtocol.GEN2, null, null, 0);
                    SimpleReadPlan plan2 = new SimpleReadPlan(null, TagProtocol.ISO180006B, null, null, 0);
                    SimpleReadPlan plan3 = new SimpleReadPlan(null, TagProtocol.IPX256, null, null, 0);
                    SimpleReadPlan plan4 = new SimpleReadPlan(null, TagProtocol.IPX64, null, null, 0);
                    readPlans.Add(plan1);
                    readPlans.Add(plan2);
                    readPlans.Add(plan3);
                    readPlans.Add(plan4);
                    MultiReadPlan testMultiReadPlan = new MultiReadPlan(readPlans);
                    r.ParamSet("/reader/read/plan", testMultiReadPlan);
                    TagReadData[] tagRead = r.Read(1000);
                    foreach (TagReadData tr in tagRead)
                        Console.WriteLine(String.Format("{0} {1}",
                            tr.Tag.Protocol, tr.ToString()));
                }
            }
            catch (ReaderException re)
            {
                Console.WriteLine("Error: " + re.Message);
            }
        }
    }
}
