using System;
using System.Collections.Generic;
using System.Text;
// for Thread.Sleep
using System.Threading;

// Reference the API
using ThingMagic;

namespace ReadAsyncTrack
{
    /// <summary>
    /// Sample program that reads tags in the background and track tags
    /// that have been seen; only print the tags that have not been seen
    /// before.
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
                    PrintNewListener rl = new PrintNewListener();
                    r.TagRead += rl.TagRead;

                    // Search for tags in the background
                    r.StartReading();
                    Thread.Sleep(10000);
                    r.StopReading();

                    r.TagRead -= rl.TagRead;
                }
            }
            catch (ReaderException re)
            {
                Console.WriteLine("Error: " + re.Message);
                Console.Out.Flush();
            }
        }
    }

    class PrintNewListener
    {
        Dictionary<string, object> SeenTags = new Dictionary<string, object>();

        public void TagRead(Object sender, TagReadDataEventArgs e)
        {
            TagData t = e.TagReadData.Tag;
            string epc = t.EpcString;
            if (!SeenTags.ContainsKey(epc))
            {
                Console.WriteLine("New tag: " + t.ToString());
                SeenTags.Add(epc, null);
            }
        }
    }
}
