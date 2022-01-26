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
using System.Text;

namespace ThingMagic
{
    /// <summary>
    /// Gen2 protocol-specific constructs
    /// </summary>
    public static class Gen2
    {
        #region Nested Enums

        #region Bank

        /// <summary>
        /// Gen2 memory bank identifiers
        /// </summary>
        public enum Bank
        {
            /// <summary>
            /// Reserved memory contains kill and access passwords
            /// </summary>
            RESERVED = 0,
            /// <summary>
            /// EPC memory contains CRC, PC, EPC
            /// </summary>
            EPC = 1,
            /// <summary>
            /// TID memory contains tag implementation identifiers
            /// </summary>
            TID = 2,
            /// <summary>
            /// User memory is optional, but exists for user-defined data
            /// </summary>
            USER = 3,
        }

        #endregion

        #region Target

        /// <summary>
        /// Gen2 target settings.
        /// Includes standard A(0) and B(1), as well as
        /// ThingMagic reader values of A-then-B, and B-then-A
        /// </summary>
        public enum Target
        {
            /// <summary>
            /// Search for tags in State A
            /// </summary>
            A,
            /// <summary>
            /// Search for tags in State B
            /// </summary>
            B,
            /// <summary>
            /// Search for tags in State A, then switch to B
            /// </summary>
            AB,
            /// <summary>
            /// Search for tags in State B, then switch to A
            /// </summary>
            BA
        }

        #endregion

        #region Session

        /// <summary>
        /// Gen2 Session settings. 
        /// </summary>
        public enum Session
        {
            /// <summary>
            /// Session 0.
            /// </summary>        
            S0 = 0,
            /// <summary>
            /// Session 1.
            /// </summary>
            S1 = 1,
            /// <summary>
            /// Session 2.
            /// </summary>
            S2 = 2,
            /// <summary>
            /// Session 3.
            /// </summary>
            S3 = 3,
        }

        #endregion

        #region TagEncoding

        /// <summary>
        /// Gen2 Tag Encoding.
        /// </summary>
        public enum TagEncoding
        {
            /// <summary>
            /// FM0.
            /// </summary>
            FM0 = 0,
            /// <summary>
            /// M = 2.
            /// </summary>
            M2 = 1,
            /// <summary>
            /// M = 4.
            /// </summary>
            M4 = 2,
            /// <summary>
            /// M = 8.
            /// </summary>
            M8 = 3,
        }

        #endregion

        #region DivideRatio

        /// <summary>
        /// Divide Ratio values
        /// </summary>
        public enum DivideRatio
        {
            /// <summary>
            /// Divide by 8
            /// </summary>
            DR8 = 0,
            /// <summary>
            /// Divide by 64/3
            /// </summary>
            DR64_3 = 1,
        }
        #endregion

        #region TrExt

        /// <summary>
        /// TRext: Include extended preamble in Tag-to-Reader response?
        /// </summary>
        public enum TrExt
        {
            /// <summary>
            /// No extension
            /// </summary>
            NOPILOTTONE = 0,
            /// <summary>
            /// Add pilot tone
            /// </summary>
            PILOTTONE = 1,
        }

        #endregion

        #region LockBits

        /// <summary>
        /// Gen2 lock bits, as used in Action and Mask fields of Gen2 Lock command.
        /// Not exposed to end user -- use friendlier Gen2LockActions instead.
        /// </summary>
        [Flags]
        private enum LockBits
        {
            /// <summary>
            /// No action (empty mask)
            /// </summary>
            NONE = 0,
            /// <summary>
            /// User memory permalock -- set to disallow changes to user memory lock bit
            /// </summary>
            USER_PERM = 1 << 0,
            /// <summary>
            /// User memory [write] lock -- set to disallow writes to user memory, clear to allow writes
            /// </summary>
            USER = 1 << 1,
            /// <summary>
            /// TID memory permalock -- set to disallow changes to TID memory lock bit
            /// </summary>
            TID_PERM = 1 << 2,
            /// <summary>
            /// TID memory [write] lock -- set to disallow writes to TID memory, clear to allow writes
            /// </summary>
            TID = 1 << 3,
            /// <summary>
            /// EPC memory permalock -- set to disallow changes to EPC memory lock bit
            /// </summary>
            EPC_PERM = 1 << 4,
            /// <summary>
            /// EPC memory [write] lock -- set to disallow writes to EPC memory, clear to allow writes
            /// </summary>
            EPC = 1 << 5,
            /// <summary>
            /// Access password memory permalock -- set to disallow changes to access password memory lock bit
            /// </summary>
            ACCESS_PERM = 1 << 6,
            /// <summary>
            /// Access password [read/write] lock -- set to disallow read and write of access password, clear to allow read and write
            /// </summary>
            ACCESS = 1 << 7,
            /// <summary>
            /// Kill password memory permalock -- set to disallow changes to kill password memory lock bit
            /// </summary>
            KILL_PERM = 1 << 8,
            /// <summary>
            /// Kill password [read/write] lock -- set to disallow read and write of kill password, clear to allow read and write
            /// </summary>
            KILL = 1 << 9,
        }

