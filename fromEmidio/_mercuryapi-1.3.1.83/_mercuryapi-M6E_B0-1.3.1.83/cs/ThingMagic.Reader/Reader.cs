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
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading;

namespace ThingMagic
{
    /// <summary>
    /// Abstract base class for ThingMagic RFID reader devices.
    /// </summary>
    public abstract class Reader : Disposable
    {
        #region Delegates
        
        /// <summary>
        /// Parameter setting filter.  Modifies parameter value on the way in or out of the Setting object.
        /// </summary>
        /// <param name="value">Input parameter:
        /// For get, this comes from Setting.Value.
        /// For set, this is the input argument to the Set method.</param>
        /// <returns>Filtered parameter:
        /// For get, this object will be presented to the user.
        /// For set, this object will be saved in Setting.Value.</returns>
        protected delegate Object SettingFilter(Object value);

        #endregion

        #region Nested Enums

        /// <summary>
        /// RFID regulatory regions
        /// </summary>
        public enum Region
        {
            /// <summary>
            /// Region not set
            /// </summary>
            UNSPEC,
            /// <summary>
            /// Canada
            /// </summary>
            CN,
            /// <summary>
            /// Europe, version 1 (LBT)
            /// </summary>
            EU,
            /// <summary>
            /// Europe, version 2 (??)
            /// </summary>
            EU2,
            /// <summary>
            /// Europe, version 3 (no LBT)
            /// </summary>
            EU3,
            /// <summary>
            /// India
            /// </summary>
            IN,
            // TODO: Get better descriptions of the different versions of EU
            /// <summary>
            /// Korea
            /// </summary>
            KR,
            /// <summary>
            /// Korea (revised)
            /// </summary>
            KR2,
            /// <summary>
            /// Japan
            /// </summary>
            JP,
            /// <summary>
            /// North America
            /// </summary>
            NA,
            /// <summary>
            /// People's Republic of China (mainland)
            /// </summary>
            PRC,
            /// <summary>
            /// Unrestricted access to full hardware range
            /// </summary>
            MANUFACTURING,
            /// <summary>
            /// Unrestricted access to full hardware range
            /// </summary>
            OPEN,
        };
        #endregion

        #region Nested Classes

        #region TagReadCallback

        /// <summary>
        /// ThreadPool-compatible wrapper for servicing asynchronous reads
        /// </summary>
        private sealed class TagReadCallback
        {
            #region Fields

            private Reader           _rdr;
            private TagReadData[]    _reads;

            #endregion

            #region Construction

            /// <summary>
            /// Create ThreadPool-compatible wrapper
            /// </summary>
            /// <param name="rdr">Reader object that will service TagReadData event</param>
            /// <param name="reads">TagReadData event to servic e</param>
         
            public TagReadCallback(Reader rdr, TagReadData[] reads)
            {
                _rdr       = rdr;
                _reads     = reads;
            }

            #endregion

            #region ThreadPoolCallBack

            /// <summary>
            /// ThreadPool-compatible callback to be passed to ThreadPool.QueueUserWorkItem
            /// </summary>
            /// <param name="threadContext">Identifier of thread that is servicing this callback</param>
            public void ThreadPoolCallBack(Object threadContext)
            {
                foreach (TagReadData tag in _reads)
                {
                    _rdr.OnTagRead(tag);
                }

                lock (_rdr._backgroundNotifierLock)
                    _rdr._backgroundNotifierCallbackCount--;
            }

            #endregion
        }

        #endregion

        #region Settings

        /// <summary>
        /// Parameter object
        /// </summary>
        protected class Setting
        {
            #region Fields

            // Name of parameter.
            // Store here because dispatch table only keeps case-insensitive version.
            internal string Name;

            // Data type
            internal Type Type;

            // Data value
            internal Object Value;

            // Write permission -- can parameter be set?
            internal bool Writeable;

            //if the setting is Confirmed to exist
            internal bool Confirmed;

            // store the value of the parameter in paramGet
            internal bool CacheGetValue;

            // Filters for get and set actions.
            // Convert Object types, call appropriate hooks on data going in or out of the Setting.
            internal SettingFilter GetFilter;
            internal SettingFilter SetFilter;

            #endregion

            #region Construction

