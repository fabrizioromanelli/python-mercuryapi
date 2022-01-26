using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace BlockPermaLock
{
    /// <summary>
    /// Sample program that demonstrates the use of BlockPermaLock
    /// </summary>
    class BlockPermaLock
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

                    Gen2.Password pass = new Gen2.Password(0x0);
                    r.ParamSet("/reader/gen2/accessPassword", pass);

                    /************************************************/
                    Gen2.BlockPermaLock blockpermalock1 = new Gen2.BlockPermaLock(0x01, Gen2.Bank.USER, 0x00, 0x01, new ushort[] { 0x0010 });
                    // r.ExecuteTagOp(blockpermalock1, null);

                    Gen2.BlockPermaLock blockpermalock2 = new Gen2.BlockPermaLock(0x00, Gen2.Bank.USER, 0x00, 0x01, null);
                    byte[] lockStatus = (byte[])r.ExecuteTagOp(blockpermalock2, null);

                    foreach (byte i in lockStatus)
                    {
                        Console.WriteLine("Lock Status : {0:x2}", i);
                    } 
                }
            }
            catch (ReaderException re)
            {
                Console.WriteLine("Error: " + re.Message);
            }

        }
    }
}