        #endregion

        #region LinkFrequency
        /// <summary>
        /// Gen2 LinkFrequency
        /// </summary>
        public enum LinkFrequency
        {
            /// <summary>
            ///LinkFrequency=40KHZ
            /// </summary>
            LINK40KHZ = 40,
            /// <summary>
            ///LinkFrequency=250KHZ
            /// </summary>
            LINK250KHZ = 250,
            /// <summary>
            ///LinkFrequency=300KHZ
            /// </summary>
            LINK300KHZ = 300,
            /// <summary>
            ///LinkFrequency=400KHZ
            /// </summary>
            LINK400KHZ =400,
            /// <summary>
            /// LinkFrequency=640KHZ
            /// </summary>
            LINK640KHZ =640,
        }

        #endregion

        #region Tari
        /// <summary>
        /// Gen2 Tari Value
        /// </summary>
        public enum Tari
        {
            /// <summary>
            /// Tari = 25us
            /// </summary>
            TARI_25US,
            /// <summary>
            /// Tari = 12.5us
            /// </summary>
            TARI_12_5US,
            /// <summary>
            /// Tari = 6.25us
            /// </summary>
            TARI_6_25US,
        }

        #endregion

        #region WriteMode
        /// <summary>
        /// The mode for write operation
        /// </summary>
        public enum WriteMode
        {
            /// <summary>
            /// use the standard write only
            /// </summary>
            WORD_ONLY,
            /// <summary>
            /// use BlockWrite only
            /// </summary>
            BLOCK_ONLY,
            /// <summary>
            /// use BlockWrite first, if fail, use standard write
            /// </summary>
            BLOCK_FALLBACK


        }

        #endregion

        #endregion

        #region Nested Classes

        #region Password

        /// <summary>
        /// Stores a 32-bit Gen2 password for use as an access or kill password.
        /// </summary>
        public class Password : TagAuthentication
        {
            #region Fields
            /// <summary>
            /// Raw 32-bit password value
            /// </summary>
            internal UInt32 _value;

            #endregion

            /// <summary>
            /// Get Gen2 native 32-bit password value (read-only)
            /// </summary>
            public UInt32 Value
            {
                get { return _value; }
            }

            #region Construction

            /// <summary>
            /// Create a new password object
            /// </summary>
            /// <param name="password">32-bit Gen2 password</param>
            public Password(UInt32 password)
            {
                _value = password;
            }

            #endregion

            #region ToString

            /// <summary>
            /// Human-readable representation
            /// </summary>
            /// <returns>Human-readable representation</returns>
            public override string ToString()
            {
                return Value.ToString("X8");
            }

            #endregion
        }

        #endregion

        #region Q

        /// <summary>
        /// Abstract Gen2 Q class.
        /// </summary>
        public class Q { }

        #endregion

        #region DynamicQ

        /// <summary>
        /// Gen2 Dynamic Q subclass.
        /// </summary>
        public class DynamicQ : Q
        {
            #region ToString

            /// <summary>
            /// Human-readable representation
            /// </summary>
            /// <returns>Human-readable representation</returns>
            public override string ToString()
            {
                return "DynamicQ";
            }

            #endregion
        }
        #endregion

        #region StaticQ