            /// <summary>
            /// Create a parameter object, which includes parameter value and metadata.
            /// </summary>
            /// <param name="name">Name of parameter</param>
            /// <param name="type">Data type; e.g., typeof(int)</param>
            /// <param name="value">Stored value</param>
            /// <param name="writeable">Allow write access?</param>
            public Setting(string name, Type type, Object value, bool writeable) : this(name, type, value, writeable, null, null) { }

            /// <summary>
            /// Create a parameter object, which includes parameter value and metadata.
            /// </summary>
            /// <param name="name">Name of parameter</param>
            /// <param name="type">Data type; e.g., typeof(int)</param>
            /// <param name="value">Stored value</param>
            /// <param name="writeable">Allow write access?</param>
            /// <param name="getfilter">Filter to use on ParamGet.  NOTE: If value is mutable, always make a copy in getfilter to prevent unintentional modifications.</param>
            /// <param name="setfilter">Filter to use on ParamSet.</param>
            public Setting(string name, Type type, Object value, bool writeable, SettingFilter getfilter, SettingFilter setfilter) : this(name, type, value, writeable, getfilter, setfilter, true) { }

            /// <summary>
            /// Create a parameter object, which includes parameter value and metadata.
            /// </summary>
            /// <param name="name">Name of parameter</param>
            /// <param name="type">Data type; e.g., typeof(int)</param>
            /// <param name="value">Stored value</param>
            /// <param name="writeable">Allow write access?</param>
            /// <param name="getfilter">Filter to use on ParamGet.  NOTE: If value is mutable, always make a copy in getfilter to prevent unintentional modifications.</param>
            /// <param name="setfilter">Filter to use on ParamSet.</param>
            /// <param name="confirmed">If the parameter is Confirmed </param>
            public Setting(string name, Type type, Object value, bool writeable, SettingFilter getfilter, SettingFilter setfilter, bool confirmed) : this(name, type, value, writeable, getfilter, setfilter, true, false) { }

            /// <summary>
            /// Create a parameter object, which includes parameter value and metadata.
            /// </summary>
            /// <param name="name">Name of parameter</param>
            /// <param name="type">Data type; e.g., typeof(int)</param>
            /// <param name="value">Stored value</param>
            /// <param name="writeable">Allow write access?</param>
            /// <param name="getfilter">Filter to use on ParamGet.  NOTE: If value is mutable, always make a copy in getfilter to prevent unintentional modifications.</param>
            /// <param name="setfilter">Filter to use on ParamSet.</param>
            /// <param name="confirmed">If the parameter is Confirmed </param>
            /// <param name="cacheGetValue">store the value of the parameter in paramGet</param>
            public Setting(string name, Type type, Object value, bool writeable, SettingFilter getfilter, SettingFilter setfilter,bool confirmed,bool cacheGetValue)
            {
                if (value != null && type.IsInstanceOfType(value) == false)
                {
                    throw new ArgumentException("Wrong type for parameter initial value.");
                }

                Name = name;
                Type = type;
                Value = value;
                Writeable = writeable;
                GetFilter = getfilter;
                SetFilter = setfilter;
                Confirmed = confirmed;
                CacheGetValue = cacheGetValue;
            }

            #endregion
        }

        #endregion

        #region ReadCollector

        private sealed class ReadCollector
        {
            #region Fields

            public List<TagReadData> ReadList = new List<TagReadData>(); 

            #endregion

            #region Cosntruction

            public void HandleRead( object sender, TagReadDataEventArgs e )
            {
                this.ReadList.Add( e.TagReadData );
            }

            #endregion
        }

        #endregion

        #endregion

        #region Fields

        /// <summary>
        /// Track doneness of ThreadPool callbacks
        /// </summary>
        private int    _backgroundNotifierCallbackCount = 0;
        private Object _backgroundNotifierLock = new Object();

        private Dictionary<string, Setting> _params = null;

        /// <summary>
        /// Internal flag to enable "tag reading."
        /// If true, generate tag reads.  If false, stop "reading tags."
        /// </summary>
        private bool _runNow = false;

        /// <summary>
        /// Internal flag to "close reader."
        /// If true, quit worker thread.
        /// </summary>
        private bool _exitNow = false;

        private Thread _workerThread = null;

        #endregion

        #region Properties

