using System;
using System.Collections.Generic;
using System.Text;
using ThingMagic;

//Sample program that runs a level 3 serial command

namespace SerialCommand
{
    class Program
    {
        static void Main(string[] args)
        {
            // Program setup
            if (args.Length != 1)
            {
                Console.Write("Please provide reader URL, such as:\n"
                                 + "tmr:///com4\n"
                                 + "tmr://my-reader.example.com\n");
                Environment.Exit(1);
            }

            // Create Reader object, connecting to physical device
            try
            {
                Reader r;

                r = Reader.Create(args[0]);
                r.Connect();
                r.ParamSet("/reader/enableStreaming", false);
                r.ParamSet("/reader/region/id", Reader.Region.NA);

                SerialReader sr = (SerialReader)r;

                // Run a level 3 command
                byte[] ports = sr.CmdGetTxRxPorts();

                Console.WriteLine("TX port: " + ports[0]);
                Console.WriteLine("RX port: " + ports[1]);

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

        
    

