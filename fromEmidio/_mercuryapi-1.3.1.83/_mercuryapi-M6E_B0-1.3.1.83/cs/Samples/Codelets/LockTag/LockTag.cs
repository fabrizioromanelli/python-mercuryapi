using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace LockTag
{
    /// <summary>
    /// Sample program that sets an access password on a tag and
    /// locks its EPC.
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
                    TagReadData[] tagReads;
                    r.ParamSet("/reader/enableStreaming", false);
                    r.ParamSet("/reader/region/id", Reader.Region.NA);
                    // Find a tag to work on
                    tagReads = r.Read(1000);
                    if (0 == tagReads.Length)
                    {
                        Console.WriteLine("No tags found to work on");
                        return;
                    }

                    TagData t = tagReads[0].Tag;

                    // Set the access password of the tag
                    Gen2.WriteData write0 = new Gen2.WriteData(Gen2.Bank.RESERVED, 2, new ushort[] { 0x0, 0x0 });
                    r.ExecuteTagOp(write0, t);

                    Console.WriteLine("Set access password of " + t.ToString() +
                        " to 0x00000000");

                    // Set the access password in the API so that the lock command can succeed
                    r.ParamSet("/reader/gen2/accessPassword", new Gen2.Password(0x0));

                    // Lock the tag. Using the same mask and action bits implies that we are setting (locking) that bit.

                    Gen2.Lock lock0 = new Gen2.Lock(0, new Gen2.LockAction(Gen2.LockAction.EPC_LOCK));
                    r.ExecuteTagOp(lock0, t);
                    Console.WriteLine("Locked EPC of tag " + t.ToString());
                }
            }
            catch (ReaderException re)
            {
                Console.WriteLine("Error: " + re.Message);
            }
        }
    }
}