        private Dictionary<string, Setting> Params
        {
            get
            {
                if (null == _params)
                {
                    _params = new Dictionary<string, Setting>(StringComparer.OrdinalIgnoreCase);

                    ParamAdd(new Setting("/reader/read/asyncOnTime", typeof(int), 250, true, delegate(Object val)
                {
                    return val;
                },
                delegate(Object val)
                {
                    if ((int)val < 0)
                    {
                        throw new ArgumentOutOfRangeException("Negative value for asyncOnTime Not Supported");
                    }
                    return val;
                }));
                    ParamAdd(new Setting("/reader/read/asyncOffTime", typeof(int), 0, true, delegate(Object val)
                {
                    return val;
                },
                delegate(Object val)
                {
                    if ((int)val < 0)
                    {
                        throw new ArgumentOutOfRangeException("Negative value for asyncOffTime Not Supported");
                    }
                    return val;
                }));
                    ParamAdd(new Setting("/reader/gen2/accessPassword", typeof(Gen2.Password), new Gen2.Password(0), true,
                        null,
                        delegate(Object val)
                        {
                            if (null == val)
                                val = new Gen2.Password(0);
                            return val;
                        }
                        ));
                    ParamAdd(new Setting("/reader/read/filter", typeof(TagFilter), null, true, null, null));
                }

                return _params;
            }
        }

        #endregion

        #region Events

        /// <summary>
        /// Occurs when each tag is read.
        /// </summary>
        public event EventHandler< TagReadDataEventArgs > TagRead;

        /// <summary>
        /// Transport message was sent or received
        /// </summary>
        public event EventHandler< TransportListenerEventArgs > Transport;

        /// <summary>
        /// Occurs when asynchronous read throws an exception.
        /// </summary>
        public event EventHandler< ReaderExceptionEventArgs > ReadException;

        #endregion

        #region Abstract Methods

        #region Connect

        /// <summary>
        /// Connect reader object to device.
        /// If object already connected, then do nothing.
        /// </summary>
        public abstract void Connect();

        #endregion

        #region Destroy

        /// <summary>
        /// Shuts down the connection with the reader device.
        /// </summary>
        public abstract void Destroy();

        #endregion

        #region Read

        /// <summary>
        /// Read RFID tags for a fixed duration.
        /// </summary>
        /// <param name="timeout">the time to spend reading tags, in milliseconds</param>
        /// <returns>the tags read</returns>
        public abstract TagReadData[] Read(int timeout);

        #endregion

        #region StartReading

        /// <summary>
        /// Start reading RFID tags in the background. The tags found will be
        /// passed to the registered read listeners, and any exceptions that
        /// occur during reading will be passed to the registered exception
        /// listeners. Reading will continue until stopReading() is called.
        /// </summary>
        public abstract void StartReading();

        #endregion

        #region StopReading

        /// <summary>
        /// Stop reading RFID tags in the background.
        /// </summary>
        public abstract void StopReading();

        #endregion

        #region FirmwareLoad

        /// <summary>
        /// Load a new firmware image into the device's nonvolatile memory.
        /// This installs the given image data onto the device and restarts
        /// it with that image. The firmware must be of an appropriate type
        /// for the device. Interrupting this operation may damage the
        /// reader.
        /// </summary>
        /// <param name="firmware">a data _stream of the firmware contents</param>
        public abstract void FirmwareLoad(System.IO.Stream firmware);

        #endregion

        #region GpiGet

        /// <summary>
        /// Get the state of all of the reader's GPI pins. 
        /// </summary>
        /// <returns>array of GpioPin objects representing the state of all input pins</returns>
        public abstract GpioPin[] GpiGet();

        #endregion

        #region GpoSet

        /// <summary>
        /// Set the state of some GPO pins.
        /// </summary>
        /// <param name="state">array of GpioPin objects</param>
        public abstract void GpoSet(ICollection<GpioPin> state);

        #endregion

        #region ExecuteTagOp

        /// <summary>
        /// execute a TagOp
        /// </summary>
        /// <param name="tagOP">Tag Operation</param>
        /// <param name="target">Tag filter</param>
        ///<returns>the return value of the tagOp method if available</returns>

        public abstract Object ExecuteTagOp(TagOp tagOP, TagFilter target);

        #endregion

        #region KillTag

        /// <summary>
        /// Kill a tag. The first tag seen is killed.
        /// </summary>
        /// <param name="target">the tag to kill, or null</param>
        /// <param name="password">the authentication needed to kill the tag</param>
        public abstract void KillTag(TagFilter target, TagAuthentication password);

