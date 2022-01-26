using System;
using System.Collections.Generic;
using System.Text;
// for Thread.Sleep
using System.Threading;

// Reference the API
using ThingMagic;

namespace ReadAsyncFilter
{
    /// <summary>
    /// Sample program that reads tags in the background and prints the
    /// tags found that match a certain filter.
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
                    // Create and add tag listener
                    CountMatchListener cml = new CountMatchListener(0xE2);
                    r.TagRead += cml.TagRead;

                    // Search for tags in the background
                    r.StartReading();
                    Thread.Sleep(500);
                    r.StopReading();

                    r.TagRead -= cml.TagRead;

                    // Print results of search, accumulated in listener object
                    Console.WriteLine("Matching tags: " + cml.Matched);
                    Console.WriteLine("Non-matching tags: " + cml.NonMatched);
                }
            }
            catch (ReaderException re)
            {
                Console.WriteLine("Error: " + re.Message);
                Console.Out.Flush();
            }
        }
    }

    class CountMatchListener
    {
        public int ToMatch;
        public int Matched;
        public int NonMatched;

        public CountMatchListener(int toMatch)
        {
            ToMatch = toMatch;
        }

        public void TagRead(Object sender, TagReadDataEventArgs e)
        {
            // Test first byte of tag EPC
            byte[] epcBytes = e.TagReadData.Tag.EpcBytes;
            if ((0 < epcBytes.Length) && 
                (e.TagReadData.Tag.EpcBytes[0] == ToMatch))
                Matched++;
            else
                NonMatched++;
        }
    }
}
