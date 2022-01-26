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

 ****************************************************************
 * Software to Demonstrate ThingMagic's M6e module.
 * Date: 02/12/2010
 * Author: Kapil Asher
 *****************************************************************
 */

using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.IO;
using System.Threading;
using ThingMagic;
using System.Timers;


namespace Demo_readWrite
{
    
    public partial class readWriteDemo : Form
    {
        public readWriteDemo()
        {
            InitializeComponent();
            // Force default region
            regioncombo.SelectedIndex = 0;
            this.FormClosing += Form1_Closing;
        }

        
        /// <summary>
        /// Define a reader variable
        /// </summary>
        Reader rdr = null;
        
        /// <summary>
        /// Define a Serial Reader variable
        /// </summary>
        SerialReader reader = null;
        
        /// <summary>
        /// Define a region variable
        /// </summary>
        Reader.Region regionToSet = new Reader.Region();
        
        /// <summary>
        /// Define a list of simple read plans for Multiple Read Plan
        /// </summary>
        List<ReadPlan> simpleReadPlans = new List<ReadPlan>();      
        
        /// <summary>
        /// Antenna used for reading
        /// </summary>
        List<int> ant = new List<int>();
        
        /// <summary>
        /// Define a protocol variable for access commands
        /// </summary>
        List<TagProtocol> tagOpProto = new List<TagProtocol>();

        string warningText;
        System.Timers.Timer myTimer = new System.Timers.Timer();
        
        /// <summary>
        /// This exception is thrown when antenna for tag operation is not selected
        /// </summary>
        public class TagOpAntennaException : ReaderException
        {
            public TagOpAntennaException(string message) : base(message) { }
        }

        /// <summary>
        /// This exception is thrown when protocol for tag operation is not selected
        /// </summary>
        public class TagOpProtocolException : ReaderException
        {
            public TagOpProtocolException(string message) : base(message) { }
        }

        /// <summary>
        /// Define a variable for selection criteria
        /// </summary>
        TagFilter selectionOnEPC = null;        
        
        /// <summary>
        /// Sub Class inherited from TagReadData for 
        /// altering the Date/Time to current system Date/Time
        /// </summary>
        public class localTagReadData : TagReadData
        {
            TagReadData originalTagReadData;
            DateTime myBaseTime = new DateTime();
            DateTime dateTime;
            int antenna;
        
            /// <summary>
            /// EPC of tag
            /// </summary>
            public new byte[] Epc
            {
                get { return originalTagReadData.Epc; }
            }
            
            /// <summary>
            /// EPC of Tag (string format)
            /// </summary>
            public new string EpcString
            {
                get { return originalTagReadData.EpcString; }
            }
            
            /// <summary>
            /// Read Data Bytes
            /// </summary>
            public new byte[] Data
            {
                get { return originalTagReadData.Data; }
            }
            
            /// <summary>
            /// [1-based] numeric identifier of antenna that tag was read on
            /// </summary>
            public new int Antenna
            {
                get { return antenna; }
            }
            //ST
            /// <summary>
            /// RSSI units
            /// </summary>
            public new int Rssi
            {
                get { return originalTagReadData.Rssi; }
            }           
            
            /// <summary>
            /// Tag that was read
            /// </summary>
            public new TagData Tag
            {
                get { return originalTagReadData.Tag; }
            }
            
            /// <summary>
            /// Current Date/Time
            /// </summary>
            public new DateTime Time
            {               
                get { return (dateTime.AddMilliseconds((double)(originalTagReadData.Time - myBaseTime).TotalMilliseconds)) ; }
            }
            public override string ToString()
            {
                return originalTagReadData.ToString();
            }
            
            /// <summary>
            /// Constructor for localReadTagData
            /// </summary>
            /// <param name="original">Original TagReadData of the Tag Read</param>
            /// <param name="dateTime">Current Date/Time</param>
            /// <param name="reader">Reader reference, for antenna mapping and other reader-specific conversions</param>
            public localTagReadData(TagReadData original, DateTime dateTime, SerialReader reader)
            {
                this.originalTagReadData = original;
                this.dateTime = dateTime;
                this.ReadCount = original.ReadCount;

                this.antenna = 0;
                int sertx = (original.Antenna >> 4) & 0xF;
                int serrx = (original.Antenna >> 0) & 0xF;
                int[][] txrxmap = (int[][])reader.ParamGet("/reader/antenna/txRxMap");
                foreach (int[] row in txrxmap)
                {
                    int virt = row[0];
                    int tx = row[1];
                    int rx = row[2];
                    if ((sertx == tx) && (serrx == rx))
                    {
                        this.antenna = virt;
                        break;
                    }
                }
            }
        }       
               
        /// <summary>
        /// Initializes the Reader connected to the reader address field
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void initReader_Click(object sender, EventArgs e)
        {
            initReader.Enabled = false;
            switch (regioncombo.Text)
            {
                case "North America (1)":
                    this.regionToSet = Reader.Region.NA;
                    break;
                case "Korea (3)":
                    this.regionToSet = Reader.Region.KR;
                    break;
                case "P.R. China (6)":
                    this.regionToSet = Reader.Region.PRC;
                    break;
                case "EU3 revised (8)":
                    this.regionToSet = Reader.Region.EU3;
                    break;
                case "Open (255)":
                    this.regionToSet = Reader.Region.OPEN;
                    break;
                default:
                    this.regionToSet = Reader.Region.OPEN;
                    break;
            }
            try
            {
                //Starts the reader from default state                
                reader.Destroy();
            }
            catch {}
            try
            {
                if (readerAddr.Text.Contains("COM") || readerAddr.Text.Contains("com"))
                {
                    ///Creates a Reader Object for operations on the Reader.
                    rdr = Reader.Create(string.Concat("tmr:///", readerAddr.Text));
                }                               
                //rdr.ParamSet("/reader/transportTimeout", int.Parse(timeout.Text) + 5000);               
                //rdr.ParamSet("/reader/region/id", regionToSet);
                ///Assign serial reader to rdr
                reader = (SerialReader)rdr;
                reader.Connect();
                reader.ParamSet("/reader/transportTimeout", int.Parse(timeout.Text) + 5000);  
                reader.ParamSet("/reader/region/id", (Reader.Region)regionToSet);
               //ST
                frequencysetting();  // se nel textbox relativo è selazionato un com vado a settare il reader con una serie di parametri tra cui, grazie a questa funzione, anche la singola frequenza con cui voglio lavorare  
                
                // UInt32[] aaa;
                // aaa = new UInt32[13];
                // aaa = (UInt32[])(reader.ParamGet("/reader/region/hopTable"));//.Parse(frequenza.Text) * 1000); //Settaggio frequenza in Mhz

                // UInt32 freq = (UInt32)(int.Parse(frequenza.Text) * 1000);
                // int h;
                //for(h=0;h<13;h++) aaa[h]=freq;

                // reader.ParamSet("/reader/region/hopTable", aaa);

                 //UInt32[] bbb;
                 //bbb = new UInt32[13];
                 //bbb = (UInt32[])(reader.ParamGet("/reader/region/hopTable"));//.Parse(frequenza.Text) * 1000); //Settaggio frequenza in Mhz


                this.initReader.BackColor = System.Drawing.Color.YellowGreen;
                baudRateToolStripMenuItem.Enabled = true;
                thingMagicReaderToolStripMenuItem.Enabled = true;
                rFPowerLevelToolStripMenuItem.Enabled = true;
                startRead.Enabled = true;
                readTag.Enabled = true;
                writeTagButton.Enabled = true;
                readTagData.Enabled = true;
                writeTagData.Enabled = true;
            }
            catch (System.IO.IOException)
            {
                if (!readerAddr.Text.Contains("COM"))
                {
                    MessageBox.Show("Application needs a valid Reader Address of type COMx", "Error", MessageBoxButtons.OK);
                }
                else
                {
                    MessageBox.Show("Reader not connected on " + readerAddr.Text, "Error", MessageBoxButtons.OK);
                }
                initReader.Enabled = true;
            }
            catch (ReaderCodeException ex)
            {
                MessageBox.Show("Error connecting to reader: " + ex.Message.ToString(), "Error!", MessageBoxButtons.OK);
            }
            catch (System.UnauthorizedAccessException)
            {
                MessageBox.Show("Access to " + readerAddr.Text + " denied. Please check if another program is accessing this port", "Error!", MessageBoxButtons.OK);
            }
        }
        
        /// <summary>
        /// Delegate for updating Read Tag ID text box
        /// </summary>
        /// <param name="data">String Data</param>
        delegate void OutputUpdateDelegate(string data);
        
        /// <summary>
        /// Method for updating Read Tag ID text box
        /// </summary>
        /// <param name="data">String Data</param>
        