        #endregion

        #region LockTag

        /// <summary>
        /// Perform a lock or unlock operation on a tag. The first tag seen
        /// is operated on - the singulation parameter may be used to control
        /// this. Note that a tag without an access password set may not
        /// accept a lock operation or remain locked.
        /// </summary>
        /// <param name="target">the tag to lock, or null</param>
        /// <param name="action">the locking action to take</param>
        public abstract void LockTag(TagFilter target, TagLockAction action);

        #endregion

        #region ReadTagMemBytes

        /// <summary>
        /// Read data from the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag to read from, or null</param>
        /// <param name="bank">the tag memory bank to read from</param>
        /// <param name="byteAddress">the word address to start reading from</param>
        /// <param name="byteCount">the number of words to read</param>
        /// <returns>the words read</returns>
        public abstract byte[] ReadTagMemBytes(TagFilter target, int bank, int byteAddress, int byteCount);

        #endregion

        #region ReadTagMemWords

        /// <summary>
        /// Read data from the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag to read from, or null</param>
        /// <param name="bank">the tag memory bank to read from</param>
        /// <param name="wordAddress">the word address to start reading from</param>
        /// <param name="wordCount">the number of words to read</param>
        /// <returns>the words read</returns>
        public abstract ushort[] ReadTagMemWords(TagFilter target, int bank, int wordAddress, int wordCount);

        #endregion

        #region WriteTag

        /// <summary>
        /// Write a new ID to a tag.
        /// </summary>
        /// <param name="target">the tag to write to, or null</param>
        /// <param name="epc">the new tag ID to write</param>
        public abstract void WriteTag(TagFilter target, TagData epc);

        #endregion

        #region WriteTagMemBytes

        /// <summary>
        /// Write data to the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag to write to, or null</param>
        /// <param name="bank">the tag memory bank to write to</param>
        /// <param name="address">the byte address to start writing to</param>
        /// <param name="data">the bytes to write</param>
        public abstract void WriteTagMemBytes(TagFilter target, int bank, int address, ICollection<byte> data);

        #endregion

        #region WriteTagMemWords

        /// <summary>
        /// Write data to the memory bank of a tag.
        /// </summary>
        /// <param name="target">the tag to write to, or null</param>
        /// <param name="bank">the tag memory bank to write to</param>
        /// <param name="address">the word address to start writing to</param>
        /// <param name="data">the words to write</param>
        public abstract void WriteTagMemWords(TagFilter target, int bank, int address, ICollection<ushort> data);

        #endregion

        #endregion

        #region Create

        /// <summary>
        /// Return an instance of a Reader class associated with a
        /// serial reader on a particular communication port.
        /// </summary>
        /// <param name="uriString">Identifies the reader to connect to with a URI
        /// syntax. The scheme can be "eapi" for the embedded module
        /// protocol, "rql" for the request query language, or "tmr" to
        /// guess. The remainder of the string identifies the _stream that the
        /// protocol will be spoken over, either a local host serial port
        /// device or a TCP network port.
        /// Examples include: 
        ///   "eapi:///dev/ttyUSB0"
        ///   "eapi:///com1"
        ///   "eapi://modproxy.example.com:2500/"
        ///   "rql://reader.example.com/"
        ///   "tmr:///dev/ttyS0"
        ///   "tmr://192.168.1.101:80/"
        /// </param>
        /// <remarks>Set autoConnect to false if you need to reconfigure the reader object before opening any physical interfaces
        /// (e.g., attach a transport listener to monitor the init sequence, set a nonstandard baud rate or transport timeout.)
        /// If autoConnect is false, Create will just create the reader object, which may then be configured
        /// before the actual connection is made by calling its Connect method.</remarks>
        /// <returns>Reader object associated with device</returns>
        public static Reader Create(string uriString)
        {
            Uri uri = new Uri(uriString);

            switch (uri.Scheme)
            {
                case "tmr":

                    if (0 < uri.Host.Length)
                        uriString = uriString.Replace("tmr:", "rql:");
                    else
                        uriString = uriString.Replace("tmr:", "eapi:");

                    return Create(uriString);

                case "eapi":
                    return new SerialReader(uri.PathAndQuery);

                case "rql":

                    if ("" == uri.Host)
                        throw new ArgumentException("Must provide a host name for URI (rql://hostname)");

                    if (!(("" == uri.PathAndQuery) || ("/" == uri.PathAndQuery)))
                        throw new ArgumentException("Path not supported for " + uri.Scheme + " URIs");

                    return new RqlReader(uri.Host, uri.Port);

                default:
                    throw new ReaderException("Unknown URI scheme: " + uri.Scheme + " in " + uriString);
            }
        }