        /// <summary>
        /// Gen2 Static Q subclass.
        /// </summary>
        public class StaticQ : Q
        {
            #region Fields
            /// <summary>
            /// The Q value to use
            /// </summary>
            public byte InitialQ;

            #endregion

            #region Construction

            /// <summary>
            /// Create a static Q algorithim instance with a particular value.
            /// </summary>
            /// <param name="initQ">Q value</param>
            public StaticQ(byte initQ)
            {
                InitialQ = initQ;
            }

            #endregion

            #region ToString

            /// <summary>
            /// Human-readable representation
            /// </summary>
            /// <returns>Human-readable representation</returns>
            public override string ToString()
            {
                return String.Format("StaticQ({0:D})", InitialQ);
            }

            #endregion
        }

        #endregion

        #region TagData

        /// <summary>
        /// Gen2-specific version of TagData
        /// </summary>
        public class TagData : ThingMagic.TagData
        {
            #region Fields

            internal byte[] _pc;

            #endregion

            #region Properties

            /// <summary>
            /// Tag's RFID protocol
            /// </summary>
            public override TagProtocol Protocol
            {
                get { return TagProtocol.GEN2; }
            }

            /// <summary>
            /// PC (Protocol Control) bits
            /// </summary>
            public byte[] PcBytes
            {
                get { return (null != _pc) ? (byte[])_pc.Clone() : null; }
            }

            #endregion

            #region Construction

            /// <summary>
            /// Create TagData with blank CRC
            /// </summary>
            /// <param name="epcBytes">EPC value</param>
            public TagData(ICollection<byte> epcBytes) : base(epcBytes) { }

            /// <summary>
            /// Create TagData
            /// </summary>
            /// <param name="epcBytes">EPC value</param>
            /// <param name="crcBytes">CRC value</param>
            public TagData(ICollection<byte> epcBytes, ICollection<byte> crcBytes) : base(epcBytes, crcBytes) { }

            /// <summary>
            /// Create TagData
            /// </summary>
            /// <param name="epcBytes">EPC value</param>
            /// <param name="crcBytes">CRC value</param>
            /// <param name="pcBytes">PC value</param>
            public TagData(ICollection<byte> epcBytes, ICollection<byte> crcBytes, ICollection<byte> pcBytes)
                : base(epcBytes, crcBytes)
            {
                _pc = (null != pcBytes) ? CollUtil.ToArray(pcBytes) : null;
            }

            #endregion
        }

        #endregion

        #region Select

        /// <summary>
        /// Representation of a Gen2 Select operation
        /// </summary>
        public class Select : TagFilter
        {
            #region Fields

            /// <summary>
            /// Whether tags that meet the comparison are selected or deselected.
            /// false: Get matching tags.
            /// true: Drop matching tags.
            /// </summary>
            public bool Invert;

            /// <summary>
            /// The memory bank in which to compare the mask
            /// </summary>
            public Bank Bank;
            /// <summary>
            /// The location (in bits) at which to begin comparing the mask
            /// </summary>
            public UInt32 BitPointer;
            /// <summary>
            /// The length (in bits) of the mask
            /// </summary>
            public UInt16 BitLength;
            /// <summary>
            /// The mask value to compare with the specified region of tag
            /// memory, MSB first
            /// </summary>
            public byte[] Mask;

            #endregion

            #region Construction

            /// <summary>
            /// Create Gen2 Select
            /// </summary>
            /// <param name="invert"> false: Get matching tags.  true: Drop matching tags.</param>
            /// <param name="bank">The memory bank in which to compare the mask</param>
            /// <param name="bitPointer">The location (in bits) at which to begin comparing the mask</param>
            /// <param name="bitLength">The length (in bits) of the mask</param>
            /// <param name="mask">The mask value to compare with the specified region of tag memory, MSB first</param>
            public Select(bool invert, Bank bank, UInt32 bitPointer,
                          UInt16 bitLength, ICollection<byte> mask)
            {
                this.Invert = invert;
                if (bank == Bank.RESERVED)
                    throw new ArgumentException("Gen2.Select may not operate on reserved memory bank");
                this.Bank = bank;
                this.BitPointer = bitPointer;
                this.BitLength = bitLength;
                this.Mask = CollUtil.ToArray(mask);
            }

