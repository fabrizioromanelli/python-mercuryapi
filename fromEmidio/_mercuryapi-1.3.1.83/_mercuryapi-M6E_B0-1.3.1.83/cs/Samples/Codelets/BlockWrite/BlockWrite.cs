using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace BlockWrite
{
    class BlockWrite
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
                    Gen2.Password pass = new Gen2.Password(0x0);
                    r.ParamSet("/reader/gen2/accessPassword", pass);


                    // BlockWrite and read using ExecuteTagOp

                    Gen2.BlockWrite blockwrite = new Gen2.BlockWrite(Gen2.Bank.USER, 0, new ushort[] { 0xFFF1,0x1122 });
                    r.ExecuteTagOp(blockwrite, null);

                    Gen2.ReadData read = new Gen2.ReadData(Gen2.Bank.USER, 0, 2);
                    ushort [] readData = (ushort[])r.ExecuteTagOp(read, null);

                    foreach (ushort word in readData)
                    {
                        Console.Write(String.Format(" {0:X4}", word));
                    }
                    Console.WriteLine();


                    // BlockWrite and read using embedded read command

                    SimpleReadPlan readplan;
                    TagReadData[] tagreads;

                    blockwrite = new Gen2.BlockWrite(Gen2.Bank.USER, 0, new ushort[] { 0x1234, 0x5678 });
                    readplan = new SimpleReadPlan(null, TagProtocol.GEN2, 
                        //null,
                        new TagData("1234567890ABCDEF"),
                        blockwrite, 0);
                    r.ParamSet("/reader/read/plan", readplan);
                    r.Read(500);

                    readplan = new SimpleReadPlan(null, TagProtocol.GEN2,
                        null,
                        new Gen2.ReadData(Gen2.Bank.USER, 0, 2),
                        0);
                    r.ParamSet("/reader/read/plan", readplan);
                    tagreads = r.Read(500);
                    foreach (TagReadData trd in tagreads)
                    {
                        foreach (byte b in trd.Data)
                        {
                            Console.Write(String.Format(" {0:X2}", b));
                        }

                        Console.WriteLine("    " + trd.ToString());
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
