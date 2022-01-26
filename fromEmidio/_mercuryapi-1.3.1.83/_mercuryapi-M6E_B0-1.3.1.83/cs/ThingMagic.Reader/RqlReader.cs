/*
 * Copyright (c) 2009 ThingMagic, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using System.Text;

namespace ThingMagic
{
    /// <summary>
    /// The RqlReader class is an implementation of a Reader object that 
    /// communicates with a ThingMagic fixed RFID reader via the RQL network protocol.
    /// 
    /// Instances of the RQL class are created with the Reader.create()method with a 
    /// "rql" URI or a generic "tmr" URI that references a network device.
    /// </summary>
    public class RqlReader : Reader
    {
        #region Constants

        private const int ANTENNA_ID  = 0;
        private const int READ_COUNT  = 1;
        private const int ID          = 2;
        private const int FREQUENCY   = 3;
        private const int DSPMICROS   = 4;
        private const int PROTOCOL_ID = 5;
        private const int LQI         = 6;

	    #endregion

        #region Static Fields

        private static string[]          _readFieldNames      = null;
        private static readonly char[]   _selFieldSeps        = new char[] { '|' };
        private static readonly string[] _astraReadFieldNames = "antenna_id read_count id frequency dspmicros protocol_id lqi".Split(new char[] { ' ' });
        private static readonly string[] _m5ReadFieldNames    = "antenna_id read_count id frequency dspmicros protocol_id".Split(new char[] { ' ' });
        private static readonly int[]    _gpioMapping         = new int[] { 0x04, 0x08, 0x10, 0x02, 0x20, 0x40, 0x80, 0x100 };
        
        #endregion

        #region Fields

        private string _hostname;
        private int _portNumber;
        private Socket _socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
        private NetworkStream _stream;
        private StreamReader _streamReader;
        private int _antennaMax;
        private int[] _antennaPorts;
        private int[] _gpiList;
        private int[] _gpoList;
        private string _model;
        private int _rfPowerMax;
        private int _rfPowerMin;
        private string _serialNumber;
        private string _softwareVersion;
        private TagProtocol[] _supportedProtocols;

        /// <summary>
        /// Reader's native TX power setting
        /// </summary>
        private int _txPower;

        #endregion

        #region Nested Classes

        #region MaskedBits

        /// <summary>
        /// Value plus mask; i.e., selectively modify bits within a register, arbitrarily setting them to 0 or 1
        /// </summary>
        private sealed class MaskedBits
        {
            #region Fields

            public int Value;
            public int Mask; 

            #endregion

            #region Construction

            public MaskedBits()
            {
                Value = 0;
                Mask = 0;
            }

            #endregion            
        }

        #endregion 

        #region Gen2KillArgs

        private sealed class Gen2KillArgs
        {
            #region Fields

            public TagFilter Target;
            public UInt32     Password; 

            #endregion

            #region Construction

            public Gen2KillArgs(TagFilter target, TagAuthentication password)
            {
                if (null == password)
                    Password = 0;

                else if (password is Gen2.Password)
                {
                    Target = target;
                    Password = ((Gen2.Password) password).Value;
                }

                else
                    throw new ArgumentException("Unsupported TagAuthentication: " + password.GetType().ToString());
            }

            #endregion
        }

        #endregion

        #region Gen2LockArgs

        private sealed class Gen2LockArgs
        {
            #region Fields

            public TagFilter      Target;
            public Gen2.LockAction Action;

            #endregion

            #region Construction

            public Gen2LockArgs(TagFilter target, Gen2.LockAction action)
            {
                Target = target;
                Action = action;
            }

            #endregion
        }

        #endregion

        #region WriteTagArgs

        private sealed class WriteTagArgs
        {
            #region Fields

            public TagFilter Target;
            public TagData    Epc;

            #endregion

            #region Construction

            public WriteTagArgs(TagFilter target, TagData epc)
            {
                Target = target;
                Epc = epc;
            }

            #endregion
        }
        
        #endregion

        #endregion

        #region Construction

        /// <summary>
        /// Connect to RQL reader on default port (8080)
        /// </summary>
        /// <param name="host">_hostname of RQL reader</param>
        /// <param name="port">Port Number of RQL reader</param>        
        public RqlReader(string host, int port)
        {
            if (_portNumber < 0)
                _portNumber = 8080;

            _hostname   = host;
            _portNumber = 8080;

            ParamAdd(new Setting("/reader/transportTimeout", typeof(int), 10000, true));
        }

        #endregion

        #region Initialization Methods

        #region GetHostAddress

        private IPAddress GetHostAddress(string host)
        {
            IPHostEntry he = Dns.GetHostEntry(host);
            return he.AddressList[0];
        }

        #endregion

        #region InitParams

        private void InitParams()
        {
            // TODO: ParamClear breaks pre-connect params.
            // ParamClear was orginally intended to reset the parameter table
            // after a firmware update, removing parameters that no longer exist
            // in the new firmware.  Deprioritize post-upgrade behavior for now.
            // --Harry Tsai 2009 Jun 26
            //ParamClear();
            // It's okay if this doesn't get set to anything other than NONE;
            // since the user doesn't need to set the region, the connection
            // shouldn't fail just because the region is unknown.
            Reader.Region region = Reader.Region.UNSPEC;

            String regionName = GetField("regionName", "params").ToUpper();
            if (regionName.Equals("US"))
                region = Reader.Region.NA;
            else if (regionName.Equals("KR"))
            {
                String regionVersion = (string)GetField("region_version", "params");
                if (regionVersion.Equals("1") || regionVersion.Equals(""))
                    region = Reader.Region.KR;
                else if (regionVersion.Equals("2"))
                    region = Reader.Region.KR2;
            }
            else if (regionName.Equals("JP"))
                region = Reader.Region.JP;
            else if (regionName.Equals("CN"))
                region = Reader.Region.PRC;
            else if (regionName.Equals("IN"))
                region = Reader.Region.IN;
            else if (regionName.Equals("EU"))
            {
                String regionVersion = (string) GetField("region_version", "params");
                if (regionVersion.Equals("1") || regionVersion.Equals(""))
                    region = Reader.Region.EU;
                else if (regionVersion.Equals("2"))
                    region = Reader.Region.EU2;
                else if (regionVersion.Equals("3"))
                    region = Reader.Region.EU3;
            }
            ParamAdd(new Setting("/reader/region/supportedRegions", typeof(Region[]), new Region[] { region }, false));
            ParamAdd(new Setting("/reader/region/id", typeof(Region), region, true,
                delegate(Object val)
                {
                    return val;
                },
                delegate(Object val)
                {
                    if ((Region) val != (Region) (ParamGet("/reader/region/id")))
                    {
                        throw new ArgumentException("Region not supported");
                    }
                    return val;
                }));
            ParamAdd(new Setting("/reader/antenna/portList", typeof(string), _antennaPorts, false));
            ParamAdd(new Setting("/reader/antenna/connectedPortList", typeof(int[]), null, false,
                delegate(Object val)
                {
                    return _antennaPorts;
                    // TODO: Get RQL to implement connected antenna query
                },
                null));
            ParamAdd(new Setting("/reader/gen2/q", typeof(Gen2.Q), null, true,
                delegate(Object val)
                {
                    int initq = int.Parse(GetField("gen2InitQ", "params"));
                    int minq = int.Parse(GetField("gen2MinQ", "params"));
                    int maxq = int.Parse(GetField("gen2MaxQ", "params"));
                    if ((minq == initq) && (initq == maxq))
                        return new Gen2.StaticQ((byte) initq);
                    else
                        return new Gen2.DynamicQ();
                },
                delegate(Object val)
                {
                    if (val is Gen2.StaticQ)
                    {
                        Gen2.StaticQ q = (Gen2.StaticQ) val;
                        string qval = q.InitialQ.ToString("D");
                        CmdSetParam("gen2InitQ", qval);
                        CmdSetParam("gen2MinQ", qval);
                        CmdSetParam("gen2MaxQ", qval);
                    }
                    else
                    {
                        CmdSetParam("gen2MinQ", "2");
                        CmdSetParam("gen2MaxQ", "6");
                    }

                    int initq = int.Parse(GetField("gen2InitQ", "params"));
                    int minq = int.Parse(GetField("gen2MinQ", "params"));
                    int maxq = int.Parse(GetField("gen2MaxQ", "params"));
                    if ((minq == initq) && (initq == maxq))
                        return new Gen2.StaticQ((byte) initq);
                    else
                        return new Gen2.DynamicQ();
                }));
            ParamAdd(new Setting("/reader/gen2/session", typeof(Gen2.Session), null, true,
                delegate(Object val)
                {
                    string rsp = GetField("gen2Session", "params");
                    int s = int.Parse(rsp);
                    switch (s)
                    {
                        // -1 is Astra's way of saying "Use the module's value."
                        // That value is dependent on the user mode, so check it.
                        case -1:
                            int mode = int.Parse(GetField("userMode", "params"));
                            if (mode == 3) // portal mode
                                return Gen2.Session.S1;
                            else
                                return Gen2.Session.S0;
                        case 0: return Gen2.Session.S0;
                        case 1: return Gen2.Session.S1;
                        case 2: return Gen2.Session.S2;
                        case 3: return Gen2.Session.S3;
                    }
                    throw new ReaderParseException("Unknown Gen2 session value " + s.ToString());
                },
                delegate(Object val) { SetField("params.gen2Session", (int) val); return val; }));
            ParamAdd(new Setting("/reader/gen2/target", typeof(Gen2.Target), null, true,
                delegate(Object val)
                {
                    string rsp = GetField("gen2Target", "params");
                    int rval = int.Parse(rsp);
                    Gen2.Target tgt;
                    switch (rval)
                    {
                        case 0: tgt = Gen2.Target.AB; break;
                        case 1: tgt = Gen2.Target.BA; break;
                        case -1:
                        case 2: tgt = Gen2.Target.A; break;
                        case 3: tgt = Gen2.Target.B; break;
                        default:
                            throw new ReaderParseException("Unrecognized Gen2 target: " + rval.ToString());
                    }
                    return tgt;
                },
                delegate(Object val)
                {
                    Gen2.Target tgt = (Gen2.Target) val;
                    int tnum;
                    switch (tgt)
                    {
                        case Gen2.Target.AB: tnum = 0; break;
                        case Gen2.Target.BA: tnum = 1; break;
                        case Gen2.Target.A: tnum = 2; break;
                        case Gen2.Target.B: tnum = 3; break;
                        default:
                            throw new ArgumentException("Unrecognized Gen2.Target " + val.ToString());
                    }
                    CmdSetParam("gen2Target", String.Format("{0:D}", tnum)); return tgt;
                }));
            ParamAdd(new Setting("/reader/gpio/inputList", typeof(int[]), _gpiList, false));
            ParamAdd(new Setting("/reader/gpio/outputList", typeof(int[]), _gpoList, false));
            ParamAdd(new Setting("/reader/version/hardware", typeof(string), "Unknown", false));
            // TODO: Implement /reader/version/hardware for real
            ParamAdd(new Setting("/reader/version/model", typeof(string), _model, false));
            SettingFilter powerSetFilter = delegate(Object val)
            {
                int power = (int) val;
                if (power < _rfPowerMin) { throw new ArgumentException(String.Format("Requested power ({0:D}) too low (RFPowerMin={1:D}cdBm)", power, _rfPowerMin)); }
                if (power > _rfPowerMax) { throw new ArgumentException(String.Format("Requested power ({0:D}) too high (RFPowerMax={1:D}cdBm)", power, _rfPowerMax)); }
                return power;
            };
            ParamAdd(new Setting("/reader/radio/readPower", typeof(int), _txPower, true, null, powerSetFilter));
            ParamAdd(new Setting("/reader/read/plan", typeof(ReadPlan), new SimpleReadPlan(), true, null,
                delegate(Object val)
                {
                    if ((val is SimpleReadPlan) || (val is MultiReadPlan)) { return val; }
                    else { throw new ArgumentException("Unsupported /reader/read/plan type: " + val.GetType().ToString() + "."); }
                }));
            ParamAdd(new Setting("/reader/radio/powerMax", typeof(int), _rfPowerMax, false));
            ParamAdd(new Setting("/reader/radio/powerMin", typeof(int), _rfPowerMin, false));
            ParamAdd(new Setting("Serial", typeof(string), _serialNumber, false));
            ParamAdd(new Setting("/reader/version/software", typeof(string), _softwareVersion, false));
            ParamAdd(new Setting("/reader/version/supportedProtocols", typeof(TagProtocol[]), _supportedProtocols, false));
            ParamAdd(new Setting("/reader/tagop/antenna", typeof(int), GetFirstConnectedAntenna(), true,
                null,
                delegate(Object val) { return ValidateAntenna((int) val); }));
            ParamAdd(new Setting("/reader/tagop/protocol", typeof(TagProtocol), TagProtocol.GEN2, true,
                null,
                delegate(Object val) { return ValidateProtocol((TagProtocol) val); }));
            ParamAdd(new Setting("/reader/radio/writePower", typeof(int), _txPower, true, null, powerSetFilter));
        }

        #endregion

        #region ProbeHardware

        private void ProbeHardware()
        {
            _antennaMax = int.Parse(GetField("reader_available_antennas", "params"));
            _antennaPorts = new int[_antennaMax];

            for (int i = 0 ; i < _antennaPorts.Length ; i++)
                _antennaPorts[i] = i + 1;

            _serialNumber = GetField("reader_serial", "params");
            _softwareVersion = GetField("version", "settings");

            switch (_softwareVersion.Substring(0, 2))
            {
                case "2.":
                    _model = "M5";
                    _gpiList = new int[] { 3, 4 };
                    _gpoList = new int[] { 0, 1, 2, 5 };
                    _readFieldNames = _m5ReadFieldNames;
                    _rfPowerMax = 3250;
                    _rfPowerMin = 0;
                    //This statement is moved up from the section after the switch statement in ProbeHardware()
                    //method. The reason being there is a bug in Astra firmware which gives an error when supported
                    //protocols is queried from the reader. Once the bug is fixed, this statement must move back to 
                    //the original location.
                    _supportedProtocols = ParseProtocolList(GetField("supported_protocols", "settings"));
                    break;
                case "4.":
                    _model = "Astra";
                    _gpiList = new int[] { 3, 4, 6, 7 };
                    _gpoList = new int[] { 0, 1, 2, 5 };
                    _readFieldNames = _astraReadFieldNames;
                    _rfPowerMax = 3000;
                    _rfPowerMin = 0;
                    //Supported Protocols is hardcoded to Gen2 since the Astra supports only Gen2. There is a bug 
                    //in the Astra firmware that gives an error when supported protocols are queries from the reader.
                    //Once the bug is fixed, this hardcoded statement must change back to:
                    //_supportedProtocols = ParseProtocolList(GetField("supported_protocols", "settings"));
                    _supportedProtocols = new TagProtocol[] { TagProtocol.GEN2 };
                    break;
                default:
                    _model = "<unknown>";
                    break;
            }


            _txPower = int.Parse(GetField("tx_power", "saved_settings"));

        }
        #endregion

        #endregion

        #region Connect


        /// <summary>
        /// Connect reader object to device.
        /// If object already connected, then do nothing.
        /// </summary>

        public override void Connect()
        {
            if( !_socket.Connected )
            {
                try
                {
                    _socket.Connect(new IPEndPoint(GetHostAddress(_hostname), _portNumber));

                    _stream       = new NetworkStream(_socket, FileAccess.ReadWrite, true);
                    _streamReader = new StreamReader(_stream, Encoding.ASCII);
                }
                catch (SocketException ex)
                {
                    throw new ReaderCommException(ex.Message);
                }
                catch (IOException ex)
                {
                    throw new ReaderCommException(ex.Message);
                }

                ProbeHardware();
                InitParams();
            }
        }

        #endregion

        #region Destroy

/// <summary>
        /// Shuts down the connection with the reader device.
        /// </summary>

        public override void Destroy()
        {
            DestroyGivenRead();

            _streamReader.Close();
            _stream.Close();
            _socket.Close();
        }

        #endregion

        #region Parameter Methods

        #region CmdGetParam

        /// <summary>
        /// Get RQL parameter
        /// </summary>
        /// <param name="field">RQL parameter name</param>
        /// <returns>RQL parameter value</returns>
        public string CmdGetParam(string field)
        {
            return GetFieldInternal(field, ParamTable(field));
        }

        #endregion

        #region CmdSetParam

        /// <summary>
        /// Set RQL parameter
        /// </summary>
        /// <param name="field">RQL parameter name</param>
        /// <param name="value">RQL parameter value</param>
        public void CmdSetParam(string field, string value)
        {
            string table = ParamTable(field);
            SetField(String.Format("{0}.{1}", table, field), value);
        }

        #endregion

        #endregion

        #region Read Methods

        #region StartReading

/// <summary>
        /// Start reading RFID tags in the background. The tags found will be
        /// passed to the registered read listeners, and any exceptions that
        /// occur during reading will be passed to the registered exception
        /// listeners. Reading will continue until stopReading() is called.
        /// </summary>

        public override void StartReading()
        {
            StartReadingGivenRead();
        }

        #endregion

        #region StopReading

/// <summary>
        /// Stop reading RFID tags in the background.
        /// </summary>

        public override void StopReading()
        {
            StopReadingGivenRead();
        }

        #endregion

        #region Read

        /// <summary>
        /// Read RFID tags for a fixed duration.
        /// </summary>
        /// <param name="milliseconds">the read timeout</param>
        /// <returns>the read tag data collection</returns>

        public override TagReadData[] Read(int milliseconds)
        {
            ReadPlan          rp    = (ReadPlan) ParamGet("/reader/read/plan");
            List<TagReadData> reads = new List<TagReadData>();

            PrepForSearch();

            ReadInternal(rp, milliseconds,ref reads);

            return reads.ToArray();
        }

        #endregion

        #region ReadInternal

        private void ReadInternal(ReadPlan rp, int milliseconds,ref List<TagReadData> reads)
        {
            if (rp is MultiReadPlan)
            {
                MultiReadPlan mrp = (MultiReadPlan) rp;

                foreach (ReadPlan r in mrp.Plans)
                {
                    int subtimeout = (int) milliseconds * r.Weight / mrp.TotalWeight;
                    subtimeout = Math.Min(subtimeout, UInt16.MaxValue);
                    ReadInternal(r, subtimeout,ref reads);
                }
            }
            else if (rp is SimpleReadPlan)
            {
                List<string> wheres = new List<string>();
                wheres.AddRange(ReadPlanToWhereClause(rp));
                wheres.AddRange(TagFilterToWhereClause(((SimpleReadPlan) rp).Filter));
                DateTime baseTime = DateTime.Now;
                String[] rows = Select(_readFieldNames, "tag_id", wheres, milliseconds);

                foreach (string row in rows)
                {
                    String[] fields = row.Split(_selFieldSeps);

                    if (_readFieldNames.Length == fields.Length)
                    {
                        byte[] epccrc = ByteFormat.FromHex(fields[ID].Substring(2));
                        byte[] epc = new byte[epccrc.Length - 2];
                        Array.Copy(epccrc, epc, epc.Length);
                        byte[] crc = new byte[2];
                        Array.Copy(epccrc, epc.Length, crc, 0, 2);

                        TagProtocol proto = CodeToProtocol(fields[PROTOCOL_ID]);
                        string idfield = fields[ID];
                        TagData tag = null;

                        switch (proto)
                        {
                            case TagProtocol.GEN2:
                                tag = new Gen2.TagData(epc, crc);
                                break;
                            case TagProtocol.ISO180006B:
                                tag = new Iso180006b.TagData(epc, crc);
                                break;
                            default:
                                throw new ReaderParseException("Unknown protocol code " + fields[PROTOCOL_ID]);
                        }

                        int antenna = int.Parse(fields[ANTENNA_ID]);
                        TagReadData tr = new TagReadData();
                        tr._tagData = tag;
                        tr._antenna = antenna;
                        tr._baseTime = baseTime;
                        tr._frequency = int.Parse(fields[FREQUENCY]);
                        tr.ReadCount = int.Parse(fields[READ_COUNT]);
                        tr._readOffset = int.Parse(fields[DSPMICROS]) / 1000;

                        if ("M5" != _model)
                            tr._lqi = int.Parse(fields[LQI]);

                        reads.Add(tr);
                    }
                }
            }
            else
                throw new ArgumentException("Unrecognized /reader/read/plan type " + typeof(ReadPlan).ToString());
        }

        #endregion

        #region PrepForSearch

        private void PrepForSearch()
        {
            SetTxPower((int) ParamGet("/reader/radio/readPower"));
        }

        #endregion

        #region SetTxPower

        private void SetTxPower(int power)
        {
            if (power != _txPower)
            {
                Query(String.Format("UPDATE saved_settings SET tx_power={0:D}", power));
            }
        }

        #endregion

        #endregion

        #region GPIO Methods

        #region GpiGet

        /// <summary>
        /// Get the state of all of the reader's GPI pins. 
        /// </summary>
        /// <returns>array of GpioPin objects representing the state of all input pins</returns>
        public override GpioPin[] GpiGet()
        {
            List<String> wheres = new List<String>();
            wheres.Add("mask=0xffffffff");
            int inputState = (int)IntUtil.StrToLong(GetField("data", "io", wheres));

            int[] pins = (int[])ParamGet("/reader/gpio/inputList");
            List<GpioPin> pinvals = new List<GpioPin>();
            foreach (int pin in pins)
            {
                bool state = (0 != (inputState & GetPinMask(pin)));
                pinvals.Add(new GpioPin(pin, state));
            }
            return pinvals.ToArray();
        }

        #endregion

        #region GpoSet

        /// <summary>
        /// Set the state of some GPO pins.
        /// </summary>
        /// <param name="state">array of GpioPin objects</param>
        public override void GpoSet(ICollection<GpioPin> state)
        {
            MaskedBits mb = new MaskedBits();
            int[] valids = (int[])ParamGet("/reader/gpio/outputList");
            foreach (GpioPin gp in state)
            {
                int pin = gp.Id;
                if (IsMember<int>(pin, valids))
                {
                    int pinmask = GetPinMask(pin);
                    mb.Mask &= pinmask;
                    if (gp.High) { mb.Value &= mb.Mask; }
                }
            }
            SetField("io.data", mb);
        }

        #endregion

        #region GetPinMask

        private static int GetPinMask(int gpioPin)
        {
            return _gpioMapping[gpioPin];
        }

        #endregion

        #endregion

        #region Tag Operation Methods

        #region ExecuteTagOp
        /// <summary>
        /// execute a TagOp
        /// </summary>
        /// <param name="tagOP">Tag Operation</param>
        /// <param name="target">Tag filter</param>
        ///<returns>the return value of the tagOp method if available</returns>

        public override Object ExecuteTagOp(TagOp tagOP, TagFilter target)
        {
            return null;
        }

        #endregion

        #region KillTag

        /// <summary>
        /// Kill a tag. The first tag seen is killed.
        /// </summary>
        /// <param name="target">the tag target</param>
        /// <param name="password">the kill password</param>
        public override void KillTag(TagFilter target, TagAuthentication password)
        {
            PrepForTagop();
            SetField("tag_id.killed", new Gen2KillArgs(target, password));
        }

        #endregion

        #region LockTag

        /// <summary>
        /// Perform a lock or unlock operation on a tag. The first tag seen
        /// is operated on - the singulation parameter may be used to control
        /// this. Note that a tag without an access password set may not
        /// accept a lock operation or remain locked.
        /// </summary>
        /// <param name="target">the tag target to operate on</param>
        /// <param name="action">the tag lock action</param>
        public override void LockTag(TagFilter target, TagLockAction action)
        {
            PrepForTagop();
            TagProtocol protocol = (TagProtocol) ParamGet("/reader/tagop/protocol");

            if (action is Gen2.LockAction)
            {
                if (TagProtocol.GEN2 != protocol)
                {
                    throw new ArgumentException(String.Format(
                        "Gen2.LockAction not compatible with protocol {0}",
                        protocol.ToString()));
                }

                Gen2.LockAction la = (Gen2.LockAction) action;

                // TODO: M4API makes a distinction between locking tag ID and data.
                // Locking tag_id allows access to only the EPC, TID and password banks.
                // Locking tag_data allows access to only the User bank.
                // Ideally, M4API would stop making the distinction, since Gen2 has a unified locking model.
                // In the meantime, just lock tag_id, since it covers more and tags with user memory are still uncommon.

                if ((la.Mask & 0x3FC) != 0)
                    SetField("tag_id.locked", new Gen2LockArgs(target, (Gen2.LockAction) action));

                else if ((la.Mask & 0x3) != 0)
                    SetField("tag_data.locked", new Gen2LockArgs(target, (Gen2.LockAction) action));
            }
            else
                throw new ArgumentException("LockTag does not support this type of TagLockAction: " + action.ToString());
        }

        #endregion

        #region ReadTagMemBytes

        /// <summary>
        /// Read data from the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag target to operate on</param>
        /// <param name="bank">the tag memory bank</param>
        /// <param name="byteAddress">the reading starting byte address</param>
        /// <param name="byteCount">the bytes to read</param>
        /// <returns>the bytes read</returns>

        public override byte[] ReadTagMemBytes(TagFilter target, int bank, int byteAddress, int byteCount)
        {
            PrepForTagop();
            int wordAddress = byteAddress / 2;
            int wordCount = ByteConv.WordsPerBytes(byteCount);
            List<string> wheres = new List<string>();
            wheres.Add(String.Format("mem_bank={0:D}", bank));
            wheres.Add(String.Format("block_number={0:D}", wordAddress));
            wheres.Add(String.Format("block_count={0:D}", wordCount));
            wheres.AddRange(MakeTagopWheres(target));
            string response = Select(new string[] { "data" }, "tag_data", wheres, -1)[0];

            // Skip "0x" prefix
            int charOffset = 2;
            // Correct for word start boundary
            int byteOffset = byteAddress - (wordAddress * 2);
            charOffset += byteOffset * 2;
            int charLength = byteCount * 2;
            string byteStr = response.Substring(charOffset, charLength);

            return ByteFormat.FromHex(byteStr);
        }

        /// <summary>
        /// Read data from the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag target to operate on</param>
        /// <param name="bank">the tag memory bank</param>
        /// <param name="wordAddress">the read starting word address</param>
        /// <param name="wordCount">the number of words to read</param>
        /// <returns>the read words</returns>

        public override ushort[] ReadTagMemWords(TagFilter target, int bank, int wordAddress, int wordCount)
        {
            return ReadTagMemWordsGivenReadTagMemBytes(target, bank, wordAddress, wordCount);
        }

        #endregion

        #region WriteTag

        /// <summary>
        /// Write a new ID to a tag.
        /// </summary>
        /// <param name="target">the tag target to operate on</param>
        /// <param name="epc">the tag ID to write</param>

        public override void WriteTag(TagFilter target, TagData epc)
        {
            PrepForTagop();
            SetField("tag_id.id", new WriteTagArgs(target, epc));
        }

        #endregion

        #region WriteTagMemBytes

        /// <summary>
        /// Write data to the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag target to operate on</param>
        /// <param name="bank">the tag memory bank</param>
        /// <param name="byteAddress">the starting memory address to write</param>
        /// <param name="data">the data to write</param>
        public override void WriteTagMemBytes(TagFilter target, int bank, int byteAddress, ICollection<byte> data)
        {
            PrepForTagop();
            int wordAddress = byteAddress / 2;

            if (wordAddress * 2 != byteAddress) 
                throw new ArgumentException("Byte write address must be even");

            int byteCount = data.Count;
            int wordCount = ByteConv.WordsPerBytes(byteCount);

            if (wordCount * 2 != byteCount)
                throw new ArgumentException("Byte write length must be even");

            ushort[] wordData = ByteConv.ToU16s(data);

            WriteTagMemWords(target, bank, wordAddress, wordData);
        }

        #endregion

        #region WriteTagMemWords

        /// <summary>
        /// Write data to the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag target to operate on</param>
        /// <param name="bank">the tag memory bank</param>
        /// <param name="address">the memory address to write</param>
        /// <param name="data">the data to write</param>
        public override void WriteTagMemWords(TagFilter target, int bank, int address, ICollection<ushort> data)
        {
            List<string> wheres = new List<string>();
            wheres.Add(String.Format("mem_bank={0:D}", bank));
            wheres.Add(String.Format("block_number={0:D}", address));
            wheres.AddRange(MakeTagopWheres(target));
            wheres.AddRange(MakeAccessPasswordWheres());

            SetField("tag_data.data", data, wheres);
        }

        #endregion

        #region PrepForTagop

        private void PrepForTagop()
        {
            SetTxPower((int) ParamGet("/reader/radio/writePower"));
            // Antenna and protocol are handled in the tagop WHERE clause generator
        }

        #endregion

        #endregion

        #region Firmware Methods

        #region FirmwareLoad
        /// <summary>
        /// Loads firmware on the Reader.
        /// </summary>
        /// <param name="firmware">Firmware IO stream</param>
        [MethodImpl(MethodImplOptions.Synchronized)]
        public override void FirmwareLoad(Stream firmware)
        {
            //URL u;
            //HttpURLConnection uc;
            //String encoding;
            //ClientHttpRequest c;
            //InputStream replyStream;
            //BufferedReader replyReader;
            //StringBuilder replyBuf;
            //String reply;
            //char buf[] = new char[1024];
            //int len;


            //// DEBUG: Try a basic GET
            //{
            //    HttpWebRequest req = MakeWebReq("/cgi-bin/status.cgi");
            //    HttpWebResponse rsp = (HttpWebResponse)req.GetResponse();
            //    string body = WebRespToString(rsp);
            //    ShowBody(req, body);
            //}

            //// DEBUG: Try a simple POST
            //{
            //    HttpWebRequest req = MakeWebReq("/cgi-bin/diagnostics.cgi?log");
            //    WebPost(req, "application/x-www-form-urlencoded", "submit=\"View Log\"");
            //    HttpWebResponse rsp = (HttpWebResponse)req.GetResponse();
            //    string body = WebRespToString(rsp);
            //    ShowBody(req, body);
            //}

            // Assume that a system with an RQL interpreter has the standard
            // web interface and password. This isn't really an RQL operation,
            // but it will work most of the time
            HttpWebRequest req = MakeWebReq("/cgi-bin/firmware.cgi");

            // Filename "firmware.tmfw" is arbitrary -- server always ignores
            // Fixed reader firmware is huge (~7MB).
            // Increase the default timeout to allow sufficient upload time
            // (Default timeout is 100 seconds)
            req.Timeout = 10 * 60 * 1000;

            WebUploadFile(req, firmware, "firmware.tmfw");
            HttpWebResponse rsp = (HttpWebResponse) req.GetResponse();
            string response = WebRespToString(rsp);

            // If asked to confirm using an older firmware, respond
            if (0 <= response.IndexOf("replace the new firmware with older firmware"))
            {
                WebRespToString((HttpWebResponse) MakeWebReq(
                    "/cgi-bin/firmware.cgi?confirm=true&DOWNGRADE=Continue"
                    ).GetResponse());
            }
            // If firmware load succeeded, reboot to make it take effect
            if (0 <= response.IndexOf("Firmware update complete"))
            {
                // Restart reader
                HttpWebRequest rebootReq = MakeWebReq("/cgi-bin/reset.cgi");
                WebPost(rebootReq, "dummy=dummy");
                Destroy();
                _socket = null;

                // TODO: Use a more sophisticated method to detect when the reader is ready again
                System.Threading.Thread.Sleep(90 * 1000);

                _socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                Connect();
            }
            else
                throw new ReaderException("Firmware update failed");
        }

        #endregion

        #endregion

        #region RQL Methods

        #region Query

        private string[] Query(string cmd)
        {
            return Query(cmd, 0, false);
        }

        private string[] Query(string cmd, bool permitEmptyResponse)
        {
            return Query(cmd, 0, permitEmptyResponse);
        }

        /// <summary>
        /// Perform RQL query
        /// </summary>
        /// <param name="cmd">Text of query to send</param>
        /// <param name="cmdTimeout">Number milliseconds to allow query to execute.
        /// The ultimate comm timeout will be this number plus the transportTimeout.</param>
        /// <param name="permitEmptyResponse">If true, then first line of RQL response may be empty -- keep looking for response terminator (which is also an empty line.)
        /// If false, stop receiving response at first empty line.</param>
        /// <returns>Query response, parsed into individual lines.
        /// Includes terminating empty line.</returns>
        [MethodImpl(MethodImplOptions.Synchronized)]
        private string[] Query(string cmd, int cmdTimeout, bool permitEmptyResponse)
        {
            if (!cmd.EndsWith(";\n"))
                cmd += ";\n";

            byte[] bytesToSend = Encoding.ASCII.GetBytes(cmd);
#if WindowsCE
            int sendTimeout = 0;
            int recvTimeout = 0;
#else
            int transportTimeout = (int) ParamGet("/reader/transportTimeout");
            int sendTimeout = _socket.SendTimeout = transportTimeout;
            int recvTimeout = _socket.ReceiveTimeout = transportTimeout + cmdTimeout;
#endif

            OnTransport(true, bytesToSend, sendTimeout);

            try
            {
                _socket.Send(bytesToSend);
            }
            catch (SocketException ex)
            {
                throw new ReaderCommException(ex.Message, bytesToSend);
            }

            List<string> response = new List<string>();

            try
            {
                string line = null;

                while (true)
                {
                    line = _streamReader.ReadLine();

                    if (null != line)
                    {
                        response.Add(line);

                        // Stop if that was the end
                        if ((0 == line.Length) && (false == permitEmptyResponse))
                            break;

                        // Always turn off permitEmptyResponse after the first line
                        permitEmptyResponse = false;
                    }
                }
            }
            catch (SocketException ex)
            {
                throw new ReaderCommException(ex.Message, StringsToBytes(response));
            }
            catch (IOException ex)
            {
                throw new ReaderCommException(ex.Message, StringsToBytes(response));
            }

            if (response[0].StartsWith("Error"))
                throw new ReaderException(response[0]);

            OnTransport(false, StringsToBytes(response), recvTimeout);

            return response.ToArray();
        }

        #endregion

        #region Select

        private string[] Select(string[] fields, string table, List<string> wheres, int timeout)
        {
            return Query(MakeSelect(fields, table, wheres, timeout), timeout, false);
        }

        #endregion

        #region MakeSelect

        private string MakeSelect(string field, string table)
        {
            return MakeSelect(new string[] { field }, table, null, -1);
        }

        private string MakeSelect(string[] fields, string table)
        {
            return MakeSelect(fields, table, null, -1);
        }

        private string MakeSelect(string[] fields, string table, List<string> wheres, int timeout)
        {
            List<string> words = new List<string>();

            words.Add("SELECT");
            words.Add(String.Join(",", fields));
            words.Add("FROM");
            words.Add(table);
            words.Add(WheresToRql(wheres));

            if (-1 < timeout)
                words.Add("SET time_out=" + timeout.ToString());

            return String.Join(" ", words.ToArray());
        }

        #endregion

        #region GetField

        /// <summary>
        /// Retrieve an RQL parameter (row from a single-field table).
        /// Throws exception on empty values -- use only for parameters that aren't allowed to be empty
        /// (e.g., Reader parameters)
        /// </summary>
        /// <param name="name">Parameter name</param>
        /// <param name="table">Table name</param>
        /// <returns>Parameter value, or throws ReaderParseException if field has empty value.</returns>
        private string GetField(string name, string table)
        {
            string value = GetFieldInternal(name, table);

            if (0 == value.Length)
                throw new FeatureNotSupportedException("No field " + name + " in table " + table);

            return value;
        }

        #endregion

        #region GetFieldInternal

        private string GetFieldInternal(string name, string table)
        {
            return Query(MakeSelect(name, table), true)[0];
        }

        #endregion

        #region GetField

        private string GetField(string name, string table, List<string> wheres)
        {
            return Select(new string[] { name }, table, wheres, -1)[0];
        }

        #endregion

        #region SetField

        private string[] SetField(string tabledotname, Object value)
        {
            return SetField(tabledotname, value, null);
        }

        private string[] SetField(string tabledotname, Object value, List<string> wheres)
        {
            string[] fields = tabledotname.Split(new char[] { '.' });
            string table = fields[0];
            string name = fields[1];
            string valstr = SetFieldValueFilter(value);
            List<string> phrases = new List<string>();

            phrases.Add(String.Format("UPDATE {0} SET {1}={2}", table, name, valstr));

            if ((null != wheres) && (0 < wheres.Count))
                phrases.Add(WheresToRql(wheres));

            return Query(String.Join(" ", phrases.ToArray()));
        }

        #endregion

        #region SetFieldValueFilter

        private string SetFieldValueFilter(Object value)
        {
            if (value is int)
                return String.Format("'{0:D}'", (int) value);

            else if (value is byte[])
            {
                // No quotes around hex strings
                return String.Format("0x{0}", ByteFormat.ToHex((byte[]) value));
            }
            else if ((value is UInt16[]) || (value is ushort[]))
            {
                // No quotes around hex strings
                return String.Format("0x{0}", ByteFormat.ToHex((UInt16[]) value));
            }
            else if (value is Gen2KillArgs)
            {
                Gen2KillArgs args = (Gen2KillArgs) value;
                if (!(args.Target is TagData))
                    throw new ArgumentException("Kill only supports targets of type TagData, not " + args.Target.ToString());

                string epcHex = ((TagData) args.Target).EpcString;
                UInt32 password = args.Password;
                List<string> wheres = new List<string>();
                wheres.AddRange(MakeTagopWheres());

                return String.Format("1, id=0x{0}, password=0x{1:X} {2}", epcHex, password, WheresToRql(wheres));
            }
            else if (value is Gen2LockArgs)
            {
                Gen2LockArgs args = (Gen2LockArgs) value;
                Gen2.LockAction act = (Gen2.LockAction) args.Action;
                List<string> wheres = new List<string>();
                wheres.Add(String.Format("type={0:D}", act.Mask));
                wheres.AddRange(MakeTagopWheres(args.Target));
                wheres.AddRange(MakeAccessPasswordWheres());

                return String.Format("{0:D} {1}", act.Action, WheresToRql(wheres));
            }
            else if (value is MaskedBits)
            {
                MaskedBits mb = (MaskedBits) value;
                return String.Format("0x{0:X} WHERE mask=0x{1:X}", mb.Value, mb.Mask);
            }
            else if (value is WriteTagArgs)
            {
                WriteTagArgs args = (WriteTagArgs) value;

                if ((null != args.Target) && ("Astra" == _model))
                    throw new ArgumentException("Singulated EPC Write not yet supported on " + _model + " readers.  Please leave target blank.");

                return String.Format("0x{0} {1}", args.Epc.EpcString, WheresToRql(MakeTagopWheres(args.Target)));
            }
            else
            {
                return String.Format("'{0}'", value.ToString());
            }
        }

        #endregion

        #region AntennaToRql

        /// <summary>
        /// Create RQL representing a single antenna
        /// </summary>
        /// <param name="ant">Antenna identifiers</param>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private string AntennaToRql(int ant)
        {
            return String.Format("antenna_id={0:D}", ant);
        }

        #endregion

        #region AntennasToWhereClause
        /// <summary>
        /// Create RQL representing a list of antennas
        /// </summary>
        /// <param name="ants">List of antenna identifiers</param>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private List<string> AntennasToWhereClause(int[] ants)
        {
            List<string> wheres = new List<string>();

            if ((null != ants) && (0 < ants.Length))
            {
                StringBuilder sb = new StringBuilder();
                sb.Append("(");
                List<string> antwords = new List<string>();

                foreach (int ant in ants)
                    antwords.Add(AntennaToRql(ant));

                sb.Append(String.Join(" OR ", antwords.ToArray()));
                sb.Append(")");
                wheres.Add(sb.ToString());
            }

            return wheres;
        } 
        #endregion

        #region ReadPlanToWhereClause
        /// <summary>
        /// Create WHERE clauses representing a ReadPlan
        /// </summary>
        /// <param name="readPlan">Read plan</param>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private List<string> ReadPlanToWhereClause(ReadPlan readPlan)
        {
            List<string> wheres = new List<string>();
            
            if (readPlan is SimpleReadPlan)
            {
                SimpleReadPlan srp = (SimpleReadPlan) readPlan;
                wheres.AddRange(TagProtocolToWhereClause(srp.Protocol));
                wheres.AddRange(AntennasToWhereClause(srp.Antennas));
            }
            else
                throw new ArgumentException("Unrecognized /reader/read/plan type " + typeof(ReadPlan).ToString());
            
            return wheres;
        }

        #endregion

        #region TagFilterToWhereClause

        /// <summary>
        /// Create WHERE clauses representing a tag filter
        /// </summary>
        /// <param name="tagFilter">Tag filter</param>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private List<string> TagFilterToWhereClause(TagFilter tagFilter)
        {
            List<string> wheres = new List<string>();

            if (null == tagFilter)
            {
                // Do nothing
            }
            else if (tagFilter is TagData)
            {
                TagData td = (TagData) tagFilter;
                wheres.Add(String.Format("id={0}", ByteFormat.ToHex(td.EpcBytes)));
            }
            else
                throw new ArgumentException("RQL only supports singulation by EPC. " + tagFilter.ToString() + " is not supported.");

            return wheres;
        }

        #endregion

        #region TagProtocolsToWhereClause

        /// <summary>
        /// Create WHERE clauses representing a list of tag protocols
        /// </summary>
        /// <param name="tp">Tag protocols</param>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private List<string> TagProtocolsToWhereClause(TagProtocol[] tp)
        {
            List<string> wheres = new List<string>();

            if ((null != tp) && (0 < tp.Length))
            {
                List<string> protwords = new List<string>();

                foreach (TagProtocol protocol in tp)
                    protwords.Add(TagProtocolToWhereClause(protocol)[0]);

                string clause = "(" + String.Join(" OR ", protwords.ToArray()) + ")";
                wheres.Add(clause);
            }

            return wheres;
        }

        #endregion

        #region TagProtocolToWhereClause

        /// <summary>
        /// Create WHERE clauses representing a tag protocol
        /// </summary>
        /// <param name="tp">Tag protocol</param>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private List<string> TagProtocolToWhereClause(TagProtocol tp)
        {
            List<string> wheres = new List<string>();
            string clause;

            switch (tp)
            {
                case TagProtocol.GEN2: 
                    clause = "GEN2"; break;
                case TagProtocol.ISO180006B: 
                    clause = "ISO18000-6B"; break;
                default:
                    throw new ArgumentException("Unrecognized protocol " + tp.ToString());
            }

            wheres.Add(String.Format("protocol_id='{0}'", clause));

            return wheres;
        } 

        #endregion

        #region MakeTagopWheres
        /// <summary>
        /// Create list of WHERE clauses representing tagop reader configuration (e.g., TagopAntenna, TagopProtocol)
        /// </summary>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private List<string> MakeTagopWheres()
        {
            List<string> wheres = new List<string>();
            wheres.AddRange(TagProtocolToWhereClause((TagProtocol) ParamGet("/reader/tagop/protocol")));
            wheres.Add(AntennaToRql((int) ParamGet("/reader/tagop/antenna")));
            return wheres;
        }         

        /// <summary>
        /// Create list of WHERE clauses representing tagop reader configuration (e.g., /reader/tagop/antenna, /reader/tagop/protocol)
        /// </summary>
        /// <param name="filt">Tags to target for tagop</param>
        /// <returns>List of strings to be incorporated into "WHERE ... AND ..." phrase.
        /// List may be empty.</returns>
        private List<string> MakeTagopWheres(TagFilter filt)
        {
            List<string> wheres = (List<string>) MakeTagopWheres();
            wheres.AddRange(TagFilterToWhereClause(filt));
            return wheres;
        }

        #endregion

        #region MakeAccessPasswordWheres
        /// <summary>
        /// Makes Where clause for Access Password
        /// </summary>
        /// <returns>List of string with either no elements or the access password WHERE clause</returns>
        private List<string> MakeAccessPasswordWheres()
        {
            List<string> where = new List<string>();
            Gen2.Password password = (Gen2.Password) (ParamGet("/reader/gen2/accessPassword"));

            if (password.Value != 0)
                where.Add(String.Format("password=0x{0:X}", password));

            return where;
        } 
        #endregion

        #region WheresToRql

        private string WheresToRql(List<string> wheres)
        {
            List<string> words = new List<string>();

            if ((null != wheres) && (0 < wheres.Count))
                words.Add(String.Format("WHERE {0}", String.Join(" AND ", wheres.ToArray())));

            return String.Join(" ", words.ToArray());
        }

        #endregion

        #region ParamTable

        private string ParamTable(string name)
        {
            switch (name)
            {
                default:
                    return "params";
                case "uhf_power_centidbm":
                case "tx_power":
                case "hostname":
                case "iface":
                case "dhcpcd":
                case "ip_address":
                case "netmask":
                case "gateway":
                case "ntp_servers":
                case "epc1_id_length":
                case "primary_dns":
                case "secondary_dns":
                case "domain_name":
                case "reader_description":
                case "reader_role":
                case "ant1_readpoint_descr":
                case "ant2_readpoint_descr":
                case "ant3_readpoint_descr":
                case "ant4_readpoint_descr":
                    return "saved_settings";
            }
        }

        #endregion

        #endregion

        #region HTTP Post Methods

        #region MakeWebReq

        /// <summary>
        /// Create web request, using readers' default username and password
        /// </summary>
        /// <param name="uripath"></param>
        /// <returns></returns>
        private HttpWebRequest MakeWebReq(string uripath)
        {
            string url = String.Format("http://{0}{1}", _hostname, uripath);
            HttpWebRequest req = (HttpWebRequest) WebRequest.Create(url);
            byte[] authBytes = Encoding.UTF8.GetBytes("web:radio".ToCharArray());
            req.Headers["Authorization"] = "Basic " + Convert.ToBase64String(authBytes);

            return req;
        }

        #endregion

        #region ReadAllBytes

        private byte[] ReadAllBytes(string filename)
        {
            return ReadAllBytes(new StreamReader(filename).BaseStream);
        }

        private byte[] ReadAllBytes(Stream stream)
        {
            BinaryReader br = new BinaryReader(stream);
            List<byte> list = new List<byte>();

            while (true)
            {
                byte[] chunk = br.ReadBytes(1024 * 1024);

                if (0 == chunk.Length)
                    break;

                list.AddRange(chunk);
            }

            return list.ToArray();
        }

        #endregion

        #region ShowBody

        private static void ShowBody(HttpWebRequest req, string body)
        {
            Console.WriteLine(String.Format("Read {0:D} bytes from {1}", body.Length, req.RequestUri));
            Console.WriteLine("First KB:");
            Console.WriteLine(body.Substring(0, 1024));
        }

        #endregion

        #region WebPost

        private static void WebPost(HttpWebRequest req, string content)
        {
            WebPost(req, "application/x-www-form-urlencoded", Encoding.UTF8.GetBytes(content));
        }

        private static void WebPost(HttpWebRequest req, string contentType, string content)
        {
            WebPost(req, contentType, Encoding.UTF8.GetBytes(content));
        }

        private static void WebPost(HttpWebRequest req, string contentType, byte[] reqbody)
        {
            req.Method = "POST";
            req.ContentType = contentType;
            req.ContentLength = reqbody.Length;
            Stream reqStream = req.GetRequestStream();
            reqStream.Write(reqbody, 0, reqbody.Length);
            reqStream.Close();
        }

        #endregion

        #region WebRespToReader

        private static StreamReader WebRespToReader(HttpWebResponse rsp)
        {
            return new StreamReader(rsp.GetResponseStream());
        }

        #endregion

        #region WebRespToString

        private static string WebRespToString(HttpWebResponse rsp)
        {
            return WebRespToReader(rsp).ReadToEnd();
        }

        #endregion

        #region WebUploadFile

        private void WebUploadFile(HttpWebRequest req, Stream contentStream, string filename)
        {
            string boundary = "MercuryAPIFormBoundary9ef3cac3aeca9440ba8a55224a363a0d";
            string contentType = String.Format("multipart/form-data; boundary={0}", boundary);
            List<byte> content = new List<byte>();
            content.AddRange(Encoding.ASCII.GetBytes(String.Format(String.Join("\r\n", new string[] {
                "--{0}",
                "Content-Disposition: form-data; name=\"uploadfile\"; filename=\"{1}\"",
                "\r\n",
            }), boundary, filename)));
            content.AddRange(ReadAllBytes(contentStream));
            content.AddRange(Encoding.ASCII.GetBytes(String.Format(
                "\r\n--{0}--\r\n",
                boundary)));
            WebPost(req, contentType, content.ToArray());
        }

        #endregion
        
        #endregion

        #region Misc Utility Methods

        #region ParseProtocolList

        private static TagProtocol[] ParseProtocolList(string pstrs)
        {
            List<TagProtocol> plist = new List<TagProtocol>();

            foreach (string pstr in pstrs.Split(new char[] { ' ' }))
            {
                TagProtocol prot = TagProtocol.NONE;

                switch (pstr)
                {
                    case "GEN2":
                        prot = TagProtocol.GEN2; break;
                    case "ISO18000-6B":
                        prot = TagProtocol.ISO180006B; break;
                    default:
                        // ignore unrecognized protocol names
                        break;
                }

                if (TagProtocol.NONE != prot)
                    plist.Add(prot);
            }

            return plist.ToArray();
        }

        #endregion

        #region StringsToBytes

        private static byte[] StringsToBytes(ICollection<string> strings)
        {
            List<byte> responseBytes = new List<byte>();

            foreach (string i in strings)
                responseBytes.AddRange(Encoding.ASCII.GetBytes(i));

            return responseBytes.ToArray();
        }

        #endregion

        #region CodeToProtocol

        /// <summary>
        ///  Translate RQL protocol IDs to TagProtocols
        /// </summary>
        /// <param name="rqlproto"></param>
        /// <returns></returns>
        private static TagProtocol CodeToProtocol(string rqlproto)
        {
            switch (rqlproto)
            {
                case "8":
                    return TagProtocol.ISO180006B;

                case "12":
                    return TagProtocol.GEN2;

                default:
                    return TagProtocol.NONE;
            }
        }

        #endregion

        #region ValidateAntenna
        /// <summary>
        /// Is requested antenna a valid antenna?
        /// </summary>
        /// <param name="reqAnt">Requested antenna</param>
        /// <returns>reqAnt if it is in the set of valid antennas, else throws ArgumentException</returns>
        private int ValidateAntenna(int reqAnt)
        {
            return ValidateParameter<int>(reqAnt, (int[]) ParamGet("/reader/antenna/portList"), "Invalid antenna");
        }

        #endregion

        #endregion        
    }
}