        #endregion

        #region Dispose

        /// <summary>
        /// Dispose
        /// </summary>
        /// <param name="bDisposing">is Disposing?</param>
        /// <returns>void</returns>
        protected override void Dispose(bool bDisposing)
        {
            Destroy();
            base.Dispose(bDisposing);
        }

        #endregion

        #region Setting Methods

        #region ValidateParameterKey
        /// <summary>
        /// Check for existence of parameter.  Throw exception if parameter does not exist.
        /// </summary>
        /// <param name="key">Parameter name</param>
        /// <returns>Setting if key is valid.  Otherwise, throws ArgumentException.</returns>
        protected Setting ValidateParameterKey(string key)
        {
            if (false == Params.ContainsKey(key))
                throw new ArgumentException("Parameter not found: \"" + key + "\"");
            else
                return Params[key];
        }

        #endregion

        #region ParamAdd
        /// <summary>
        /// Register a new parameter handler
        /// </summary>
        /// <param name="handler">Parameter handler.
        /// Get method will be called -- parameter will only be added if get succeeds.</param>
        protected void ParamAdd(Setting handler)
        {
            // IMPORTANT!  Put params in dictionary using item interface (dict[key]=value)
            // instead of dict.Add(key,value) method to avoid exceptions when overwriting
            // parameter definitions (e.g., after firmware update.)
            // http://msdn.microsoft.com/en-us/library/k7z0zy8k(VS.80).aspx
            Params[handler.Name] = handler;
        }

        #endregion

        #region ParamClear

        /// <summary>
        ///  Reset parameter table; e.g., to reprobe hardware afer firmware update
        /// </summary>
        protected void ParamClear()
        {
            _params = null;
        }

        #endregion

        #region probeSetting

        /// <summary>
        /// Probe if the parameter exists in the module.
        /// </summary>
        /// <param name="s">the parameter setting</param>
        /// <returns>the boolean value representing the existence of the parameter in the module</returns>
        bool probeSetting(Setting s)
        {
            try
            {
                s.GetFilter(null);
                s.Confirmed = true;
            }
            catch (ReaderException)
            {
            }
            if (s.Confirmed == false)
            {
                Params.Remove(s.Name);
            }
            return s.Confirmed;

        }

        #endregion

        #region ParamGet

        /// <summary>
        /// Get the value of a Reader parameter.
        /// </summary>
        /// <param name="key">the parameter name</param>
        /// <returns>the value of the parameter, as an Object</returns>
        public Object ParamGet(string key)
        {
            Setting set = ValidateParameterKey(key);
            Object  val = set.Value;

            if (set.Confirmed==false && probeSetting(set)== false)
            {
                throw new ArgumentException("Parameter not found: \"" + key + "\"");
            }

            if (null != set.GetFilter)
                val = set.GetFilter(val);

            else
            {
                // Don't ever return a direct reference to a mutable value
                if (val is ICloneable)
                    val = ((ICloneable) val).Clone();
            }
            if (set.CacheGetValue)
            {
                set.Value = val;
            }
            return val;
        }

        #endregion

        #region ParamList

