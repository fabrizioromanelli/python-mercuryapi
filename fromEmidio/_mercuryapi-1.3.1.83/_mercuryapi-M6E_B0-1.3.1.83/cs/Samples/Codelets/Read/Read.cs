using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace Read
{
    /// <summary>
    /// Sample program that reads tags for a fixed period of time (500ms)
    /// and prints the tags found.
    /// </summary>
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
                    r.ParamSet("/reader/enableStreaming", false);
                    r.ParamSet("/reader/region/id", Reader.Region.NA);
                    TagReadData[] tagReads;
                    
                    // Read tags
                    tagReads = r.Read(500);
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
    }
}
