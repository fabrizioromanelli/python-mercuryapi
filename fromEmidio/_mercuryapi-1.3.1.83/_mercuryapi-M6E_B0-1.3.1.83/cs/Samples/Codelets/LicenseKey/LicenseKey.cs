using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace LicenseKey
{
    class LicenseKey
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

                    byte[] licenseKey = { };
                    foreach (TagProtocol protocol in ((SerialReader)r).CmdSetProtocolLicenseKey(licenseKey))
                    {
                        Console.WriteLine(protocol.ToString());
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