        /// <summary>
        /// Get a list of the parameters available
        /// 
        /// <para>Supported Parameters:
        /// <list type="bullet">
        /// <item><term>/reader/antenna/PortList</term></item>
        /// <item><term>/reader/antenna/checkPort</term></item>
        /// <item><term>/reader/antenna/connectedPortList</term></item>
        /// <item><term>/reader/antenna/portList</term></item>
        /// <item><term>/reader/antenna/portSwitchGpos</term></item>
        /// <item><term>/reader/antenna/portSwitchGpos not supported</term></item>
        /// <item><term>/reader/antenna/settlingTimeList</term></item>
        /// <item><term>/reader/antenna/txRxMap</term></item>
        /// <item><term>/reader/baudRate</term></item>
        /// <item><term>/reader/commandTimeout</term></item>
        /// <item><term>/reader/enableBlockWrite</term></item>
        /// <item><term>/reader/enableStreaming</term></item>
        /// <item><term>/reader/gen2/BLF</term></item>
        /// <item><term>/reader/gen2/accessPassword</term></item>
        /// <item><term>/reader/gen2/q</term></item>
        /// <item><term>/reader/gen2/session</term></item>
        /// <item><term>/reader/gen2/tagEncoding</term></item>
        /// <item><term>/reader/gen2/target</term></item>
        /// <item><term>/reader/gen2/target not supported</term></item>
        /// <item><term>/reader/gen2/tari</term></item>
        /// <item><term>/reader/gen2/writeMode</term></item>
        /// <item><term>/reader/gpio/inputList</term></item>
        /// <item><term>/reader/gpio/outputList</term></item>
        /// <item><term>/reader/iso180006b/BLF</term></item>
        /// <item><term>/reader/powerMode</term></item>
        /// <item><term>/reader/radio/enablePowerSave</term></item>
        /// <item><term>/reader/radio/portReadPowerList</term></item>
        /// <item><term>/reader/radio/portWritePowerList</term></item>
        /// <item><term>/reader/radio/powerMax</term></item>
        /// <item><term>/reader/radio/powerMin</term></item>
        /// <item><term>/reader/radio/readPower</term></item>
        /// <item><term>/reader/radio/temperature</term></item>
        /// <item><term>/reader/radio/writePower</term></item>
        /// <item><term>/reader/read/asyncOffTime</term></item>
        /// <item><term>/reader/read/asyncOnTime</term></item>
        /// <item><term>/reader/read/filter</term></item>
        /// <item><term>/reader/read/plan</term></item>
        /// <item><term>/reader/region/hopTable</term></item>
        /// <item><term>/reader/region/hopTime</term></item>
        /// <item><term>/reader/region/id</term></item>
        /// <item><term>/reader/region/lbt/enable</term></item>
        /// <item><term>/reader/region/supportedRegions</term></item>
        /// <item><term>/reader/tagReadData/recordHighestRssi</term></item>
        /// <item><term>/reader/tagReadData/reportRssiInDbm</term></item>
        /// <item><term>/reader/tagReadData/uniqueByAntenna</term></item>
        /// <item><term>/reader/tagReadData/uniqueByData</term></item>
        /// <item><term>/reader/tagop/antenna</term></item>
        /// <item><term>/reader/tagop/protocol</term></item>
        /// <item><term>/reader/transportTimeout</term></item>
        /// <item><term>/reader/userMode</term></item>
        /// <item><term>/reader/version/hardware</term></item>
        /// <item><term>/reader/version/model</term></item>
        /// <item><term>/reader/version/serial</term></item>
        /// <item><term>/reader/version/software</term></item>
        /// <item><term>/reader/version/supportedProtocols</term></item>
        /// </list>
        /// </para>
        /// </summary>
        /// <returns>an array of the parameter names</returns>
        public string[] ParamList()
        {
            List<string> names = new List<string>();

            foreach (Setting set in Params.Values)
            {
                if (set.Confirmed == false && probeSetting(set) == false)
                {
                    continue;
                }

                names.Add(set.Name);
            }

            names.Sort();

            return names.ToArray();
        }

        #endregion

        #region ParamSet

        /// <summary>
        /// Set the value of a Reader parameter.
        /// </summary>
        /// <remarks>See <see>ParamGet</see> for list of supported parameters.</remarks>
        /// <param name="key">the parameter name</param>
        /// <param name="value">value of the parameter, as an Object</param>
        public void ParamSet(string key, Object value)
        {
            Setting set = ValidateParameterKey(key);


            if (set.Confirmed == false && probeSetting(set) == false)
            {
                throw new ArgumentException("Parameter not found: \"" + key + "\"");
            }

            if (false == set.Writeable)
                throw new ArgumentException("Parameter \"" + key + "\" is read-only.");

            if ((null != value) && (false == set.Type.IsAssignableFrom(value.GetType())))
                throw new ArgumentException("Wrong type " + value.GetType().Name + " for parameter \"" + key + "\".");

            Object val = value;

            if (null != set.SetFilter)
                val = set.SetFilter(val);

            set.Value = val;
        }

        #endregion

        #endregion

        #region Read Methods

        #region ReadGivenStartStop

        //// Implement Read, given working StartReading and StopReading methods