        private void OutputUpdateReadTagID(string data)
        {
            if (data.Length != 0)
            {
                readTagID.Text = data;
            }
            else
            {
                readTagID.Text = "";
            }
        }
        
        /// <summary>
        /// Method to invoke OutputUpdateDelegate
        /// </summary>
        /// <param name="data">String Data</param>       
        public void UpdateReadTagIDBox(string data)
        {
            if (readTagID.InvokeRequired)
            {
                readTagID.BeginInvoke(new OutputUpdateDelegate(OutputUpdateReadTagID), new object[] { data });
            }
            else
            {
                OutputUpdateReadTagID(data);
            }
        }    
       
        /// <summary>
        /// Initiates a Read Tag ID Single for the timeout given in the timeout Control Box.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void readTag_Click(object sender, EventArgs e)            
        {
            fontSize_TextChanged_1(sender, e);
            simpleReadPlans.Clear();
            ant.Clear();
            try
            {
                if (antenna1ToolStripMenuItem.Checked)
                {
                    ant.Add(1);
                }
                if (antenna2ToolStripMenuItem.Checked)
                {
                    ant.Add(2);
                }
                if (antenna3ToolStripMenuItem.Checked)
                {
                    ant.Add(3);
                }
                if (antenna4ToolStripMenuItem.Checked)
                {
                    ant.Add(4);
                }                
                if (readOnAllConnectedAntennasToolStripMenuItem.Checked)
                {
                    ant.AddRange((int[])rdr.ParamGet("/reader/antenna/connectedPortList"));                   
                }
                this.allAvailableProtocolLabel.BackColor = System.Drawing.SystemColors.Control;
                this.gen2Label.BackColor = System.Drawing.SystemColors.Control;
                this.isoLabel.BackColor = System.Drawing.SystemColors.Control;
                this.ipx64Label.BackColor = System.Drawing.SystemColors.Control;
                this.ipx256Label.BackColor = System.Drawing.SystemColors.Control;
                if (gEN2ToolStripMenuItem.Checked)
                {
                    SimpleReadPlan srpGen2 = new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2);
                    this.gen2Label.BackColor = System.Drawing.Color.GreenYellow;
                    simpleReadPlans.Add(srpGen2);
                }
                if (iSO180006BToolStripMenuItem.Checked)
                {
                    SimpleReadPlan srpIso = new SimpleReadPlan(ant.ToArray(), TagProtocol.ISO180006B);
                    this.isoLabel.BackColor = System.Drawing.Color.GreenYellow;
                    simpleReadPlans.Add(srpIso);                    
                }
                if (iPX64KHzToolStripMenuItem.Checked)
                {
                    SimpleReadPlan srpIpx64 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX64);
                    this.ipx64Label.BackColor = System.Drawing.Color.GreenYellow;
                    simpleReadPlans.Add(srpIpx64);                    
                }
                if (iPX256KHzToolStripMenuItem.Checked)
                {
                    SimpleReadPlan srpIpx256 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX256);
                    this.ipx256Label.BackColor = System.Drawing.Color.GreenYellow;
                    simpleReadPlans.Add(srpIpx256);
                }
                if (readOnAllAvailableProtocolsToolStripMenuItem.Checked)
                {
                    this.allAvailableProtocolLabel.BackColor = System.Drawing.Color.GreenYellow;
                    SimpleReadPlan srpGen2 = new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2);
                    simpleReadPlans.Add(srpGen2);
                    SimpleReadPlan srpIso = new SimpleReadPlan(ant.ToArray(), TagProtocol.ISO180006B);
                    simpleReadPlans.Add(srpIso);
                    SimpleReadPlan srpIpx64 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX64);
                    simpleReadPlans.Add(srpIpx64);
                    SimpleReadPlan srpIpx256 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX256);
                    simpleReadPlans.Add(srpIpx256);
                }
                if (simpleReadPlans.ToArray().Length == 0)
                {
                    reader.ParamSet("/reader/read/plan", new SimpleReadPlan(ant.ToArray(),TagProtocol.GEN2));
                }
                else
                {
                    MultiReadPlan multiPlan = new MultiReadPlan(simpleReadPlans);
                    reader.ParamSet("/reader/read/plan", multiPlan);
                }
            }

            catch { }

            try
            {
               //ST
                string testodasalvare = saveFileDialogFileTesto.FileName; //nella variabile stringa chiamata testodasalvare ci memorizzo il campo FileName di saveFileDialogFileTesto
                StreamWriter sw = null;
                if (testodasalvare != "")
                    sw = new StreamWriter(testodasalvare, true);
                UpdateReadTagIDBox("");
                string placeholder = "";
                int dimensione=readTagID.Width;
                DateTime timeBeforeRead = DateTime.Now;
                //    //ST
                frequencysetting();
            
                TagReadData[] tagID = rdr.Read(int.Parse(timeout.Text));

            
                DateTime timeAfterRead = DateTime.Now;
                TimeSpan timeElapsed = timeAfterRead - timeBeforeRead;
                for (int i = 0; i < tagID.Length; i++)
                {
                    // Modificare la stringa per leggere i parametri in più.
                    //placeholder += string.Concat(tagID[i].Time, "\t\t", tagID[i].ReadCount, "\t\t", tagID[i].Antenna, "\t\t", (tagID[i].Tag.Protocol == TagProtocol.ISO180006B) ? "ISO18K6B" : tagID[i].Tag.Protocol.ToString(), "\t\t", tagID[i].Rssi,"\t","0x"  + tagID[i].EpcString, "\r\n");
               //ST
                    placeholder += string.Concat(tagID[i].Time, "\t", tagID[i].ReadCount, "\t", tagID[i].Antenna, "\t", (tagID[i].Tag.Protocol == TagProtocol.ISO180006B) ? "ISO18K6B" : tagID[i].Tag.Protocol.ToString(), "\t\t", tagID[i].Rssi, "\t",  tagID[i].Phase,"\t", tagID[i].Frequency,"\t\t", "0x" + tagID[i].EpcString, "\r\n");
                    //placeholder += string.Concat(String.Format("{0,-24}", tagID[i].Time),"\t\t", String.Format("{0,-4}", tagID[i].ReadCount), String.Format("{0,-2}", tagID[i].Antenna), (tagID[i].Tag.Protocol == TagProtocol.ISO180006B) ? String.Format("{0,-10}", "ISO18K6B") : String.Format("{0,-10}", tagID[i].Tag.Protocol.ToString()), String.Format("{0,-3}", tagID[i].Rssi), String.Format("{0,-4}", tagID[i].Phase), String.Format("{0,-10}", tagID[i].Frequency), String.Format("{0,-28}", "0x" + tagID[i].EpcString), "\r\n");//PARTE CHE SI OCCUPA DELL'ALLINEAMENTO
                    if (testodasalvare != "")
                    {
                        sw.WriteLine(placeholder);
                        sw.Flush();
                    }
                }                
                UpdateReadTagIDBox(placeholder);
                totalTimeElapsedTextBox.Text = timeElapsed.TotalSeconds.ToString();
                totalTagsReadTextBox.Text = tagID.Length.ToString();
                if (testodasalvare != "") sw.Close();
            }
            catch (ReaderCodeException ex)
            {
                if ((ex.Code == 0x504) || (ex.Code == 0x505))
                {
                    switch (ex.Code)
                    {
                        case 0x504:
                            warningText = "Over Temperature";
                            break;
                        case 0x505:
                            warningText = "High Return Loss";
                            break;
                        default:
                            warningText = "warning";
                            break;
                    }
                    myTimer.Elapsed += new ElapsedEventHandler(TimeEvent);
                    myTimer.Interval =  2000;
                    myTimer.Start();
                    GUIshowWarning();
                }
                else
                {
                    MessageBox.Show(ex.Message, "Reader Message", MessageBoxButtons.OK);
                }
            }
            catch (ReaderCommException ex)
            {
                MessageBox.Show(ex.Message, "Reader Message", MessageBoxButtons.OK);
            }

        }      
        /// <summary>
        /// Timeout used for synchronous and asynchronous reads
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void timeout_TextChanged(object sender, EventArgs e)
        {
            try
            {
                if(timeout.Text!="")
                    reader.ParamSet("/reader/transportTimeout", int.Parse(timeout.Text) + 5000);
            }
            catch { }
        }
       

        private void label4_Click(object sender, EventArgs e)
        {

        }

        /// <summary>
        /// Validates EPC String and formats valid strings into EPC
        /// </summary>
        /// <param name="epcString">Valid/Invalid EPC String</param>
        /// <returns>Valid TagData used for selection</returns>
        private TagData validateEpcStringAndReturnTagData(string epcString)
        {
            TagData validTagData = null;
            if (epcString.Contains(" "))
            {
                 validTagData = null;
            }            
            else
            {                
                if (epcString.Length == 0)
                {
                    validTagData = null;
                }
                else 
                {
                    if (epcString.Contains("0x"))
                    {
                        validTagData = new TagData(epcString.Remove(0, 2));
                    }
                    validTagData = new TagData(epcString);
                }                
            }
            return validTagData;
        }
        
        /// <summary>
        /// Chenges the font size of the tags read
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void fontSize_TextChanged_1(object sender, EventArgs e)
        {
            try
            {
                if (fontSize.Text == null) { }
                else
                {
                    this.readTagID.Font = new System.Drawing.Font("Microsoft Sans Serif", float.Parse(fontSize.Text), System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
                }
            }
            catch { }
        }

        void restartReading()
        {
            totalTagsReadTextBox.Text = "0";
            totalTimeElapsedTextBox.Text = "0";
            totalTimeElapsedTextBox.Enabled = false;
            totalTagsReadTextBox.Enabled = false;
            totalTagsReadLabel.Enabled = false;
            inLabel.Enabled = false;
            secondsLabel.Enabled = false;
            commandSecLabel.Enabled = false;
            commandTotalTimeTextBox.Enabled = false;
            commandTimeLabel.Enabled = false;
            baudRateToolStripMenuItem.Enabled = false;
            thingMagicReaderToolStripMenuItem.Enabled = false;
            rFPowerLevelToolStripMenuItem.Enabled = false;
            readOnAntennaNumberToolStripMenuItem.Enabled = false;
            protocolToolStripMenuItem.Enabled = false;
            bankToReadToolStripMenuItem.Enabled = false;
            this.allAvailableProtocolLabel.BackColor = System.Drawing.SystemColors.Control;
            this.gen2Label.BackColor = System.Drawing.SystemColors.Control;
            this.isoLabel.BackColor = System.Drawing.SystemColors.Control;
            this.ipx64Label.BackColor = System.Drawing.SystemColors.Control;
            this.ipx256Label.BackColor = System.Drawing.SystemColors.Control;
            WarningLabel.BackColor = System.Drawing.Color.GreenYellow;
            WarningLabel.Text = "No Warnings";
            simpleReadPlans.Clear();
            ant.Clear();


            if (antenna1ToolStripMenuItem.Checked)
            {
                ant.Add(1);
            }
            if (antenna2ToolStripMenuItem.Checked)
            {
                ant.Add(2);
            }
            if (antenna3ToolStripMenuItem.Checked)
            {
                ant.Add(3);
            }
            if (antenna4ToolStripMenuItem.Checked)
            {
                ant.Add(4);
            }
            if (readOnAllConnectedAntennasToolStripMenuItem.Checked)
            {
                ant.AddRange((int[])rdr.ParamGet("/reader/antenna/connectedPortList"));
            }

            if (gEN2ToolStripMenuItem.Checked)
            {
                SimpleReadPlan srpGen2 = new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2);
                this.gen2Label.BackColor = System.Drawing.Color.GreenYellow;
                simpleReadPlans.Add(srpGen2);
            }
            if (iSO180006BToolStripMenuItem.Checked)
            {
                SimpleReadPlan srpIso = new SimpleReadPlan(ant.ToArray(), TagProtocol.ISO180006B);
                this.isoLabel.BackColor = System.Drawing.Color.GreenYellow;
                simpleReadPlans.Add(srpIso);
            }
            if (iPX64KHzToolStripMenuItem.Checked)
            {
                SimpleReadPlan srpIpx64 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX64);
                this.ipx64Label.BackColor = System.Drawing.Color.GreenYellow;
                simpleReadPlans.Add(srpIpx64);
            }
            if (iPX256KHzToolStripMenuItem.Checked)
            {
                SimpleReadPlan srpIpx256 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX256);
                this.ipx256Label.BackColor = System.Drawing.Color.GreenYellow;
                simpleReadPlans.Add(srpIpx256);
            }
            if (readOnAllAvailableProtocolsToolStripMenuItem.Checked)
            {
                this.allAvailableProtocolLabel.BackColor = System.Drawing.Color.GreenYellow;
                SimpleReadPlan srpGen2 = new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2);
                simpleReadPlans.Add(srpGen2);
                SimpleReadPlan srpIso = new SimpleReadPlan(ant.ToArray(), TagProtocol.ISO180006B);
                simpleReadPlans.Add(srpIso);
                SimpleReadPlan srpIpx64 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX64);
                simpleReadPlans.Add(srpIpx64);
                SimpleReadPlan srpIpx256 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX256);
                simpleReadPlans.Add(srpIpx256);
            }
            if (simpleReadPlans.ToArray().Length == 0)
            {
                reader.ParamSet("/reader/read/plan", new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2));
            }
            else
            {
                MultiReadPlan multiPlan = new MultiReadPlan(simpleReadPlans);
                reader.ParamSet("/reader/read/plan", multiPlan);
            }
            reader.ReadException += ReadException;
            startRead.Text = "Stop Reads";
            readTag.Enabled = false;
            writeTagButton.Enabled = false;
            writeTagData.Enabled = false;
            readTagData.Enabled = false;
            selectionOnEPC = validateEpcStringAndReturnTagData(tagIDtoFind.Text);
            reader.ParamSet("/reader/read/asyncOnTime", int.Parse(timeout.Text));
            //ST
            frequencysetting();


            reader.ParamSet("/reader/read/asyncOffTime", 2000);
            reader.TagRead += PrintTagRead;
            reader.StartReading();
        }


        void TimeEvent(object sender, ElapsedEventArgs e)
        {
            myTimer.Stop();
            WarningLabel.Invoke(new del(GUIturnoffWarning));
        }

        void stopReading()
        {
            reader.StopReading();
            reader.TagRead -= PrintTagRead;
            reader.ReadException -= ReadException;
        }

        void GUIshowWarning()
        {
            WarningLabel.BackColor = System.Drawing.Color.Red;
            WarningLabel.Text = warningText;
        }

        void GUIturnoffWarning()
        {
            WarningLabel.BackColor = System.Drawing.Color.GreenYellow;
            WarningLabel.Text = "No Warnings";
        }

        delegate void del();

        void updateGUIatStop()
        {
            WarningLabel.BackColor = System.Drawing.Color.Red;
            WarningLabel.Text = warningText;
            startRead.Text = "Start Reads";
            readTag.Enabled = true;
            writeTagButton.Enabled = true;
            writeTagData.Enabled = true;
            readTagData.Enabled = true;
            totalTimeElapsedTextBox.Enabled = true;
            totalTagsReadTextBox.Enabled = true;
            totalTagsReadLabel.Enabled = true;
            inLabel.Enabled = true;
            secondsLabel.Enabled = true;
            commandSecLabel.Enabled = true;
            commandTotalTimeTextBox.Enabled = true;
            commandTimeLabel.Enabled = true;
            baudRateToolStripMenuItem.Enabled = true;
            thingMagicReaderToolStripMenuItem.Enabled = true;
            rFPowerLevelToolStripMenuItem.Enabled = true;
            readOnAntennaNumberToolStripMenuItem.Enabled = true;
            protocolToolStripMenuItem.Enabled = true;
            bankToReadToolStripMenuItem.Enabled = true;
        }


        /// <summary>
        /// Synchronous Reads Exception
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        void ReadException(Object sender, ReaderExceptionEventArgs e)
        {

            if ((e.ReaderException is ReaderCodeException) && ((((ReaderCodeException)e.ReaderException).Code == 0x504) || (((ReaderCodeException)e.ReaderException).Code == 0x505)))
            {
                switch(((ReaderCodeException)e.ReaderException).Code)
                {
                    case 0x504:
                        warningText = "Over Temperature";
                        break;
                    case 0x505:
                        warningText = "High Return Loss";
                        break;
                    default:
                        warningText = "warning";
                        break;

                }
                    stopReading();
                    startRead.Invoke(new del(updateGUIatStop));
                    Thread.Sleep(2000);
                    startRead.Invoke(new del(restartReading));
            }
            else
            {
                MessageBox.Show(e.ReaderException.Message
                    + "\r\nPlease click Stop Reads and continue"
                    , "Reader Message", MessageBoxButtons.OK);
            }
        }

        /// <summary>
        /// Starts a Reading Tags until The Stop button is pressed.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void startRead_Click(object sender, EventArgs e)
        {
            try
            {
                
                if (startRead.Text == "Start Reads")
                {
                    totalTagsReadTextBox.Text = "0";
                    totalTimeElapsedTextBox.Text = "0";
                    totalTimeElapsedTextBox.Enabled = false;
                    totalTagsReadTextBox.Enabled = false;
                    totalTagsReadLabel.Enabled = false;
                    inLabel.Enabled = false;
                    secondsLabel.Enabled = false;
                    commandSecLabel.Enabled = false;
                    commandTotalTimeTextBox.Enabled = false;
                    commandTimeLabel.Enabled = false;
                    baudRateToolStripMenuItem.Enabled = false;
                    thingMagicReaderToolStripMenuItem.Enabled = false;
                    rFPowerLevelToolStripMenuItem.Enabled = false;
                    readOnAntennaNumberToolStripMenuItem.Enabled = false;
                    protocolToolStripMenuItem.Enabled = false;
                    bankToReadToolStripMenuItem.Enabled = false;
                    this.allAvailableProtocolLabel.BackColor = System.Drawing.SystemColors.Control;
                    this.gen2Label.BackColor = System.Drawing.SystemColors.Control;
                    this.isoLabel.BackColor = System.Drawing.SystemColors.Control;
                    this.ipx64Label.BackColor = System.Drawing.SystemColors.Control;
                    this.ipx256Label.BackColor = System.Drawing.SystemColors.Control;
                    fontSize_TextChanged_1(sender, e);
                    simpleReadPlans.Clear();
                    ant.Clear();


                    if (antenna1ToolStripMenuItem.Checked)
                    {
                        ant.Add(1);
                    }
                    if (antenna2ToolStripMenuItem.Checked)
                    {
                        ant.Add(2);
                    }
                    if (antenna3ToolStripMenuItem.Checked)
                    {
                        ant.Add(3);
                    }
                    if (antenna4ToolStripMenuItem.Checked)
                    {
                        ant.Add(4);
                    }
                    if (readOnAllConnectedAntennasToolStripMenuItem.Checked)
                    {
                        ant.AddRange((int[])rdr.ParamGet("/reader/antenna/connectedPortList"));
                    }

                    if (gEN2ToolStripMenuItem.Checked)
                    {
                        SimpleReadPlan srpGen2 = new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2);
                        this.gen2Label.BackColor = System.Drawing.Color.GreenYellow;
                        simpleReadPlans.Add(srpGen2);
                    }
                    if (iSO180006BToolStripMenuItem.Checked)
                    {
                        SimpleReadPlan srpIso = new SimpleReadPlan(ant.ToArray(), TagProtocol.ISO180006B);
                        this.isoLabel.BackColor = System.Drawing.Color.GreenYellow;
                        simpleReadPlans.Add(srpIso);
                    }
                    if (iPX64KHzToolStripMenuItem.Checked)
                    {
                        SimpleReadPlan srpIpx64 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX64);
                        this.ipx64Label.BackColor = System.Drawing.Color.GreenYellow;
                        simpleReadPlans.Add(srpIpx64);
                    }
                    if (iPX256KHzToolStripMenuItem.Checked)
                    {
                        SimpleReadPlan srpIpx256 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX256);
                        this.ipx256Label.BackColor = System.Drawing.Color.GreenYellow;
                        simpleReadPlans.Add(srpIpx256);
                    }
                    if (readOnAllAvailableProtocolsToolStripMenuItem.Checked)
                    {
                        this.allAvailableProtocolLabel.BackColor = System.Drawing.Color.GreenYellow;
                        SimpleReadPlan srpGen2 = new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2);
                        simpleReadPlans.Add(srpGen2);
                        SimpleReadPlan srpIso = new SimpleReadPlan(ant.ToArray(), TagProtocol.ISO180006B);
                        simpleReadPlans.Add(srpIso);
                        SimpleReadPlan srpIpx64 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX64);
                        simpleReadPlans.Add(srpIpx64);
                        SimpleReadPlan srpIpx256 = new SimpleReadPlan(ant.ToArray(), TagProtocol.IPX256);
                        simpleReadPlans.Add(srpIpx256);
                    }
                    if (simpleReadPlans.ToArray().Length == 0)
                    {
                        reader.ParamSet("/reader/read/plan", new SimpleReadPlan(ant.ToArray(), TagProtocol.GEN2));
                    }
                    else
                    {
                        MultiReadPlan multiPlan = new MultiReadPlan(simpleReadPlans);
                        reader.ParamSet("/reader/read/plan", multiPlan);
                    }
                    reader.ReadException += ReadException;
                    startRead.Text = "Stop Reads";
                    readTag.Enabled = false;
                    writeTagButton.Enabled = false;
                    writeTagData.Enabled = false;
                    readTagData.Enabled = false;
                    selectionOnEPC = validateEpcStringAndReturnTagData(tagIDtoFind.Text);
                    reader.ParamSet("/reader/read/asyncOnTime", int.Parse(timeout.Text));
                    //ST
                    frequencysetting();


                    reader.ParamSet("/reader/read/asyncOffTime", 2000);
                    reader.TagRead += PrintTagRead;
                    if (selectionOnEPC != null)
                    {
                        clear_Click(this, e);
                    }
                    reader.StartReading();
                }

                else if (startRead.Text == "Stop Reads")
                {
                    reader.StopReading();
                    reader.TagRead -= PrintTagRead;
                    reader.ReadException -= ReadException;
                    startRead.Text = "Start Reads";
                    readTag.Enabled = true;
                    writeTagButton.Enabled = true;
                    writeTagData.Enabled = true;
                    readTagData.Enabled = true;
                    totalTimeElapsedTextBox.Enabled = true;
                    totalTagsReadTextBox.Enabled = true;
                    totalTagsReadLabel.Enabled = true;
                    inLabel.Enabled = true;
                    secondsLabel.Enabled = true;
                    commandSecLabel.Enabled = true;
                    commandTotalTimeTextBox.Enabled = true;
                    commandTimeLabel.Enabled = true;
                    baudRateToolStripMenuItem.Enabled = true;
                    thingMagicReaderToolStripMenuItem.Enabled = true;
                    rFPowerLevelToolStripMenuItem.Enabled = true;
                    readOnAntennaNumberToolStripMenuItem.Enabled = true;
                    protocolToolStripMenuItem.Enabled = true;
                    bankToReadToolStripMenuItem.Enabled = true;
                }
            }            
            catch (Exception exp)
            {

                if (exp is ReaderCodeException)
                {
                    switch (MessageBox.Show(exp.Message.ToString() + "\nStart/Stop Tag Reads Failed. Try Again", "Error", MessageBoxButtons.RetryCancel))
                    {
                        case DialogResult.Retry:
                            {
                                startRead_Click(this, e);
                                startRead.Text = "Start Reads (EPC Only)";
                                startRead_Click(this, e);
                                break;
                            }
                        default:
                            break;
                    }


                }
                if (exp is ReaderCommException)
                {                    
                    handleReaderCommException((ReaderCommException)exp, e);
                }
            }
        }

        /// <summary>
        /// Dictionary to define Tag DataBase
        /// </summary>
        Dictionary<string,TagReadData> tagDataBase = new Dictionary<string,TagReadData>();             
        
        /// <summary>
        /// Function that processes the Tag Data produced by StartReading();
        /// </summary>
        /// <param name="read"></param>
        void PrintTagRead(Object sender, TagReadDataEventArgs e)
        {
           //ST
            string testodasalvare = saveFileDialogFileTesto.FileName;
            StreamWriter sw = null;
            if(testodasalvare !="") 
                sw= new StreamWriter(testodasalvare, true);

            TagReadData read = e.TagReadData;            
            UpdateReadTagIDBox("");
            TagReadData forDataBase;
            string placeHolder = "";   
            lock (tagDataBase)
                       {
                if (tagDataBase.ContainsKey(read.EpcString))
                {
                    forDataBase = tagDataBase[read.EpcString];
                    tagDataBase.Remove(read.EpcString);
                    read.ReadCount += forDataBase.ReadCount;
                    tagDataBase.Add(read.EpcString, read);
                }
                else
                {
                    tagDataBase.Add(read.EpcString, read);
                }
                foreach (KeyValuePair<string, TagReadData> kvp in tagDataBase)
                {
             
                    placeHolder += string.Concat(kvp.Value.Time, "\t", kvp.Value.ReadCount, "\t", kvp.Value.Antenna, "\t", (kvp.Value.Tag.Protocol == TagProtocol.ISO180006B) ? "ISO18K6B" : kvp.Value.Tag.Protocol.ToString(), "\t\t", kvp.Value.Rssi,"\t", kvp.Value.Phase,"\t" , kvp.Value.Frequency,"\t\t","0x" + kvp.Value.EpcString, "\r\n");
                    if(testodasalvare !="") {
                    sw.WriteLine(placeHolder);
                    sw.Flush();
                    }
                }
                UpdateReadTagIDBox(placeHolder);
                if(testodasalvare !="")  sw.Close();
            }
        }        
        
        /// <summary>
        /// Clears Read Tag
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void clear_Click(object sender, EventArgs e)
        {
            lock (tagDataBase)
            {
                tagDataBase.Clear();
                UpdateReadTagIDBox("");
            }
        }       

        private void antennaID_TextChanged(object sender, EventArgs e)
        {

        }

     

        private void label3_Click_1(object sender, EventArgs e)
        {

        }

        private void saveFile_Click(object sender, EventArgs e)
        {
           
        }

        /// <summary>
        /// Sets up the tag operation protocol and antenna based on  Option Menu Bar
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void tagOpProtocolAntenna(object sender, EventArgs e)
        {
            if (readOnAllConnectedAntennasToolStripMenuItem.Checked)
            {
                throw new TagOpAntennaException("Please select a single antenna port for this operation");
            }
            else
            {
                ant.Clear();
                if (antenna1ToolStripMenuItem.Checked)
                {
                    ant.Add(1);
                }
                if (antenna2ToolStripMenuItem.Checked)
                {
                    ant.Add(2);
                }
                if (antenna3ToolStripMenuItem.Checked)
                {
                    ant.Add(3);
                }
                if (antenna4ToolStripMenuItem.Checked)
                {
                    ant.Add(4);
                }
                if ((ant.ToArray().Length == 0) || (ant.ToArray().Length > 1))
                {
                    throw new TagOpAntennaException("Please select a single antenna port for this operation");
                }
                else
                {
                    reader.ParamSet("/reader/tagop/antenna", ant.ToArray()[0]);
                }
            }
            if (readOnAllAvailableProtocolsToolStripMenuItem.Checked)
            {
                throw new TagOpProtocolException("Please select a single protocol for this operation");
            }
            else
            {
                tagOpProto.Clear();
                if (gEN2ToolStripMenuItem.Checked)
                {
                    tagOpProto.Add(TagProtocol.GEN2);
                }
                if (iSO180006BToolStripMenuItem.Checked)
                {
                    tagOpProto.Add(TagProtocol.ISO180006B);
                }
                if (iPX64KHzToolStripMenuItem.Checked)
                {
                    tagOpProto.Add(TagProtocol.IPX64);
                }
                if (iPX256KHzToolStripMenuItem.Checked)
                {
                    tagOpProto.Add(TagProtocol.IPX256);
                }
                if ((tagOpProto.ToArray().Length == 0) || (tagOpProto.ToArray().Length > 1))
                {
                    throw new TagOpProtocolException("Please select a single protocol for this operation");
                }
                else
                {
                    reader.ParamSet("/reader/tagop/protocol", tagOpProto.ToArray()[0]);
                }
            }
        }

        /// <summary>
        /// Writes Tag ID Single
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void writeTagButton_Click(object sender, EventArgs e)
        {
            try
            {
                tagOpProtocolAntenna(sender, e);
                pvtWriteTagEPC(sender, e);
            }
            catch (TagOpAntennaException)
            {
                MessageBox.Show("Please select a single antenna port for this operation under Option", "Error", MessageBoxButtons.OK);
            }
            catch (TagOpProtocolException)
            {
                MessageBox.Show("Please select a single protocol for this operation under Option", "Error", MessageBoxButtons.OK);
            }
        }
        
        /// <summary>
        /// Helper function for Write Tag ID Button
        /// </summary>
        /// <param name="sender">same as writeTagButton_Click</param>
        /// <param name="e">same as writeTagButton_Click</param>
        private void pvtWriteTagEPC(object sender, EventArgs e)
        {
            try
            {
                TagProtocol tagopproto = (TagProtocol)(reader.ParamGet("/reader/tagop/protocol"));
                switch (tagopproto)
                {
                    case TagProtocol.GEN2:
                        {
                            this.allAvailableProtocolLabel.BackColor = System.Drawing.SystemColors.Control;
                            this.gen2Label.BackColor = System.Drawing.Color.GreenYellow;
                            this.isoLabel.BackColor = System.Drawing.SystemColors.Control;
                            this.ipx64Label.BackColor = System.Drawing.SystemColors.Control;
                            this.ipx256Label.BackColor = System.Drawing.SystemColors.Control;
                            TagData tagID = validateEpcStringAndReturnTagData(writeTag.Text);
                            DateTime timeBeforeRead = DateTime.Now;
                            reader.WriteTag(null, tagID);
                            DateTime timeAfterRead = DateTime.Now;
                            TimeSpan timeElapsed = timeAfterRead - timeBeforeRead;
                            commandTotalTimeTextBox.Text = timeElapsed.TotalSeconds.ToString();
                            MessageBox.Show("Wrote EPC: " + writeTag.Text, "Write Success!", MessageBoxButtons.OK);
                            break;
                        }
                    default:
                        {
                            MessageBox.Show("Write Tag ID for " + tagopproto + " is not supported", "Error", MessageBoxButtons.OK);
                            break;
                        }
                }
            }
            catch (Exception exp)
            {
                if (exp is ReaderCodeException)
                {
                    switch (MessageBox.Show("Write Tag ID Failed. Try Again", "Error", MessageBoxButtons.RetryCancel))
                    {
                        case DialogResult.Retry:
                            {
                                writeTagButton_Click(this, e);
                                break;
                            }
                        default:
                            break;
                    }
                }
                if (exp is ReaderCommException)
                {
                    handleReaderCommException((ReaderCommException)exp, e);
                }

            }
        }
        private void label2_Click(object sender, EventArgs e)
        {

        }

        private void readerAddress_combo_SelectedIndexChanged(object sender, EventArgs e)
        {

        }


        private void regioncombo_SelectedIndexChanged(object sender, EventArgs e)
        {

        }
        
        /// <summary>
        /// Saves Tags Read
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>      
        private void saveToolStripMenuItem_Click(object sender, EventArgs e)
        {
            try
            {
                saveFileDialog1.ShowDialog();
                TextWriter textFile = new StreamWriter(saveFileDialog1.FileName);
                textFile.Write("TimeStamp\tReadCount\tAntenna\tProtocol\t\tRSSI\tEPC\r\n" + readTagID.Text);
                textFile.Close();
            }
            catch { }
        }

        /// <summary>
        /// Enables Copy Paste
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void copyCtrlCToolStripMenuItem_Click(object sender, EventArgs e)
        {
            try
            {
                Clipboard.Clear();
                string selectedText = "";
                if (readTagID.SelectedText != "")
                {
                    selectedText = readTagID.SelectedText;
                    readTagID.SelectionLength = 0;
                }
                if (readerAddr.SelectedText != "")
                {
                    selectedText = readerAddr.SelectedText;
                    readerAddr.SelectionLength = 0;
                }
                if (regioncombo.SelectedText != "")
                {
                    selectedText = regioncombo.SelectedText;
                    regioncombo.SelectionLength = 0;
                }                
                if (timeout.SelectedText != "")
                {
                    selectedText = timeout.SelectedText;
                    timeout.SelectionLength = 0;
                }
                if (fontSize.SelectedText != "")
                {
                    selectedText = fontSize.SelectedText;
                    fontSize.SelectionLength = 0;
                }
                if (tagIDtoFind.SelectedText != "")
                {
                    selectedText = tagIDtoFind.SelectedText;
                    tagIDtoFind.SelectionLength = 0;
                }
                if (writeTag.SelectedText != "")
                {
                    selectedText = writeTag.SelectedText;
                    writeTag.SelectionLength = 0;
                }
                if (writeData.SelectedText != "")
                {
                    selectedText = writeData.SelectedText;
                    writeData.SelectionLength = 0;
                }
                Clipboard.SetText(selectedText);
            }
            catch { }
        }


        private void helpToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }

        private void aboutToolStripMenuItem_Click(object sender, EventArgs e)
        {
        }

        /// <summary>
        /// Exits Program
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void exitToolStripMenuItem1_Click(object sender, EventArgs e)
        {
            try
            {
                reader.ParamSet("/reader/baudRate", 115200);
                reader.Destroy();
            }
            catch
            {
            }
            Close();
        }

        
        private void Form1_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            try
            {
                reader.ParamSet("/reader/baudRate", 115200);
                reader.Destroy();
            }
            catch
            {
            }
           
        }


        private void reservedToolStripMenuItem_Click(object sender, EventArgs e)
        {
            userToolStripMenuItem.Checked = false;
            ePCToolStripMenuItem.Checked = false;
            tIDToolStripMenuItem.Checked = false;
        }

        private void ePCToolStripMenuItem_Click(object sender, EventArgs e)
        {
            reservedToolStripMenuItem.Checked = false;
            tIDToolStripMenuItem.Checked = false;
            userToolStripMenuItem.Checked = false;            
        }

        private void tIDToolStripMenuItem_Click(object sender, EventArgs e)
        {
            reservedToolStripMenuItem.Checked = false;
            userToolStripMenuItem.Checked = false;
            ePCToolStripMenuItem.Checked = false;            
        }

        private void userToolStripMenuItem_Click(object sender, EventArgs e)
        {
            reservedToolStripMenuItem.Checked = false;
            tIDToolStripMenuItem.Checked = false;
            ePCToolStripMenuItem.Checked = false;      
        }   

        /// <summary>
        /// Flags for specific code usage (Not reader specific)
        /// </summary>       
        bool flagForTagIdToFind = true;
        bool flagForWriteTag = true;
        bool flagForWriteData = true;
        bool flagForDataAddr = true;
        bool flagForByteCount = true;

        
        private void tagIDtoFind_TextChanged(object sender, EventArgs e)
        {
            if (("Type Tag EPC for General Selection here (Hex String - No Spaces)".Contains(tagIDtoFind.Text)) && (flagForTagIdToFind))
            {
                tagIDtoFind.ForeColor = Color.Gray;
                tagIDtoFind.Text = "";
                flagForTagIdToFind = false;
            }
            else
            {
                tagIDtoFind.ForeColor = Color.Black;
            }            
        }       
        
        private void writeTag_TextChanged(object sender, EventArgs e)
        {
            if ("Type Tag EPC for Write Tag ID here (Hex String - No Spaces)".Contains(writeTag.Text) && (flagForWriteTag))
            {
                writeTag.ForeColor = Color.Gray;
                writeTag.Text = "";
                flagForWriteTag = false;
            }
            else
            {
                writeTag.ForeColor = Color.Black;
            }
        }
        
        private void writeData_TextChanged(object sender, EventArgs e)
        {
            if ("Type Tag Data for Write Data here (Hex String - No Spaces)".Contains(writeData.Text) && (flagForWriteData))
            {
                writeData.ForeColor = Color.Gray;
                writeData.Text = "";
                flagForWriteData = false;
            }
            else
            {
                writeData.ForeColor = Color.Black;
            }
        }
        
        /// <summary>
        /// Write Tag Data
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void writeTagData_Click(object sender, EventArgs e)
        {
            try
            {
                tagOpProtocolAntenna(sender, e);
                pvtWriteTagData(sender, e);
            }
            catch (TagOpAntennaException)
            {
                MessageBox.Show("Please select a single antenna port for this operation under Option", "Error", MessageBoxButtons.OK);
            }
            catch (TagOpProtocolException)
            {
                MessageBox.Show("Please select a single protocol for this operation under Option", "Error", MessageBoxButtons.OK);
            }            
        }
        
        /// <summary>
        /// Helper function for Write Tag Data Button
        /// </summary>
        /// <param name="sender">same as writeTagData_Click</param>
        /// <param name="e">same as writeTagData_Click</param>
        private void pvtWriteTagData(object sender, EventArgs e)
        {
            try
            {
                selectionOnEPC = validateEpcStringAndReturnTagData(tagIDtoFind.Text);
                int addressToWrite = int.Parse(addrReadData.Text);
                byte[] writeDataBytes = ByteFormat.FromHex(writeData.Text);
                switch ((TagProtocol)(reader.ParamGet("/reader/tagop/protocol")))
                {
                    case TagProtocol.GEN2:
                        {
                            Gen2.Bank bankTOWrite = Gen2.Bank.RESERVED;                            
                            if (reservedToolStripMenuItem.Checked)
                            {
                                bankTOWrite = Gen2.Bank.RESERVED;
                            }
                            if (ePCToolStripMenuItem.Checked)
                            {
                                bankTOWrite = Gen2.Bank.EPC;
                            }
                            if (tIDToolStripMenuItem.Checked)
                            {
                                bankTOWrite = Gen2.Bank.TID;
                            }
                            if (userToolStripMenuItem.Checked)
                            {
                                bankTOWrite = Gen2.Bank.USER;
                            }
                            DateTime timeBeforeRead = DateTime.Now;
                            reader.WriteTagMemBytes(selectionOnEPC, (int)bankTOWrite, addressToWrite, writeDataBytes);
                            DateTime timeAfterRead = DateTime.Now;
                            TimeSpan timeElapsed = timeAfterRead - timeBeforeRead;
                            commandTotalTimeTextBox.Text = timeElapsed.TotalSeconds.ToString();
                            MessageBox.Show("Wrote Data: " + writeData.Text, "Write Success!", MessageBoxButtons.OK);
                            break;
                        }
                    case TagProtocol.ISO180006B:
                        {
                            DateTime timeBeforeRead = DateTime.Now;
                            reader.WriteTagMemBytes(selectionOnEPC, 0, addressToWrite, writeDataBytes);
                            DateTime timeAfterRead = DateTime.Now;
                            TimeSpan timeElapsed = timeAfterRead - timeBeforeRead;
                            commandTotalTimeTextBox.Text = timeElapsed.TotalSeconds.ToString();
                            MessageBox.Show("Wrote Data: " + writeData.Text, "Write Success!", MessageBoxButtons.OK);
                            break;
                        }
                }
               
            }
            catch (Exception exp)
            {
                if (exp is ReaderCodeException)
                {
                    switch (MessageBox.Show(exp.Message.ToString() + "\nWrite Tag Data Failed. Try Again", "Error", MessageBoxButtons.RetryCancel))
                    {
                        case DialogResult.Retry:
                            {
                                writeTagData_Click(this, e);
                                break;
                            }
                        default:
                            break;
                    }
                }
                if (exp is ReaderCommException)
                {
                    handleReaderCommException((ReaderCommException)exp, e);
                }

            }
        }

        /// <summary>
        /// Helper function for Tag Read Data
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>        
        private void pvtReadTagData(object sender, EventArgs e)
        {
            try
            {
                int addressToRead = int.Parse(addrReadData.Text);
                int byteCount = int.Parse(byteCountTextBox.Text);
                selectionOnEPC = validateEpcStringAndReturnTagData(tagIDtoFind.Text);
                switch ((TagProtocol)(reader.ParamGet("/reader/tagop/protocol")))
                {
                    case TagProtocol.GEN2:         
                        {
                            this.allAvailableProtocolLabel.BackColor = System.Drawing.SystemColors.Control;
                            this.gen2Label.BackColor = System.Drawing.Color.GreenYellow;
                            this.isoLabel.BackColor = System.Drawing.SystemColors.Control;
                            this.ipx64Label.BackColor = System.Drawing.SystemColors.Control;
                            this.ipx256Label.BackColor = System.Drawing.SystemColors.Control;
                            Gen2.Bank bankTORead = Gen2.Bank.RESERVED;
                            
                            if (reservedToolStripMenuItem.Checked)
                            {
                                bankTORead = Gen2.Bank.RESERVED;
                            }
                            if (ePCToolStripMenuItem.Checked)
                            {
                                bankTORead = Gen2.Bank.EPC;
                            }
                            if (tIDToolStripMenuItem.Checked)
                            {
                                bankTORead = Gen2.Bank.TID;
                            }
                            if (userToolStripMenuItem.Checked)
                            {
                                bankTORead = Gen2.Bank.USER;
                            }
                            DateTime timeBeforeRead = DateTime.Now;
                            readData.Text = ByteFormat.ToHex(reader.ReadTagMemBytes(selectionOnEPC, (int)bankTORead, addressToRead, byteCount));
                            DateTime timeAfterRead = DateTime.Now;
                            TimeSpan timeElapsed = timeAfterRead - timeBeforeRead;
                            commandTotalTimeTextBox.Text = timeElapsed.TotalSeconds.ToString();
                            break;
                        }
                    case TagProtocol.ISO180006B:
                        {
                            this.allAvailableProtocolLabel.BackColor = System.Drawing.SystemColors.Control;
                            this.gen2Label.BackColor = System.Drawing.SystemColors.Control;
                            this.isoLabel.BackColor = System.Drawing.Color.GreenYellow;
                            this.ipx64Label.BackColor = System.Drawing.SystemColors.Control;
                            this.ipx256Label.BackColor = System.Drawing.SystemColors.Control;
                            DateTime timeBeforeRead = DateTime.Now;
                            readData.Text = ByteFormat.ToHex(reader.ReadTagMemBytes(selectionOnEPC, 0, addressToRead, byteCount));
                            DateTime timeAfterRead = DateTime.Now;
                            TimeSpan timeElapsed = timeAfterRead - timeBeforeRead;
                            commandTotalTimeTextBox.Text = timeElapsed.TotalSeconds.ToString();
                            break;
                        }                        
                }
            }
            catch (Exception exp)
            {
                if (exp is ReaderCodeException)
                {
                    switch (MessageBox.Show(exp.Message.ToString() + "\nRead Data Failed. Try Again", "Error", MessageBoxButtons.RetryCancel))
                    {
                        case DialogResult.Retry:
                            {
                                readTagData_Click(this, e);
                                break;
                            }
                        default:
                            break;
                    }
                    readData.Text = "";
                }
                if (exp is ReaderCommException)
                {
                    handleReaderCommException((ReaderCommException)exp, e);
                }

            }
        }
       
        private void selectionStatus_Click(object sender, EventArgs e)
        {
        }

        /// <summary>
        /// Disconnects from ThingMagic Reader
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void disconnectReaderMenuItem_Click(object sender, EventArgs e)
        {
            try
            {
                if (startRead.Text == "Stop Reads")
                {
                    startRead_Click(sender, e);
                }
                initReader.Enabled = true;
                baudRateToolStripMenuItem.Enabled = false;
                thingMagicReaderToolStripMenuItem.Enabled = false;
                rFPowerLevelToolStripMenuItem.Enabled = false;
                startRead.Enabled = false;
                readTag.Enabled = false;
                writeTagButton.Enabled = false;
                readTagData.Enabled = false;
                writeTagData.Enabled = false;
                this.initReader.BackColor = System.Drawing.SystemColors.ButtonFace;
                reader.ParamSet("/reader/baudRate", 115200);
                reader.Destroy();
            }
            catch { }
        }

        private void readerAddr_TextChanged(object sender, EventArgs e)
        {
            readerAddr.ForeColor = Color.Black;
        }

        private void menuStrip1_ItemClicked(object sender, ToolStripItemClickedEventArgs e)
        {

        }

        /// <summary>
        /// Shows dialog box when Reader Comm exception is found
        /// </summary>
        /// <param name="exp">Reader Comm Exception</param>
        /// <param name="e"></param>
        private void handleReaderCommException(ReaderCommException exp, EventArgs e)
        {
            switch (MessageBox.Show(exp.Message.ToString() + "\nDo you want to reset the reader now?", "Error", MessageBoxButtons.YesNoCancel))
            {
                case DialogResult.Yes:
                    {
                        disconnectReaderMenuItem_Click(this, e);
                        initReader_Click(this, e);
                        break;
                    }
                case DialogResult.No:
                    {
                        break;
                    }
                case DialogResult.Cancel:
                    {
                        exitToolStripMenuItem1_Click(this, e);
                        break;
                    }
                default:
                    {
                        break;
                    }
            }
        }

        private void pasteCtrlVToolStripMenuItem_Click(object sender, EventArgs e)
        {
            this.ActiveControl.Text = Clipboard.GetText();
        }

        private void fakeExit_Click(object sender, EventArgs e)
        {
            exitToolStripMenuItem1_Click(this, e);
        }
        
        

        private void gEN2ToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllAvailableProtocolsToolStripMenuItem.Checked = false;
        }

        private void iSO180006BToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllAvailableProtocolsToolStripMenuItem.Checked = false;
        }

        private void iPX64KHzToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllAvailableProtocolsToolStripMenuItem.Checked = false;
        }

        private void iPX256KHzToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllAvailableProtocolsToolStripMenuItem.Checked = false;
        }

        private void readOnAllAvailableProtocolsToolStripMenuItem_Click(object sender, EventArgs e)
        {
            gEN2ToolStripMenuItem.Checked = false;
            iSO180006BToolStripMenuItem.Checked = false;
            iPX64KHzToolStripMenuItem.Checked = false;
            iPX256KHzToolStripMenuItem.Checked = false;

        }

        private void antenna1ToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllConnectedAntennasToolStripMenuItem.Checked = false;
        }

        private void antenna2ToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllConnectedAntennasToolStripMenuItem.Checked = false;
        }

        private void antenna3ToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllConnectedAntennasToolStripMenuItem.Checked = false;
        }

        private void antenna4ToolStripMenuItem_Click(object sender, EventArgs e)
        {
            readOnAllConnectedAntennasToolStripMenuItem.Checked = false;
        }

        private void readOnAllConnectedAntennasToolStripMenuItem_Click(object sender, EventArgs e)
        {
            antenna1ToolStripMenuItem.Checked = false;
            antenna2ToolStripMenuItem.Checked = false;
            antenna3ToolStripMenuItem.Checked = false;
            antenna4ToolStripMenuItem.Checked = false;
        }

        private void readOnAntennaNumberToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }

        private void protocolToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }
        
        private void addrReadData_TextChanged(object sender, EventArgs e)
        {            
                if (("Type Byte Address of Memory to Read and Write here").Contains(addrReadData.Text) && (flagForDataAddr))
                {
                    addrReadData.ForeColor = Color.Gray;
                    addrReadData.Text = "";
                    flagForDataAddr = false;
                }
                else
                {
                    addrReadData.ForeColor = Color.Black;
                }
            
        }

        /// <summary>
        /// Reads Tag Data
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void readTagData_Click(object sender, EventArgs e)
        {
            try
            {
                tagOpProtocolAntenna(sender, e);
                pvtReadTagData(sender, e);
            }
            catch (TagOpAntennaException)
            {
                MessageBox.Show("Please select a single antenna port for this operation under Option", "Error", MessageBoxButtons.OK);
            }
            catch (TagOpProtocolException)
            {
                MessageBox.Show("Please select a single protocol for this operation under Option", "Error", MessageBoxButtons.OK);
            }
        }

        private void readData_TextChanged(object sender, EventArgs e)
        {

        }

        private void byteCountTextBox_TextChanged(object sender, EventArgs e)
        {
            if (("Type Number of Bytes to read for Read Data here").Contains(byteCountTextBox.Text) && (flagForByteCount))
            {
                byteCountTextBox.ForeColor = Color.Gray;
                byteCountTextBox.Text = "";
                flagForByteCount = false;
            }
            else
            {
                byteCountTextBox.ForeColor = Color.Black;
            }
        }

        private void demoApplicationToolStripMenuItem_Click(object sender, EventArgs e)
        {
            MessageBox.Show("M6e Read/Write Demo Tool\nMercury API Version 1.2.1.7", "About Demo Application...", MessageBoxButtons.OK);
        }

        private void thingMagicReaderToolStripMenuItem_Click(object sender, EventArgs e)
        {
            try
            {
                string software = (string)reader.ParamGet("/reader/version/software");
                MessageBox.Show(string.Concat("Hardware: M6e", "  ", "Software: ", software), "About ThingMagic Reader...", MessageBoxButtons.OK);
            }
            catch
            {
                MessageBox.Show("Connection to ThingMagic Reader not established", "Error!", MessageBoxButtons.OK);
            }
        }

        private void gen2Label_Click(object sender, EventArgs e)
        {

        }

        private void readTagID_TextChanged(object sender, EventArgs e)
        {

        }

        

        private void totalTagsReadTextBox_TextChanged(object sender, EventArgs e)
        {

        }

        private void totalTimeElapsedTextBox_TextChanged(object sender, EventArgs e)
        {

        }

        private void totalTagsReadLabel_Click(object sender, EventArgs e)
        {

        }

        private void inLabel_Click(object sender, EventArgs e)
        {

        }

        private void secondsLabel_Click(object sender, EventArgs e)
        {

        }

        private void baud9600MenuItem_Click(object sender, EventArgs e)
        {
            baud19200MenuItem.Checked = false;
            baud38400MenuItem.Checked = false;
            baud115200MenuItem.Checked = false;
            baud230400MenuItem.Checked = false;
            baud460800MenuItem.Checked = false;
            baud921600MenuItem.Checked = false;
            reader.ParamSet("/reader/baudRate", 9600);
        }

        private void baud19200MenuItem_Click(object sender, EventArgs e)
        {
            baud9600MenuItem.Checked = false;          
            baud38400MenuItem.Checked = false;
            baud115200MenuItem.Checked = false;
            baud230400MenuItem.Checked = false;
            baud460800MenuItem.Checked = false;
            baud921600MenuItem.Checked = false;
            reader.ParamSet("/reader/baudRate", 19200);
        }

        private void baud38400MenuItem_Click(object sender, EventArgs e)
        {
            baud19200MenuItem.Checked = false;
            baud9600MenuItem.Checked = false;     
            baud115200MenuItem.Checked = false;
            baud230400MenuItem.Checked = false;
            baud460800MenuItem.Checked = false;
            baud921600MenuItem.Checked = false;
            reader.ParamSet("/reader/baudRate", 38400);
        }

        private void baud115200MenuItem_Click(object sender, EventArgs e)
        {
            baud19200MenuItem.Checked = false;
            baud38400MenuItem.Checked = false;
            baud9600MenuItem.Checked = false;     
            baud230400MenuItem.Checked = false;
            baud460800MenuItem.Checked = false;
            baud921600MenuItem.Checked = false;
            reader.ParamSet("/reader/baudRate", 115200);
        }

        private void baud230400MenuItem_Click(object sender, EventArgs e)
        {
            baud19200MenuItem.Checked = false;
            baud38400MenuItem.Checked = false;
            baud115200MenuItem.Checked = false;
            baud9600MenuItem.Checked = false;     
            baud460800MenuItem.Checked = false;
            baud921600MenuItem.Checked = false;
            reader.ParamSet("/reader/baudRate", 230400);
        }

        private void baud460800MenuItem_Click(object sender, EventArgs e)
        {
            baud19200MenuItem.Checked = false;
            baud38400MenuItem.Checked = false;
            baud115200MenuItem.Checked = false;
            baud230400MenuItem.Checked = false;
            baud9600MenuItem.Checked = false;     
            baud921600MenuItem.Checked = false;
            reader.ParamSet("/reader/baudRate", 460800);
        }

        private void baud921600MenuItem_Click(object sender, EventArgs e)
        {
            baud19200MenuItem.Checked = false;
            baud38400MenuItem.Checked = false;
            baud115200MenuItem.Checked = false;
            baud230400MenuItem.Checked = false;
            baud460800MenuItem.Checked = false;
            baud9600MenuItem.Checked = false;
            reader.ParamSet("/reader/baudRate", 921600);

        }
       //ST
       // VEDI QUI'
        private void clearAllPowerMenuItems()    
        {
            power15dBmMenuItem.Checked = false; //quì si resetta il menù delle potenze ponendo tutte le possibili scelte a zero
            power16dBmMenuItem.Checked = false; //io ho aggiunto la stessa stringa per le potenze agggiunte che sono 16,17,18,19,21,22,23,24,26,27,28,29
            power17dBmMenuItem.Checked = false;
            power18dBmMenuItem.Checked = false;
            power19dBmMenuItem.Checked = false;
            power20dBmMenuItem.Checked = false;
            power21dBmMenuItem.Checked = false;
            power22dBmMenuItem.Checked = false;
            power23dBmMenuItem.Checked = false;
            power24dBmMenuItem.Checked = false;
            power25dBmMenuItem.Checked = false;
            power26dBmMenuItem.Checked = false;
            power27dBmMenuItem.Checked = false;
            power28dBmMenuItem.Checked = false;
            power29dBmMenuItem.Checked = false;
            power30dBmMenuItem.Checked = false;
            power31dBmMenuItem.Checked = false;
            power31p5dBmMenuItem.Checked = false;
        }

        private void power15dBmMenuItem_Click(object sender, EventArgs e) //metodo power15dBmMenuItem_Click richiamato quando si seleziona 15 dBm
        {
            clearAllPowerMenuItems(); //richiamo il metodo o funzione clearAllPowerMenuItems() con il quale deseleziono tutte le potenze
            power15dBmMenuItem.Checked = true; //se è selezionata la potenza 15dBm
            reader.ParamSet("/reader/radio/readPower",1500); //richiamo il campo ParamSet del metodo reader con il quale grazie ai due argomenti tra parantesi setto il reader a 15dBm
        }

        private void power16dBmMenuItem_Click(object sender, EventArgs e)// pezzo aggiunto da me insieme a quello relativo a 17,18,19,21,22,23,24,26,27,28,29 dBm
        {
            clearAllPowerMenuItems();
            power16dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",1600);
        }

        private void power17dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power17dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",1700);
        }

        private void power18dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power18dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",1800);
        }

        private void power19dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power19dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",1900);
        }
        
        private void power20dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power20dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2000);
        }

        private void power21dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power21dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2100);
        }
        
        private void power22dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power22dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2200);
        }

        private void power23dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power23dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2300);
        }

        private void power24dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power24dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2400);
        }

        private void power25dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power25dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2500);
        }

        private void power26dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power26dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2600);
        }

        private void power27dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power27dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2700);
        }

        private void power28dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power28dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2800);
        }

        private void power29dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power29dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",2900);
        }

        private void power30dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power30dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower",3000);
        }

        private void power31dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power31dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower", 3100);
        }

        private void power31p5dBmMenuItem_Click(object sender, EventArgs e)
        {
            clearAllPowerMenuItems();
            power31p5dBmMenuItem.Checked = true;
            reader.ParamSet("/reader/radio/readPower", 3150);
        }

        private void rFPowerLevelToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }

        private void bankToReadToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }

        private void pictureBox1_Click(object sender, EventArgs e)
        {
            ToolTip forWebsite = new ToolTip();
            //forWebsite.SetToolTip(pictureBox1, "www.thingmagic.com");
            System.Diagnostics.Process.Start("http://www.thingmagic.com/mercuryapi");                 
        }

        private void demoToolAssistantToolStripMenuItem_Click(object sender, EventArgs e)
        {
            try
            {
                System.Reflection.Assembly execAssembly = System.Reflection.Assembly.GetExecutingAssembly();
                string path = Path.GetDirectoryName(execAssembly.Location);
                System.Diagnostics.Process.Start(path + "\\Readme.txt");
            }
            catch (System.ComponentModel.Win32Exception)
            {
                MessageBox.Show("Help file missing! Please re-compile the source code", "Error!", MessageBoxButtons.OK);

            }

        }

        private void label19_Click(object sender, EventArgs e)
        {

        }

        private void label8_Click(object sender, EventArgs e)
        {

        }

        private void toolStripTextBox1_Click(object sender, EventArgs e)
        {
             
        }

        private void dBmToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }

        private void label6_Click(object sender, EventArgs e)
        {

        }

        private void label5_Click(object sender, EventArgs e)
        {

        }

        private void label21_Click(object sender, EventArgs e)
        {

        }
       //ST
        public void buttonSalvaDati_Click(object sender, EventArgs e) //se clicco sul pulzante Salva Dati si richiama questo metodo
        {
            saveFileDialogFileTesto.ShowDialog(); //il metodo buttonSalavaDati richiama questo il metodo saveFileDialogFileTesto
           // string testodasalvare = saveFileDialogFileTesto.FileName;
           // StreamWriter sw = new StreamWriter(testodasalvare,true);
            
            //sw.WriteLine(testodasalvare);
            //sw.WriteLine(readTagID.Text);
            //sw.Flush();
            //sw.Close();
        }

       

        private void toolStripMenuItem1_Click(object sender, EventArgs e)
        {

        }


        private void fileToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }

        private void editToolStripMenuItem_Click(object sender, EventArgs e)
        {

        }

        private void isoLabel_Click(object sender, EventArgs e)
        {

        }
       
        private void frequenza_TextChanged(object sender, EventArgs e)
        {
            //ST
            try
            {
                if (frequenza.Text != "")
                    reader.ParamSet("/reader/region/hopTable", int.Parse(frequenza.Text) * 1000);
            }
            catch { }
        }

        private void labelFrequenza_Click(object sender, EventArgs e)
        {

        }
       //ST
        private void frequencysetting_TextChanged(object sender, EventArgs e)
        {
            try
            {
                if (frequenza.Text != "")
                    frequencysetting();
            }
            catch { }
        }
       
        
        
        
        
       //ST 
        private void frequencysetting()//funzione per il settaggio della frequenza
        {
            UInt32[] aaa; //si definisce una variabile aaa che è un array di interi 
            aaa = new UInt32[13]; //associamo ad aaa un array di interi di 13 elementi 
            aaa = (UInt32[])(reader.ParamGet("/reader/region/hopTable"));//.Parse(frequenza.Text) * 1000); //Settaggio frequenza in Mhz
                                                                         
            UInt32 freq = (UInt32)(int.Parse(frequenza.Text) * 1000); //definiamo una variabile UInt32 di nome freq e la inizializziamo passandogli il valore contenuto nel campo Text del textbox chiamato frequenza, dopo averlo convertito in UInt32
            if (freq >= 840000 & freq <= 960000)
            {

                int h;
                for (h = 0; h < 13; h++) aaa[h] = freq; // a tutti gli elementi del vettore aaa gli associo lo stesso valore che è quello di freq e quindi del valore che ho scritto nel textbox
            }
            else MessageBox.Show("Settaggio frequenza errato [840-960]MHz", "Error", MessageBoxButtons.OK);
            reader.ParamSet("/reader/region/hopTable", aaa); // alla funzione ParamSet gli associo l'argomento aaa quindi in questo modo rendo effettivo il settaggio del reader e lo faccio lavorare ad un'unica frequenza che è quella stabilita da me
            
         }
    }
}
