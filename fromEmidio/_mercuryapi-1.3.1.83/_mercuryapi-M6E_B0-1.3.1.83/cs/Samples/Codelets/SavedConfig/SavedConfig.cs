using System;
using System.Collections.Generic;
using System.Text;

// Reference the API
using ThingMagic;

namespace SavedConfig
{
    class SavedConfig
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

                    ((SerialReader)r).CmdSetProtocol(TagProtocol.GEN2);
                    ((SerialReader)r).CmdSetUserProfile(SerialReader.SetUserProfileOption.SAVE, SerialReader.SetUserProfileKey.ALL, SerialReader.SetUserProfileValue.CUSTOM_CONFIGURATION); //save all the configurations
                    Console.WriteLine("User profile set option:save all configuration");

                    ((SerialReader)r).CmdSetUserProfile(SerialReader.SetUserProfileOption.RESTORE, SerialReader.SetUserProfileKey.ALL, SerialReader.SetUserProfileValue.CUSTOM_CONFIGURATION);//restore all the configurations
                    Console.WriteLine("User profile set option:restore all configuration");

                    ((SerialReader)r).CmdSetUserProfile(SerialReader.SetUserProfileOption.VERIFY, SerialReader.SetUserProfileKey.ALL, SerialReader.SetUserProfileValue.CUSTOM_CONFIGURATION);//verify all the configurations
                    Console.WriteLine("User profile set option:verify all configuration");

                    /**********  Testing cmdGetUserProfile function ***********/

                    byte[] data1 = new byte[] { 0x67 };
                    byte[] response1;
                    response1 = ((SerialReader)r).cmdGetUserProfile(data1);
                    Console.WriteLine("Get user profile success option:Region");
                    foreach (byte i in response1)
                    {
                        Console.Write(" {0:X2}", i);
                    }
                    Console.WriteLine();

                    byte[] data2 = new byte[] { 0x63 };
                    byte[] response2;
                    response2 = ((SerialReader)r).cmdGetUserProfile(data2);
                    Console.WriteLine("Get user profile success option:Protocol");
                    foreach (byte i in response2)
                    {
                        Console.Write(" {0:X2}", i);
                    }
                    Console.WriteLine();

                    byte[] data3 = new byte[] { 0x06 };
                    byte[] response3;
                    response3 = ((SerialReader)r).cmdGetUserProfile(data3);
                    Console.WriteLine("Get user profile success option:Baudrate");
                    foreach (byte i in response3)
                    {
                        Console.Write(" {0:X2}", i);
                    }
                    Console.WriteLine();

                    ((SerialReader)r).CmdSetUserProfile(SerialReader.SetUserProfileOption.CLEAR, SerialReader.SetUserProfileKey.ALL, SerialReader.SetUserProfileValue.CUSTOM_CONFIGURATION);//reset all the configurations
                    Console.WriteLine("User profile set option:reset all configuration");

                    ((SerialReader)r).CmdSetProtocol(TagProtocol.GEN2);
                    ((SerialReader)r).CmdSetUserProfile(SerialReader.SetUserProfileOption.SAVE, SerialReader.SetUserProfileKey.ALL, SerialReader.SetUserProfileValue.CUSTOM_CONFIGURATION); //save all the configurations
                    Console.WriteLine("User profile set option:save all configuration");
                    ((SerialReader)r).CmdSetUserProfile(SerialReader.SetUserProfileOption.RESTORE, SerialReader.SetUserProfileKey.ALL, SerialReader.SetUserProfileValue.FIRMWARE_DEFAULT); //restore firmware default configuration parameters
                    Console.WriteLine("User profile set option:restore firmware default configuration parameters");


                }
            }
            catch (ReaderException re)
            {
                Console.WriteLine("Error: " + re.Message);
            }
        }
    }
}