        /// <summary>
        /// Utility function to implement Read given working StartReading and StopReading methods
        /// </summary>
        /// <param name="milliseconds">Number of milliseconds to keep reading.</param>
        /// <returns>the read tag data collection</returns>
        protected TagReadData[] ReadGivenStartStop(int milliseconds)
        {
            ReadCollector collector = new ReadCollector();
            
            this.TagRead += new EventHandler< TagReadDataEventArgs >( collector.HandleRead );
            
            this.StartReading();
            
            Thread.Sleep(milliseconds);
            
            this.StopReading();
            
            this.TagRead -= new EventHandler< TagReadDataEventArgs >( collector.HandleRead );
            
            return collector.ReadList.ToArray();
        }

        #endregion

        #region StartReadingGivenRead

        //// Implement StartReading, given a working Read method

        /// <summary>
        /// Utility function to implement StartReading given a working Read method
        /// </summary>
        [MethodImpl(MethodImplOptions.Synchronized)]
        protected void StartReadingGivenRead()
        {
            _exitNow = false;
            _runNow  = true;

            if (null == _workerThread)
            {
                _workerThread = new Thread(DoWorkGivenRead);
                _workerThread.IsBackground = true;
                _workerThread.Start();
            }
        }

        #endregion

        #region StopReadingGivenRead

        //// Implement StopReading, given a working Read method

        /// <summary>
        /// Utility function to implement StopReading given a working Read method
        /// </summary>
        protected void StopReadingGivenRead()
        {
            _exitNow = true;

            if (null != _workerThread)
            {
                // Wait for read thread to finish
                _workerThread.Join();
                _workerThread = null;
            }

            // Wait for all callbacks to be serviced
            while (0 < _backgroundNotifierCallbackCount) 
            {
                // Don't tie up the CPU.
                // Ideally, wait on the ThreadPool, but there's no accessor for that.
                Thread.Sleep(10);
            }
        }

        #endregion

        #region DoWorkGivenRead

        /// <summary>
        /// Logic for asynchronous worker thread given a working Read method
        /// </summary>
        protected void DoWorkGivenRead()
        {
            try
            {
                while (false == _exitNow)
                {
                    if (_runNow)
                    {
                        int readTime = (int)ParamGet("/reader/read/asyncOnTime");
                        int sleepTime = (int)ParamGet("/reader/read/asyncOffTime");
                        TagReadData[] reads = Read(readTime);

                        lock (_backgroundNotifierLock)
                            _backgroundNotifierCallbackCount++;

                        TagReadCallback callback = new TagReadCallback(this, reads);
                        ThreadPool.QueueUserWorkItem(callback.ThreadPoolCallBack);
                        Thread.Sleep(sleepTime);
                    }
                }
            }
            catch (ReaderException ex)
            {
                ReadExceptionPublisher expub = new ReadExceptionPublisher(this, ex);
                Thread trd = new Thread(expub.OnReadException);
                trd.Start();
                
                // Don't call the usual stop method, or we'll deadlock ourselves
                //StopReadingGivenRead();
            }
        }

        #endregion

        #region DestroyGivenRead

        /// <summary>
        /// Clean up actions given a working Read method
        /// </summary>
        protected void DestroyGivenRead()
        {
            StopReading();
        }

        #endregion

        #region ReadTagMemWordsGivenReadTagMemBytes

        //// Some tagops can be implemented in terms of other tagops
        /// <summary>
        /// Implement ReadTagMemWords in terms of ReadTagMemBytes
        /// </summary>
        /// <param name="target">Tag to read</param>
        /// <param name="bank">Memory bank identifier</param>
        /// <param name="wordAddress">Word address to start reading at</param>
        /// <param name="wordCount">Number of words to read</param>
        /// <returns>Words read</returns>
        protected internal ushort[] ReadTagMemWordsGivenReadTagMemBytes(TagFilter target, int bank, int wordAddress, int wordCount)
        {
            byte[]   memBytes = ReadTagMemBytes(target, bank, wordAddress * 2, wordCount * 2);
            ushort[] memWords = new ushort[wordCount];

            for (int i = 0 ; i < wordCount ; i++)
            {
                memWords[i] = memBytes[2 * i];
                memWords[i] <<= 8;
                memWords[i] |= memBytes[2 * i + 1];
            }

            return memWords;
        }

        #endregion

        #endregion

        #region Event Handlers

        #region OnTagRead