            #endregion

            #region Matches

            /// <summary>
            /// Test if a tag Matches this filter. Only applies to selects based
            /// on the EPC.
            /// </summary>
            /// <param name="t">tag data to screen</param>
            /// <returns>Return true to allow tag through the filter.
            /// Return false to reject tag.</returns>
            public bool Matches(ThingMagic.TagData t)
            {
                bool match = true;
                int i, bitAddr;

                if (Bank != Bank.EPC)
                    throw new NotSupportedException("Can't match against non-EPC memory");

                i = 0;
                bitAddr = (int)BitPointer;
                // Matching against the CRC and PC does not have defined
                // behavior; see section 6.3.2.11.1.1 of Gen2 version 1.2.0.
                // We choose to let it match, because that's simple.
                bitAddr -= 32;
                if (bitAddr < 0)
                {
                    i -= bitAddr;
                    bitAddr = 0;
                }

                for (; i < BitLength; i++, bitAddr++)
                {
                    if (bitAddr > (t.EpcBytes.Length * 8))
                    {
                        match = false;
                        break;
                    }
                    // Extract the relevant bit from both the EPC and the mask.
                    if (((t.EpcBytes[bitAddr / 8] >> (7 - (bitAddr & 7))) & 1) !=
                        ((Mask[i / 8] >> (7 - (i & 7))) & 1))
                    {
                        match = false;
                        break;
                    }
                }

                if (Invert)
                    match = match ? false : true;

                return match;
            }
            #endregion

            /// <summary>
            /// Returns a String that represents the current Object.
            /// </summary>
            /// <returns>A String that represents the current Object.</returns>
            public override string ToString()
            {
                return String.Format(
                    "Gen2.Select:[{0}{1},{2},{3},{4}]",
                    (Invert ? "Invert," : ""),
                    Bank, BitPointer, BitLength, ByteFormat.ToHex(Mask));
            }
        }

        #endregion

        #region LockAction

        /// <summary>
        /// Gen2 lock action specifier
        /// </summary>
        public class LockAction : TagLockAction
        {
            #region Static Fields

