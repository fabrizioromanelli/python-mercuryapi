using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace SerialTime
{
    /// <summary>
    /// Sample program that sets an access password on a tag and
    /// locks its EPC.
    /// </summary>
    class SerialTime
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
                // Create Reader object, but do not connect to physical device.
                // Wrap reader in a "using" block to get automatic
                // reader shutdown (using IDisposable interface).
                using (Reader r = Reader.Create(args[0]))
                {
                    // Add the serial-reader-specific message logger
                    // before connecting, so we can see the initialization.
                    r.Transport += TimestampListener;
                    // Now connect to physical device
                    r.Connect();
                    r.ParamSet("/reader/enableStreaming", false);
                    r.ParamSet("/reader/region/id",Reader.Region.NA);
                    // Read tags
                    TagReadData[] tagReads = r.Read(500);
                    // Print tag reads
                    foreach (TagReadData tr in tagReads)
                        Console.WriteLine(tr.ToString());
                }
            }
            catch (ReaderException re)
            {

                Console.WriteLine("Error: " + re.Message);
            }
        }

        static void TimestampListener(Object sender, TransportListenerEventArgs e)
        {
            Console.Write(String.Format("{0} {1}",
                DateTime.Now.ToString("MM/dd/yyyy hh:mm:ss.fff tt"),
                e.Tx ? "Sending" : "Received"));
            for (int i = 0; i < e.Data.Length; i++)
            {
                if ((i & 15) == 0)
                {
                    Console.WriteLine();
                    Console.Write("  ");
                }
                Console.Write("  " + e.Data[i].ToString("X2"));
            }
            Console.WriteLine();
        }
    }
}