        /// <summary>
        /// Internal accessor to TagRead event.
        /// Called by members of the Reader class to fire a TagRead event.
        /// </summary>
        /// <param name="tagReadData">Data from a single tag read</param>
        protected void OnTagRead( TagReadData tagReadData )
        {
            if (null != TagRead)
                TagRead(this, new TagReadDataEventArgs(tagReadData));
        }

        #endregion

        #region OnReadException

        private class ReadExceptionPublisher
        {
            Reader reader;
            ReaderException exception;

            public ReadExceptionPublisher(Reader reader, ReaderException ex)
            {
                this.reader = reader;
                exception = ex;
            }

            /// <summary>
            /// Internal accessor to ReadException event.
            /// Called by members of the Reader class to fire a ReadException event.
            /// </summary>
            public void OnReadException()
            {
                if (null != reader.ReadException)
                    reader.ReadException(this, new ReaderExceptionEventArgs(exception));
            }
        }

        #endregion

        #region OnTransport

        /// <summary>
        /// Fire Transport message event
        /// </summary>
        /// <param name="tx">Message direction: True=host to reader, False=reader to host</param>
        /// <param name="data">Message contents, including framing and checksum bytes</param>
        /// <param name="timeout">Transport timeout setting (milliseconds) when message was sent or received</param>
        protected void OnTransport(bool tx, byte[] data, int timeout)
        {
            if (null != Transport)
                Transport(this, new TransportListenerEventArgs(tx, data, timeout));
        }

        #endregion

        #endregion

        #region  Misc Utility Methods

        #region GetFirstConnectedAntenna

        /// <summary>
        /// Pick first available connected antenna
        /// </summary>
        /// <returns>First connected antenna, or 0, if none connected.
        /// (Assumes 0 is never a valid antenna number.)</returns>
        protected int GetFirstConnectedAntenna()
        {
            int[] validAnts = (int[]) ParamGet("/reader/antenna/connectedPortList");

            if (0 < validAnts.Length)
                return validAnts[0];

            return 0;
        }

        #endregion

        #region GetFirstSupportedProtocol

        /// <summary>
        /// Pick first available supported protocol
        /// </summary>
        /// <returns>First supported protocol.  Throws exception if none supported.</returns>
        protected TagProtocol GetFirstSupportedProtocol()
        {
            TagProtocol[] valids = (TagProtocol[]) ParamGet("/reader/version/supportedProtocols");

            if (0 < valids.Length)
                return valids[0];

            throw new ReaderException("No tag protcols available");
        }

        #endregion

        #region ValidateProtocol

        /// <summary>
        /// Is requested protocol a valid protcol?
        /// </summary>
        /// <param name="req">Requested protocol</param>
        /// <returns>req if it is valid, else throws ArgumentException</returns>
        protected TagProtocol ValidateProtocol(TagProtocol req)
        {
            return ValidateParameter<TagProtocol>(req, (TagProtocol[]) ParamGet("/reader/version/supportedProtocols"), "Unsupported protocol");
        }

        #endregion

        #region ValidateParameter

        /// <summary>
        /// Is requested value a valid value?
        /// </summary>
        /// <typeparam name="T">the parameter data type</typeparam>
        /// <param name="req">Requested value</param>
        /// <param name="valids">Array of valid parameters (will be sorted, hope you don't mind.)</param>
        /// <param name="errmsg">Message to use for invalid values; e.g., "Invalid antenna" -> "Invalid antenna: 3"</param>
        /// <returns>Value if valid.  Throws ReaderException if invalid.</returns>
        protected static T ValidateParameter<T>(T req, T[] valids, string errmsg)
        {
            if (IsMember<T>(req, valids))
                return req;

            throw new ReaderException(errmsg + ": " + req);
        }

        #endregion

        #region IsMember

        /// <summary>
        /// Is requested value a valid value?
        /// </summary>
        /// <typeparam name="T"> the member type</typeparam>
        /// <param name="req">Requested value</param>
        /// <param name="valids">Array of valid parameters (will be sorted, hope you don't mind.)</param>
        /// <returns>True if value is member of list.  False otherwise.</returns>
        protected static bool IsMember<T>(T req, T[] valids)
        {
            Array.Sort(valids);

            if (0 <= Array.BinarySearch(valids, req))
                return true;

            return false;
        }

        #endregion

        #endregion
    }
}