            private static readonly LockAction _KILL_LOCK = new LockAction(LockBits.KILL | LockBits.KILL_PERM, LockBits.KILL);
            private static readonly LockAction _KILL_UNLOCK = new LockAction(LockBits.KILL | LockBits.KILL_PERM, 0);
            private static readonly LockAction _KILL_PERMALOCK = new LockAction(LockBits.KILL | LockBits.KILL_PERM, LockBits.KILL | LockBits.KILL_PERM);
            private static readonly LockAction _KILL_PERMAUNLOCK = new LockAction(LockBits.KILL | LockBits.KILL_PERM, LockBits.KILL_PERM);
            private static readonly LockAction _ACCESS_LOCK = new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM, LockBits.ACCESS);
            private static readonly LockAction _ACCESS_UNLOCK = new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM, 0);
            private static readonly LockAction _ACCESS_PERMALOCK = new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM, LockBits.ACCESS | LockBits.ACCESS_PERM);
            private static readonly LockAction _ACCESS_PERMAUNLOCK = new LockAction(LockBits.ACCESS | LockBits.ACCESS_PERM, LockBits.ACCESS_PERM);
            private static readonly LockAction _EPC_LOCK = new LockAction(LockBits.EPC | LockBits.EPC_PERM, LockBits.EPC);
            private static readonly LockAction _EPC_UNLOCK = new LockAction(LockBits.EPC | LockBits.EPC_PERM, 0);
            private static readonly LockAction _EPC_PERMALOCK = new LockAction(LockBits.EPC | LockBits.EPC_PERM, LockBits.EPC | LockBits.EPC_PERM);
            private static readonly LockAction _EPC_PERMAUNLOCK = new LockAction(LockBits.EPC | LockBits.EPC_PERM, LockBits.EPC_PERM);
            private static readonly LockAction _TID_LOCK = new LockAction(LockBits.TID | LockBits.TID_PERM, LockBits.TID);
            private static readonly LockAction _TID_UNLOCK = new LockAction(LockBits.TID | LockBits.TID_PERM, 0);
            private static readonly LockAction _TID_PERMALOCK = new LockAction(LockBits.TID | LockBits.TID_PERM, LockBits.TID | LockBits.TID_PERM);
            private static readonly LockAction _TID_PERMAUNLOCK = new LockAction(LockBits.TID | LockBits.TID_PERM, LockBits.TID_PERM);
            private static readonly LockAction _USER_LOCK = new LockAction(LockBits.USER | LockBits.USER_PERM, LockBits.USER);
            private static readonly LockAction _USER_UNLOCK = new LockAction(LockBits.USER | LockBits.USER_PERM, 0);
            private static readonly LockAction _USER_PERMALOCK = new LockAction(LockBits.USER | LockBits.USER_PERM, LockBits.USER | LockBits.USER_PERM);
            private static readonly LockAction _USER_PERMAUNLOCK = new LockAction(LockBits.USER | LockBits.USER_PERM, LockBits.USER_PERM);

            private static readonly Dictionary<string, LockAction> _name2ladict = new Dictionary<string, LockAction>(StringComparer.OrdinalIgnoreCase);

            #endregion

            #region Fields

            private UInt16 _action;
            private UInt16 _mask;

            #endregion

            #region Static Properties

            /// <summary>
            /// Lock Kill Password
            /// </summary>
            public static LockAction KILL_LOCK { get { return _KILL_LOCK; } }

            /// <summary>
            /// Unlock Kill Password
            /// </summary>
            public static LockAction KILL_UNLOCK { get { return _KILL_UNLOCK; } }

            /// <summary>
            /// Permanently Lock Kill Password
            /// </summary>
            public static LockAction KILL_PERMALOCK { get { return _KILL_PERMALOCK; } }

            /// <summary>
            /// Permanently Unlock Kill Password
            /// </summary>
            public static LockAction KILL_PERMAUNLOCK { get { return _KILL_PERMAUNLOCK; } }

            /// <summary>
            /// Lock Access Password
            /// </summary>
            public static LockAction ACCESS_LOCK { get { return _ACCESS_LOCK; } }
            /// <summary>
            /// Unlock Access Password
            /// </summary>
            public static LockAction ACCESS_UNLOCK { get { return _ACCESS_UNLOCK; } }

            /// <summary>
            /// Permanently Lock Access Password
            /// </summary>
            public static LockAction ACCESS_PERMALOCK { get { return _ACCESS_PERMALOCK; } }

            /// <summary>
            /// Permanently Unlock Access Password
            /// </summary>
            public static LockAction ACCESS_PERMAUNLOCK { get { return _ACCESS_PERMAUNLOCK; } }

            /// <summary>
            /// Lock EPC Memory Bank
            /// </summary>
            public static LockAction EPC_LOCK { get { return _EPC_LOCK; } }

            /// <summary>
            /// Unlock EPC Memory Bank
            /// </summary>
            public static LockAction EPC_UNLOCK { get { return _EPC_UNLOCK; } }

            /// <summary>
            /// Permanently Lock EPC Memory Bank
            /// </summary>
            public static LockAction EPC_PERMALOCK { get { return _EPC_PERMALOCK; } }

            /// <summary>
            /// Permanently Unlock EPC Memory Bank
            /// </summary>
            public static LockAction EPC_PERMAUNLOCK { get { return _EPC_PERMAUNLOCK; } }

            /// <summary>
            /// Lock TID Memory Bank
            /// </summary>
            public static LockAction TID_LOCK { get { return _TID_LOCK; } }

            /// <summary>
            /// Unlock TID Memory Bank
            /// </summary>
            public static LockAction TID_UNLOCK { get { return _TID_UNLOCK; } }

            /// <summary>
            /// Permanently Lock TID Memory Bank
            /// </summary>
            public static LockAction TID_PERMALOCK { get { return _TID_PERMALOCK; } }

            /// <summary>
            /// Permanently Unlock TID Memory Bank
            /// </summary>
            public static LockAction TID_PERMAUNLOCK { get { return _TID_PERMAUNLOCK; } }

            /// <summary>
            /// Lock User Memory Bank
            /// </summary>
            public static LockAction USER_LOCK { get { return _USER_LOCK; } }

            /// <summary>
            /// Unlock User Memory Bank
            /// </summary>
            public static LockAction USER_UNLOCK { get { return _USER_UNLOCK; } }

            /// <summary>
            /// Permanently Lock User Memory Bank
            /// </summary>
            public static LockAction USER_PERMALOCK { get { return _USER_PERMALOCK; } }

            /// <summary>
            /// Permanently Unlock User Memory Bank
            /// </summary>
            public static LockAction USER_PERMAUNLOCK { get { return _USER_PERMAUNLOCK; } }

            #endregion

            #region Properties

            /// <summary>
            /// Action field for M5e Lock Tag command
            /// </summary>
            protected internal UInt16 Action
            {
                get { return _action; }
            }

            /// <summary>
            /// Mask field for M5e Lock Tag command
            /// </summary>
            protected internal UInt16 Mask
            {
                get { return _mask; }
            }

            #endregion

            #region Construction

            static LockAction()
            {
                _name2ladict.Add("KILL_LOCK", KILL_LOCK);
                _name2ladict.Add("KILL_UNLOCK", KILL_UNLOCK);
                _name2ladict.Add("KILL_PERMALOCK", KILL_PERMALOCK);
                _name2ladict.Add("KILL_PERMAUNLOCK", KILL_PERMAUNLOCK);
                _name2ladict.Add("ACCESS_LOCK", ACCESS_LOCK);
                _name2ladict.Add("ACCESS_UNLOCK", ACCESS_UNLOCK);
                _name2ladict.Add("ACCESS_PERMALOCK", ACCESS_PERMALOCK);
                _name2ladict.Add("ACCESS_PERMAUNLOCK", ACCESS_PERMAUNLOCK);
                _name2ladict.Add("EPC_LOCK", EPC_LOCK);
                _name2ladict.Add("EPC_UNLOCK", EPC_UNLOCK);
                _name2ladict.Add("EPC_PERMALOCK", EPC_PERMALOCK);
                _name2ladict.Add("EPC_PERMAUNLOCK", EPC_PERMAUNLOCK);
                _name2ladict.Add("TID_LOCK", TID_LOCK);
                _name2ladict.Add("TID_UNLOCK", TID_UNLOCK);
                _name2ladict.Add("TID_PERMALOCK", TID_PERMALOCK);
                _name2ladict.Add("TID_PERMAUNLOCK", TID_PERMAUNLOCK);
                _name2ladict.Add("USER_LOCK", USER_LOCK);
                _name2ladict.Add("USER_UNLOCK", USER_UNLOCK);
                _name2ladict.Add("USER_PERMALOCK", USER_PERMALOCK);
                _name2ladict.Add("USER_PERMAUNLOCK", USER_PERMAUNLOCK);
            }

            /// <summary>
            /// Create Gen2.LockAction out of raw mask and action bitmasks
            /// </summary>
            /// <param name="mask">Lock bits to act on</param>
            /// <param name="action">Lock bit values</param>
            public LockAction(UInt16 mask, UInt16 action)
            {
                _mask = mask;
                _action = action;
            }

            /// <summary>
            /// Create Gen2.LockAction out of enum-wrapped mask and action bitmasks
            /// </summary>
            /// <param name="mask">Lock bits to act on</param>
            /// <param name="action">Lock bit values</param>
            private LockAction(Gen2.LockBits mask, Gen2.LockBits action)
                : this((UInt16)mask, (UInt16)action) { }

            /// <summary>
            /// Create Gen2.LockAction out of other Gen2.LockActions
            /// </summary>
            /// <param name="actions">Lock actions to combine.
            /// If a data field is repeated, the last one takes precedence; e.g.,
            /// Gen2.LockAction.USER_LOCK, Gen2.LockAction.USER_UNLOCK
            /// turns into Gen2.LockAction.USER_UNLOCK.</param>
            public LockAction(params LockAction[] actions)
                : this((UInt16)0, (UInt16)0)
            {
                foreach (LockAction la in actions)
                {
                    // Union mask
                    _mask |= la.Mask;

                    // Overwrite action
                    _action &= (UInt16)(~(la.Mask));
                    _action |= (UInt16)(la.Action & la.Mask);
                }
            }

            #endregion

            #region Parse

            /// <summary>
            /// Convert the string representation into an equivalent object.
            /// </summary>
            /// <param name="value">A string containing the name to convert.
            /// May be the name of a predefined constant, or a comma-separated list of predefined constant names.
            /// </param>
            /// <returns>A LockAction whose value is represented by value.</returns>
            public static LockAction Parse(string value)
            {
                if (null == value)
                    throw new ArgumentNullException("value is null");

                List<Gen2.LockAction> actions = new List<Gen2.LockAction>();

                foreach (string name in value.Split(new char[] { ',' }))
                {
                    if (_name2ladict.ContainsKey(name))
                    {
                        LockAction act = _name2ladict[name];
                        actions.Add(act);
                    }
                    else
                        throw new ArgumentException("Unknown Gen2.LockAction " + value);
                }

                return new Gen2.LockAction(actions.ToArray());
            }

            #endregion

            #region ToString

            /// <summary>
            /// Convert the value of this instance to its equivalent string representation.
            /// </summary>
            /// <returns>A string that represents the current object</returns>
            public override string ToString()
            {
                List<string> names = new List<string>();

                foreach (KeyValuePair<string, LockAction> kv in _name2ladict)
                {
                    string name = kv.Key;
                    LockAction value = kv.Value;

                    // Extract relevant portion of action and mask
                    UInt16 maskpart = (UInt16)(Mask & value.Mask);
                    UInt16 actionpart = (UInt16)(Action & value.Mask);

                    // Compare to predefined constant
                    if ((value.Mask == maskpart) && (value.Action == actionpart))
                        names.Add(name);
                }

                return String.Join(",", names.ToArray());
            }

            #endregion
        }

        #endregion

        #region Tag Embedded Commands

        #region WriteData
        /// <summary>
        /// Embedded Tag Operation: Write Data
        /// </summary>
        public class WriteData : TagOp
        {
            #region Fields

            /// <summary>
            /// Gen2 memory bank to write to
            /// </summary>
            public Gen2.Bank Bank;
            /// <summary>
            /// Word address to start writing at
            /// </summary>
            public UInt32 WordAddress;
            /// <summary>
            /// Data to write
            /// </summary>
            public ushort[] Data;

            #endregion

            #region Construction
            /// <summary>
            /// Constructor to initialize the parameters of WriteData
            /// </summary>
            /// <param name="bank">The memory bank to write</param>
            /// <param name="wordAddress">Write starting address</param>
            /// <param name="data">The data to write</param>
            public WriteData(Gen2.Bank bank, UInt32 wordAddress, ushort[] data)
            {
                this.Bank = bank;
                this.WordAddress = wordAddress;
                this.Data = data;
            }

            #endregion

        }

        #endregion

        #region ReadData
        /// <summary>
        /// Embedded Tag Operation: Read Data
        /// </summary>
        public class ReadData : TagOp
        {
            #region Fields

            /// <summary>
            /// Gen2 memory bank to read from
            /// </summary>
            public Gen2.Bank Bank;
            /// <summary>
            /// Word address to start reading at
            /// </summary>
            public UInt32 WordAddress;
            /// <summary>
            /// Number of words to read
            /// </summary>
            public byte Len;

            #endregion

            #region Construction
            /// <summary>
            /// Constructor to initialize the parameters of ReadData
            /// </summary>
            /// <param name="bank">The memory bank to read</param>
            /// <param name="wordAddress">Read starting address</param>
            /// <param name="length">The length of data to read</param>
            public ReadData(Gen2.Bank bank, UInt32 wordAddress, byte length)
            {
                this.Bank = bank;
                this.WordAddress = wordAddress;
                this.Len = length;
            }

            #endregion

        }

        #endregion

        #region Lock
        /// <summary>
        /// Embedded Tag Operation: Lock
        /// </summary>
        public class Lock : TagOp
        {
            #region Fields

            /// <summary>
            /// Access Password
            /// </summary>
            public UInt32 AccessPassword;

            /// <summary>
            /// Gen2 Lock Action
            /// </summary>
            public LockAction LockAction;

            #endregion

            #region Construction
            /// <summary>
            /// Constructor to initialize the parameters of Lock
            /// </summary>
            /// <param name="accessPassword">The access password</param>
            /// <param name="lockAction">The Gen2 Lock Action</param>
            
            public Lock(UInt32 accessPassword, LockAction lockAction)
            {
                this.AccessPassword = accessPassword;
                this.LockAction = lockAction;
            }

            #endregion

        }
        #endregion

        #region Kill
        /// <summary>
        ///Embedded Tag Operation: Kill
        /// </summary>
        public class Kill : TagOp
        {
            #region Fields

            /// <summary>
            /// Kill password to use to kill the tag
            /// </summary>
            public UInt32 KillPassword;

            #endregion

            #region Construction
            /// <summary>
            /// Constructor to initialize the parameters of Kill
            /// </summary>
            /// <param name="killPassword">Kill password to use to kill the tag</param>
            public Kill(UInt32 killPassword)
            {
                this.KillPassword = killPassword;
            }

            #endregion

        }
        #endregion

        #endregion
        
        #region Tag Commands

        #region WriteTag
        /// <summary>
        /// Write a new ID to a tag.
        /// </summary>
        /// 
        public class WriteTag : TagOp
        {
            #region Fields

            /// <summary>
            /// the new tag ID to write
            /// </summary>
            public TagData Epc;

            #endregion

            #region Construction
            /// <summary>
            /// Constructor to initialize the parameters of WriteTag
            /// </summary>
            /// <param name="epc">the new tag ID to write</param>
            /// 
            public WriteTag(TagData epc)
            {
                this.Epc = epc;
            }

            #endregion

        }

        #endregion

        #region BlockWrite
        /// <summary>
        /// BlockWrite
        /// </summary>
        public class BlockWrite : TagOp
        {
            #region Fields

            /// <summary>
            /// the tag memory bank to write to
            /// </summary>
            public Gen2.Bank Bank;

            /// <summary>
            /// the word address to start writing to
            /// </summary>
            public uint WordPtr;

            /// <summary>
            /// the words to write
            /// </summary>
            public ushort[] Data;
            
            #endregion

            #region Construction
            /// <summary>
            /// Constructor to initialize the parameters of BlockWrite
            /// </summary>
            /// <param name="bank">Gen2 memory bank to write to</param>
            /// <param name="wordPtr">the word address to start writing to</param>
            /// <param name="data">the data to write</param>
            public BlockWrite(Gen2.Bank bank, uint wordPtr, ushort[] data)
            {
                this.Bank = bank;
                this.WordPtr=wordPtr;
                this.Data = data;
            }

            #endregion
        }

        #endregion

        #region BlockPermaLock
        /// <summary>
        /// BlockPermalock
        /// </summary>
        public class BlockPermaLock : TagOp
        {
            #region Fields

            /// <summary>
            /// Read or Lock?
            /// </summary>
            public byte ReadLock;

            /// <summary>
            /// the tag memory bank to lock
            /// </summary>
            public Gen2.Bank Bank;

            /// <summary>
            /// the staring word address to lock
            /// </summary>
            public uint BlockPtr;

            /// <summary>
            /// number of 16 blocks
            /// </summary>
            public byte BlockRange;

            /// <summary>
            /// the Mask
            /// </summary>
            public ushort[] Mask;

            #endregion

            #region Construction
            /// <summary>
            /// Constructor to initialize the parameters of BlockPermaLock
            /// </summary>
            /// <param name="readLock">Read or Lock?</param>
            /// <param name="bank">Gen2 Memory Bank to perform Lock</param>
            /// <param name="blockPtr">starting address of the blocks to operate</param>
            /// <param name="blockRange">number of 16 blocks</param>
            /// <param name="mask">mask</param>
         
            public BlockPermaLock(byte readLock,Gen2.Bank bank, uint blockPtr, byte blockRange, ushort[] mask)
            {
                this.ReadLock = readLock;
                this.Bank = bank;
                this.BlockPtr = blockPtr;
                this.BlockRange = blockRange;
                this.Mask = mask;

            }

            #endregion
        }

        #endregion

        #endregion

        #endregion


    }
}