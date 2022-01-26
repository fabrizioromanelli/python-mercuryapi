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

namespace Demo_readWrite
{
    partial class readWriteDemo
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(readWriteDemo));
            this.tagIDtoFind = new System.Windows.Forms.TextBox();
            this.initReader = new System.Windows.Forms.Button();
            this.readTag = new System.Windows.Forms.Button();
            this.readTagID = new System.Windows.Forms.TextBox();
            this.timeout = new System.Windows.Forms.TextBox();
            this.label1 = new System.Windows.Forms.Label();
            this.label2 = new System.Windows.Forms.Label();
            this.label4 = new System.Windows.Forms.Label();
            this.fontSize = new System.Windows.Forms.TextBox();
            this.startRead = new System.Windows.Forms.Button();
            this.stopReads = new System.Windows.Forms.Button();
            this.label3 = new System.Windows.Forms.Label();
            this.label6 = new System.Windows.Forms.Label();
            this.label7 = new System.Windows.Forms.Label();
            this.label8 = new System.Windows.Forms.Label();
            this.writeTag = new System.Windows.Forms.TextBox();
            this.writeTagButton = new System.Windows.Forms.Button();
            this.saveFileDialog1 = new System.Windows.Forms.SaveFileDialog();
            this.regioncombo = new System.Windows.Forms.ComboBox();
            this.label9 = new System.Windows.Forms.Label();
            this.menuStrip1 = new System.Windows.Forms.MenuStrip();
            this.fileToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.saveToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.disconnectReaderMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.toolStripMenuItem3 = new System.Windows.Forms.ToolStripSeparator();
            this.exitToolStripMenuItem1 = new System.Windows.Forms.ToolStripMenuItem();
            this.editToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.copyCtrlCToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.pasteCtrlVToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.toolStripMenuItem1 = new System.Windows.Forms.ToolStripMenuItem();
            this.rFPowerLevelToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power15dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power16dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power17dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power18dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power19dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power20dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power21dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power22dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power23dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power24dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power25dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power26dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power27dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power28dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power29dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power30dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power31dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.power31p5dBmMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.toolStripTextBox1 = new System.Windows.Forms.ToolStripTextBox();
            this.protocolToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.gEN2ToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.iSO180006BToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.iPX64KHzToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.iPX256KHzToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.readOnAllAvailableProtocolsToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.readOnAntennaNumberToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.antenna1ToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.antenna2ToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.antenna3ToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.antenna4ToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.readOnAllConnectedAntennasToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.bankToReadToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.reservedToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.ePCToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.tIDToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.userToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baudRateToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baud9600MenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baud19200MenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baud38400MenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baud115200MenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baud230400MenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baud460800MenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.baud921600MenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.helpToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.aboutToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.demoApplicationToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.thingMagicReaderToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.demoToolAssistantToolStripMenuItem = new System.Windows.Forms.ToolStripMenuItem();
            this.writeData = new System.Windows.Forms.TextBox();
            this.writeTagData = new System.Windows.Forms.Button();
            this.label11 = new System.Windows.Forms.Label();
            this.label12 = new System.Windows.Forms.Label();
            this.label13 = new System.Windows.Forms.Label();
            this.readerAddr = new System.Windows.Forms.TextBox();
            this.fakeExit = new System.Windows.Forms.Button();
            this.label5 = new System.Windows.Forms.Label();
            this.addrReadData = new System.Windows.Forms.TextBox();
            this.readTagData = new System.Windows.Forms.Button();
            this.label14 = new System.Windows.Forms.Label();
            this.label15 = new System.Windows.Forms.Label();
            this.readData = new System.Windows.Forms.TextBox();
            this.byteCountTextBox = new System.Windows.Forms.TextBox();
            this.label16 = new System.Windows.Forms.Label();
            this.label17 = new System.Windows.Forms.Label();
            this.label10 = new System.Windows.Forms.Label();
            this.gen2Label = new System.Windows.Forms.Label();
            this.isoLabel = new System.Windows.Forms.Label();
            this.ipx64Label = new System.Windows.Forms.Label();
            this.ipx256Label = new System.Windows.Forms.Label();
            this.allAvailableProtocolLabel = new System.Windows.Forms.Label();
            this.label18 = new System.Windows.Forms.Label();
            this.totalTimeElapsedTextBox = new System.Windows.Forms.TextBox();
            this.inLabel = new System.Windows.Forms.Label();
            this.totalTagsReadTextBox = new System.Windows.Forms.TextBox();
            this.totalTagsReadLabel = new System.Windows.Forms.Label();
            this.commandTimeLabel = new System.Windows.Forms.Label();
            this.commandTotalTimeTextBox = new System.Windows.Forms.TextBox();
            this.secondsLabel = new System.Windows.Forms.Label();
            this.commandSecLabel = new System.Windows.Forms.Label();
            this.label19 = new System.Windows.Forms.Label();
            this.WarningLabel = new System.Windows.Forms.Label();
            this.label20 = new System.Windows.Forms.Label();
            this.label21 = new System.Windows.Forms.Label();
            this.buttonSalvaDati = new System.Windows.Forms.Button();
            this.saveFileDialogFileTesto = new System.Windows.Forms.SaveFileDialog();
            this.labelFrequenza = new System.Windows.Forms.Label();
            this.frequenza = new System.Windows.Forms.TextBox();
            this.label22 = new System.Windows.Forms.Label();
            this.textBox1 = new System.Windows.Forms.TextBox();
            this.textBox2 = new System.Windows.Forms.TextBox();
            this.label23 = new System.Windows.Forms.Label();
            this.textBox3 = new System.Windows.Forms.TextBox();
            this.label24 = new System.Windows.Forms.Label();
            this.label25 = new System.Windows.Forms.Label();
            this.turnOnSearch = new System.Windows.Forms.Button();
            this.menuStrip1.SuspendLayout();
            this.SuspendLayout();
            // 
            // tagIDtoFind
            // 
            this.tagIDtoFind.AllowDrop = true;
            this.tagIDtoFind.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.tagIDtoFind.ForeColor = System.Drawing.SystemColors.InactiveCaption;
            this.tagIDtoFind.Location = new System.Drawing.Point(85, 574);
            this.tagIDtoFind.Name = "tagIDtoFind";
            this.tagIDtoFind.Size = new System.Drawing.Size(652, 20);
            this.tagIDtoFind.TabIndex = 12;
            this.tagIDtoFind.Text = "Type Tag EPC for General Selection here (Hex String - No Spaces)";
            this.tagIDtoFind.TextChanged += new System.EventHandler(this.tagIDtoFind_TextChanged);
            // 
            // initReader
            // 
            this.initReader.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.initReader.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.initReader.Location = new System.Drawing.Point(456, 37);
            this.initReader.Name = "initReader";
            this.initReader.Size = new System.Drawing.Size(198, 23);
            this.initReader.TabIndex = 3;
            this.initReader.Text = "Initialize Reader";
            this.initReader.UseVisualStyleBackColor = false;
            this.initReader.Click += new System.EventHandler(this.initReader_Click);
            // 
            // readTag
            // 
            this.readTag.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.readTag.Enabled = false;
            this.readTag.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.readTag.Location = new System.Drawing.Point(242, 77);
            this.readTag.Name = "readTag";
            this.readTag.Size = new System.Drawing.Size(152, 25);
            this.readTag.TabIndex = 6;
            this.readTag.Text = "Read Once";
            this.readTag.UseVisualStyleBackColor = false;
            this.readTag.Click += new System.EventHandler(this.readTag_Click);
            // 
            // readTagID
            // 
            this.readTagID.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom)
                        | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.readTagID.BackColor = System.Drawing.SystemColors.Control;
            this.readTagID.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.readTagID.Location = new System.Drawing.Point(12, 198);
            this.readTagID.Multiline = true;
            this.readTagID.Name = "readTagID";
            this.readTagID.ReadOnly = true;
            this.readTagID.ScrollBars = System.Windows.Forms.ScrollBars.Vertical;
            this.readTagID.Size = new System.Drawing.Size(913, 331);
            this.readTagID.TabIndex = 11;
            this.readTagID.TextChanged += new System.EventHandler(this.readTagID_TextChanged);
            // 
            // timeout
            // 
            this.timeout.Location = new System.Drawing.Point(93, 79);
            this.timeout.Name = "timeout";
            this.timeout.Size = new System.Drawing.Size(39, 20);
            this.timeout.TabIndex = 4;
            this.timeout.Text = "100";
            this.timeout.TextChanged += new System.EventHandler(this.timeout_TextChanged);
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label1.Location = new System.Drawing.Point(5, 83);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(67, 13);
            this.label1.TabIndex = 9;
            this.label1.Text = "Timeout (ms)";
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label2.Location = new System.Drawing.Point(9, 42);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(97, 13);
            this.label2.TabIndex = 10;
            this.label2.Text = "Reader (COM Port)";
            this.label2.Click += new System.EventHandler(this.label2_Click);
            // 
            // label4
            // 
            this.label4.AutoSize = true;
            this.label4.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label4.Location = new System.Drawing.Point(138, 83);
            this.label4.Name = "label4";
            this.label4.Size = new System.Drawing.Size(51, 13);
            this.label4.TabIndex = 14;
            this.label4.Text = "Font Size";
            this.label4.Click += new System.EventHandler(this.label4_Click);
            // 
            // fontSize
            // 
            this.fontSize.Location = new System.Drawing.Point(190, 79);
            this.fontSize.Name = "fontSize";
            this.fontSize.Size = new System.Drawing.Size(45, 20);
            this.fontSize.TabIndex = 5;
            this.fontSize.Text = "10";
            this.fontSize.TextChanged += new System.EventHandler(this.fontSize_TextChanged_1);
            // 
            // startRead
            // 
            this.startRead.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.startRead.Enabled = false;
            this.startRead.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.startRead.Location = new System.Drawing.Point(408, 77);
            this.startRead.Name = "startRead";
            this.startRead.Size = new System.Drawing.Size(152, 25);
            this.startRead.TabIndex = 7;
            this.startRead.Text = "Start Reads";
            this.startRead.UseVisualStyleBackColor = false;
            this.startRead.Click += new System.EventHandler(this.startRead_Click);
            // 
            // stopReads
            // 
            this.stopReads.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.stopReads.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.stopReads.Location = new System.Drawing.Point(574, 77);
            this.stopReads.Name = "stopReads";
            this.stopReads.Size = new System.Drawing.Size(95, 25);
            this.stopReads.TabIndex = 8;
            this.stopReads.Text = "Clear";
            this.stopReads.UseVisualStyleBackColor = false;
            this.stopReads.Click += new System.EventHandler(this.clear_Click);
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label3.Location = new System.Drawing.Point(25, 182);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(58, 13);
            this.label3.TabIndex = 22;
            this.label3.Text = "Timestamp";
            this.label3.Click += new System.EventHandler(this.label3_Click_1);
            // 
            // label6
            // 
            this.label6.AutoSize = true;
            this.label6.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label6.Location = new System.Drawing.Point(129, 182);
            this.label6.Name = "label6";
            this.label6.Size = new System.Drawing.Size(61, 13);
            this.label6.TabIndex = 23;
            this.label6.Text = "ReadCount";
            this.label6.Click += new System.EventHandler(this.label6_Click);
            // 
            // label7
            // 
            this.label7.AutoSize = true;
            this.label7.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label7.Location = new System.Drawing.Point(196, 182);
            this.label7.Name = "label7";
            this.label7.Size = new System.Drawing.Size(57, 13);
            this.label7.TabIndex = 24;
            this.label7.Text = "Antenna #";
            // 
            // label8
            // 
            this.label8.AutoSize = true;
            this.label8.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label8.Location = new System.Drawing.Point(456, 182);
            this.label8.Name = "label8";
            this.label8.Size = new System.Drawing.Size(43, 13);
            this.label8.TabIndex = 25;
            this.label8.Text = "PHASE";
            this.label8.Click += new System.EventHandler(this.label8_Click);
            // 
            // writeTag
            // 
            this.writeTag.AllowDrop = true;
            this.writeTag.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.writeTag.ForeColor = System.Drawing.SystemColors.InactiveCaption;
            this.writeTag.Location = new System.Drawing.Point(85, 608);
            this.writeTag.Name = "writeTag";
            this.writeTag.Size = new System.Drawing.Size(652, 20);
            this.writeTag.TabIndex = 14;
            this.writeTag.Text = "Type Tag EPC for Write Tag ID here (Hex String - No Spaces)";
            this.writeTag.TextChanged += new System.EventHandler(this.writeTag_TextChanged);
            // 
            // writeTagButton
            // 
            this.writeTagButton.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.writeTagButton.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.writeTagButton.Enabled = false;
            this.writeTagButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.writeTagButton.Location = new System.Drawing.Point(751, 608);
            this.writeTagButton.Name = "writeTagButton";
            this.writeTagButton.Size = new System.Drawing.Size(172, 23);
            this.writeTagButton.TabIndex = 15;
            this.writeTagButton.Text = "Write GEN2 EPC (Single)";
            this.writeTagButton.UseVisualStyleBackColor = false;
            this.writeTagButton.Click += new System.EventHandler(this.writeTagButton_Click);
            // 
            // regioncombo
            // 
            this.regioncombo.FormattingEnabled = true;
            this.regioncombo.Items.AddRange(new object[] {
            "Open (255)",
            "EU3 revised (8)",
            "North America (1)",
            "Korea (3)",
            "P.R. China (6)"});
            this.regioncombo.Location = new System.Drawing.Point(283, 38);
            this.regioncombo.Name = "regioncombo";
            this.regioncombo.Size = new System.Drawing.Size(167, 21);
            this.regioncombo.TabIndex = 2;
            this.regioncombo.SelectedIndexChanged += new System.EventHandler(this.regioncombo_SelectedIndexChanged);
            // 
            // label9
            // 
            this.label9.AutoSize = true;
            this.label9.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label9.Location = new System.Drawing.Point(239, 42);
            this.label9.Name = "label9";
            this.label9.Size = new System.Drawing.Size(41, 13);
            this.label9.TabIndex = 28;
            this.label9.Text = "Region";
            // 
            // menuStrip1
            // 
            this.menuStrip1.BackColor = System.Drawing.SystemColors.ControlLight;
            this.menuStrip1.Items.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.fileToolStripMenuItem,
            this.editToolStripMenuItem,
            this.toolStripMenuItem1,
            this.helpToolStripMenuItem});
            this.menuStrip1.Location = new System.Drawing.Point(0, 0);
            this.menuStrip1.Name = "menuStrip1";
            this.menuStrip1.Size = new System.Drawing.Size(935, 25);
            this.menuStrip1.TabIndex = 30;
            this.menuStrip1.Text = "File";
            this.menuStrip1.ItemClicked += new System.Windows.Forms.ToolStripItemClickedEventHandler(this.menuStrip1_ItemClicked);
            // 
            // fileToolStripMenuItem
            // 
            this.fileToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.saveToolStripMenuItem,
            this.disconnectReaderMenuItem,
            this.toolStripMenuItem3,
            this.exitToolStripMenuItem1});
            this.fileToolStripMenuItem.Name = "fileToolStripMenuItem";
            this.fileToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)((System.Windows.Forms.Keys.Alt | System.Windows.Forms.Keys.F)));
            this.fileToolStripMenuItem.ShowShortcutKeys = false;
            this.fileToolStripMenuItem.Size = new System.Drawing.Size(39, 21);
            this.fileToolStripMenuItem.Text = "File";
            this.fileToolStripMenuItem.Click += new System.EventHandler(this.fileToolStripMenuItem_Click);
            // 
            // saveToolStripMenuItem
            // 
            this.saveToolStripMenuItem.Name = "saveToolStripMenuItem";
            this.saveToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.S)));
            this.saveToolStripMenuItem.Size = new System.Drawing.Size(296, 22);
            this.saveToolStripMenuItem.Text = "Save Data";
            this.saveToolStripMenuItem.Click += new System.EventHandler(this.saveToolStripMenuItem_Click);
            // 
            // disconnectReaderMenuItem
            // 
            this.disconnectReaderMenuItem.Name = "disconnectReaderMenuItem";
            this.disconnectReaderMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.D)));
            this.disconnectReaderMenuItem.Size = new System.Drawing.Size(296, 22);
            this.disconnectReaderMenuItem.Text = "Disconnect Reader";
            this.disconnectReaderMenuItem.Click += new System.EventHandler(this.disconnectReaderMenuItem_Click);
            // 
            // toolStripMenuItem3
            // 
            this.toolStripMenuItem3.Name = "toolStripMenuItem3";
            this.toolStripMenuItem3.Size = new System.Drawing.Size(293, 6);
            // 
            // exitToolStripMenuItem1
            // 
            this.exitToolStripMenuItem1.Name = "exitToolStripMenuItem1";
            this.exitToolStripMenuItem1.Size = new System.Drawing.Size(296, 22);
            this.exitToolStripMenuItem1.Text = "Exit";
            this.exitToolStripMenuItem1.Click += new System.EventHandler(this.exitToolStripMenuItem1_Click);
            // 
            // editToolStripMenuItem
            // 
            this.editToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.copyCtrlCToolStripMenuItem,
            this.pasteCtrlVToolStripMenuItem});
            this.editToolStripMenuItem.Name = "editToolStripMenuItem";
            this.editToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)((System.Windows.Forms.Keys.Alt | System.Windows.Forms.Keys.E)));
            this.editToolStripMenuItem.ShowShortcutKeys = false;
            this.editToolStripMenuItem.Size = new System.Drawing.Size(42, 21);
            this.editToolStripMenuItem.Text = "Edit";
            this.editToolStripMenuItem.Click += new System.EventHandler(this.editToolStripMenuItem_Click);
            // 
            // copyCtrlCToolStripMenuItem
            // 
            this.copyCtrlCToolStripMenuItem.Name = "copyCtrlCToolStripMenuItem";
            this.copyCtrlCToolStripMenuItem.Size = new System.Drawing.Size(183, 22);
            this.copyCtrlCToolStripMenuItem.Text = "Copy          Ctrl+C";
            this.copyCtrlCToolStripMenuItem.Click += new System.EventHandler(this.copyCtrlCToolStripMenuItem_Click);
            // 
            // pasteCtrlVToolStripMenuItem
            // 
            this.pasteCtrlVToolStripMenuItem.Name = "pasteCtrlVToolStripMenuItem";
            this.pasteCtrlVToolStripMenuItem.Size = new System.Drawing.Size(183, 22);
            this.pasteCtrlVToolStripMenuItem.Text = "Paste         Ctrl+V";
            this.pasteCtrlVToolStripMenuItem.Click += new System.EventHandler(this.pasteCtrlVToolStripMenuItem_Click);
            // 
            // toolStripMenuItem1
            // 
            this.toolStripMenuItem1.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.rFPowerLevelToolStripMenuItem,
            this.protocolToolStripMenuItem,
            this.readOnAntennaNumberToolStripMenuItem,
            this.bankToReadToolStripMenuItem,
            this.baudRateToolStripMenuItem});
            this.toolStripMenuItem1.Name = "toolStripMenuItem1";
            this.toolStripMenuItem1.ShortcutKeys = ((System.Windows.Forms.Keys)((System.Windows.Forms.Keys.Alt | System.Windows.Forms.Keys.O)));
            this.toolStripMenuItem1.ShowShortcutKeys = false;
            this.toolStripMenuItem1.Size = new System.Drawing.Size(60, 21);
            this.toolStripMenuItem1.Text = "Option";
            this.toolStripMenuItem1.Click += new System.EventHandler(this.toolStripMenuItem1_Click);
            // 
            // rFPowerLevelToolStripMenuItem
            // 
            this.rFPowerLevelToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.power15dBmMenuItem,
            this.power16dBmMenuItem,
            this.power17dBmMenuItem,
            this.power18dBmMenuItem,
            this.power19dBmMenuItem,
            this.power20dBmMenuItem,
            this.power21dBmMenuItem,
            this.power22dBmMenuItem,
            this.power23dBmMenuItem,
            this.power24dBmMenuItem,
            this.power25dBmMenuItem,
            this.power26dBmMenuItem,
            this.power27dBmMenuItem,
            this.power28dBmMenuItem,
            this.power29dBmMenuItem,
            this.power30dBmMenuItem,
            this.power31dBmMenuItem,
            this.power31p5dBmMenuItem,
            this.toolStripTextBox1});
            this.rFPowerLevelToolStripMenuItem.Enabled = false;
            this.rFPowerLevelToolStripMenuItem.Name = "rFPowerLevelToolStripMenuItem";
            this.rFPowerLevelToolStripMenuItem.Size = new System.Drawing.Size(253, 22);
            this.rFPowerLevelToolStripMenuItem.Text = "RF Power Level...";
            this.rFPowerLevelToolStripMenuItem.Click += new System.EventHandler(this.rFPowerLevelToolStripMenuItem_Click);
            // 
            // power15dBmMenuItem
            // 
            this.power15dBmMenuItem.CheckOnClick = true;
            this.power15dBmMenuItem.Name = "power15dBmMenuItem";
            this.power15dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power15dBmMenuItem.Text = "15 dBm";
            this.power15dBmMenuItem.Click += new System.EventHandler(this.power15dBmMenuItem_Click);
            // 
            // power16dBmMenuItem
            // 
            this.power16dBmMenuItem.CheckOnClick = true;
            this.power16dBmMenuItem.Name = "power16dBmMenuItem";
            this.power16dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power16dBmMenuItem.Text = "16 dBm";
            this.power16dBmMenuItem.Click += new System.EventHandler(this.power16dBmMenuItem_Click);
            // 
            // power17dBmMenuItem
            // 
            this.power17dBmMenuItem.CheckOnClick = true;
            this.power17dBmMenuItem.Name = "power17dBmMenuItem";
            this.power17dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power17dBmMenuItem.Text = "17 dBm";
            this.power17dBmMenuItem.Click += new System.EventHandler(this.power17dBmMenuItem_Click);
            // 
            // power18dBmMenuItem
            // 
            this.power18dBmMenuItem.CheckOnClick = true;
            this.power18dBmMenuItem.Name = "power18dBmMenuItem";
            this.power18dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power18dBmMenuItem.Text = "18 dBm";
            this.power18dBmMenuItem.Click += new System.EventHandler(this.power18dBmMenuItem_Click);
            // 
            // power19dBmMenuItem
            // 
            this.power19dBmMenuItem.CheckOnClick = true;
            this.power19dBmMenuItem.Name = "power19dBmMenuItem";
            this.power19dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power19dBmMenuItem.Text = "19 dBm";
            this.power19dBmMenuItem.Click += new System.EventHandler(this.power19dBmMenuItem_Click);
            // 
            // power20dBmMenuItem
            // 
            this.power20dBmMenuItem.CheckOnClick = true;
            this.power20dBmMenuItem.Name = "power20dBmMenuItem";
            this.power20dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power20dBmMenuItem.Text = "20 dBm";
            this.power20dBmMenuItem.Click += new System.EventHandler(this.power20dBmMenuItem_Click);
            // 
            // power21dBmMenuItem
            // 
            this.power21dBmMenuItem.CheckOnClick = true;
            this.power21dBmMenuItem.Name = "power21dBmMenuItem";
            this.power21dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power21dBmMenuItem.Text = "21 dBm";
            this.power21dBmMenuItem.Click += new System.EventHandler(this.power21dBmMenuItem_Click);
            // 
            // power22dBmMenuItem
            // 
            this.power22dBmMenuItem.CheckOnClick = true;
            this.power22dBmMenuItem.Name = "power22dBmMenuItem";
            this.power22dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power22dBmMenuItem.Text = "22 dBm";
            this.power22dBmMenuItem.Click += new System.EventHandler(this.power22dBmMenuItem_Click);
            // 
            // power23dBmMenuItem
            // 
            this.power23dBmMenuItem.CheckOnClick = true;
            this.power23dBmMenuItem.Name = "power23dBmMenuItem";
            this.power23dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power23dBmMenuItem.Text = "23 dBm";
            this.power23dBmMenuItem.Click += new System.EventHandler(this.power23dBmMenuItem_Click);
            // 
            // power24dBmMenuItem
            // 
            this.power24dBmMenuItem.CheckOnClick = true;
            this.power24dBmMenuItem.Name = "power24dBmMenuItem";
            this.power24dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power24dBmMenuItem.Text = "24 dBm";
            this.power24dBmMenuItem.Click += new System.EventHandler(this.power24dBmMenuItem_Click);
            // 
            // power25dBmMenuItem
            // 
            this.power25dBmMenuItem.CheckOnClick = true;
            this.power25dBmMenuItem.Name = "power25dBmMenuItem";
            this.power25dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power25dBmMenuItem.Text = "25 dBm";
            this.power25dBmMenuItem.Click += new System.EventHandler(this.power25dBmMenuItem_Click);
            // 
            // power26dBmMenuItem
            // 
            this.power26dBmMenuItem.CheckOnClick = true;
            this.power26dBmMenuItem.Name = "power26dBmMenuItem";
            this.power26dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power26dBmMenuItem.Text = "26 dBm";
            this.power26dBmMenuItem.Click += new System.EventHandler(this.power26dBmMenuItem_Click);
            // 
            // power27dBmMenuItem
            // 
            this.power27dBmMenuItem.CheckOnClick = true;
            this.power27dBmMenuItem.Name = "power27dBmMenuItem";
            this.power27dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power27dBmMenuItem.Text = "27 dBm";
            this.power27dBmMenuItem.Click += new System.EventHandler(this.power27dBmMenuItem_Click);
            // 
            // power28dBmMenuItem
            // 
            this.power28dBmMenuItem.CheckOnClick = true;
            this.power28dBmMenuItem.Name = "power28dBmMenuItem";
            this.power28dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power28dBmMenuItem.Text = "28 dBm";
            this.power28dBmMenuItem.Click += new System.EventHandler(this.power28dBmMenuItem_Click);
            // 
            // power29dBmMenuItem
            // 
            this.power29dBmMenuItem.CheckOnClick = true;
            this.power29dBmMenuItem.Name = "power29dBmMenuItem";
            this.power29dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power29dBmMenuItem.Text = "29 dBm";
            this.power29dBmMenuItem.Click += new System.EventHandler(this.power29dBmMenuItem_Click);
            // 
            // power30dBmMenuItem
            // 
            this.power30dBmMenuItem.Checked = true;
            this.power30dBmMenuItem.CheckOnClick = true;
            this.power30dBmMenuItem.CheckState = System.Windows.Forms.CheckState.Checked;
            this.power30dBmMenuItem.Name = "power30dBmMenuItem";
            this.power30dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power30dBmMenuItem.Text = "30 dBm";
            this.power30dBmMenuItem.Click += new System.EventHandler(this.power30dBmMenuItem_Click);
            // 
            // power31dBmMenuItem
            // 
            this.power31dBmMenuItem.CheckOnClick = true;
            this.power31dBmMenuItem.Name = "power31dBmMenuItem";
            this.power31dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power31dBmMenuItem.Text = "31 dBm";
            this.power31dBmMenuItem.Click += new System.EventHandler(this.power31dBmMenuItem_Click);
            // 
            // power31p5dBmMenuItem
            // 
            this.power31p5dBmMenuItem.CheckOnClick = true;
            this.power31p5dBmMenuItem.Name = "power31p5dBmMenuItem";
            this.power31p5dBmMenuItem.Size = new System.Drawing.Size(160, 22);
            this.power31p5dBmMenuItem.Text = "31.5 dBm";
            this.power31p5dBmMenuItem.Click += new System.EventHandler(this.power31p5dBmMenuItem_Click);
            // 
            // toolStripTextBox1
            // 
            this.toolStripTextBox1.Name = "toolStripTextBox1";
            this.toolStripTextBox1.Size = new System.Drawing.Size(100, 25);
            this.toolStripTextBox1.Click += new System.EventHandler(this.toolStripTextBox1_Click);
            // 
            // protocolToolStripMenuItem
            // 
            this.protocolToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.gEN2ToolStripMenuItem,
            this.iSO180006BToolStripMenuItem,
            this.iPX64KHzToolStripMenuItem,
            this.iPX256KHzToolStripMenuItem,
            this.readOnAllAvailableProtocolsToolStripMenuItem});
            this.protocolToolStripMenuItem.Name = "protocolToolStripMenuItem";
            this.protocolToolStripMenuItem.Size = new System.Drawing.Size(253, 22);
            this.protocolToolStripMenuItem.Text = "Protocol...";
            this.protocolToolStripMenuItem.Click += new System.EventHandler(this.protocolToolStripMenuItem_Click);
            // 
            // gEN2ToolStripMenuItem
            // 
            this.gEN2ToolStripMenuItem.Checked = true;
            this.gEN2ToolStripMenuItem.CheckOnClick = true;
            this.gEN2ToolStripMenuItem.CheckState = System.Windows.Forms.CheckState.Checked;
            this.gEN2ToolStripMenuItem.Name = "gEN2ToolStripMenuItem";
            this.gEN2ToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Alt)
                        | System.Windows.Forms.Keys.G)));
            this.gEN2ToolStripMenuItem.Size = new System.Drawing.Size(341, 22);
            this.gEN2ToolStripMenuItem.Text = "GEN2";
            this.gEN2ToolStripMenuItem.Click += new System.EventHandler(this.gEN2ToolStripMenuItem_Click);
            // 
            // iSO180006BToolStripMenuItem
            // 
            this.iSO180006BToolStripMenuItem.CheckOnClick = true;
            this.iSO180006BToolStripMenuItem.Name = "iSO180006BToolStripMenuItem";
            this.iSO180006BToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Alt)
                        | System.Windows.Forms.Keys.I)));
            this.iSO180006BToolStripMenuItem.Size = new System.Drawing.Size(341, 22);
            this.iSO180006BToolStripMenuItem.Text = "ISO-180006B";
            this.iSO180006BToolStripMenuItem.Click += new System.EventHandler(this.iSO180006BToolStripMenuItem_Click);
            // 
            // iPX64KHzToolStripMenuItem
            // 
            this.iPX64KHzToolStripMenuItem.CheckOnClick = true;
            this.iPX64KHzToolStripMenuItem.Name = "iPX64KHzToolStripMenuItem";
            this.iPX64KHzToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Alt)
                        | System.Windows.Forms.Keys.D6)));
            this.iPX64KHzToolStripMenuItem.Size = new System.Drawing.Size(341, 22);
            this.iPX64KHzToolStripMenuItem.Text = "IPX (64KHz)";
            this.iPX64KHzToolStripMenuItem.Click += new System.EventHandler(this.iPX64KHzToolStripMenuItem_Click);
            // 
            // iPX256KHzToolStripMenuItem
            // 
            this.iPX256KHzToolStripMenuItem.CheckOnClick = true;
            this.iPX256KHzToolStripMenuItem.Name = "iPX256KHzToolStripMenuItem";
            this.iPX256KHzToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Alt)
                        | System.Windows.Forms.Keys.D2)));
            this.iPX256KHzToolStripMenuItem.Size = new System.Drawing.Size(341, 22);
            this.iPX256KHzToolStripMenuItem.Text = "IPX (256KHz)";
            this.iPX256KHzToolStripMenuItem.Click += new System.EventHandler(this.iPX256KHzToolStripMenuItem_Click);
            // 
            // readOnAllAvailableProtocolsToolStripMenuItem
            // 
            this.readOnAllAvailableProtocolsToolStripMenuItem.CheckOnClick = true;
            this.readOnAllAvailableProtocolsToolStripMenuItem.Name = "readOnAllAvailableProtocolsToolStripMenuItem";
            this.readOnAllAvailableProtocolsToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Alt)
                        | System.Windows.Forms.Keys.A)));
            this.readOnAllAvailableProtocolsToolStripMenuItem.Size = new System.Drawing.Size(341, 22);
            this.readOnAllAvailableProtocolsToolStripMenuItem.Text = "Read on all available protocols";
            this.readOnAllAvailableProtocolsToolStripMenuItem.Click += new System.EventHandler(this.readOnAllAvailableProtocolsToolStripMenuItem_Click);
            // 
            // readOnAntennaNumberToolStripMenuItem
            // 
            this.readOnAntennaNumberToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.antenna1ToolStripMenuItem,
            this.antenna2ToolStripMenuItem,
            this.antenna3ToolStripMenuItem,
            this.antenna4ToolStripMenuItem,
            this.readOnAllConnectedAntennasToolStripMenuItem});
            this.readOnAntennaNumberToolStripMenuItem.Name = "readOnAntennaNumberToolStripMenuItem";
            this.readOnAntennaNumberToolStripMenuItem.Size = new System.Drawing.Size(253, 22);
            this.readOnAntennaNumberToolStripMenuItem.Text = "Read on...";
            this.readOnAntennaNumberToolStripMenuItem.Click += new System.EventHandler(this.readOnAntennaNumberToolStripMenuItem_Click);
            // 
            // antenna1ToolStripMenuItem
            // 
            this.antenna1ToolStripMenuItem.Checked = true;
            this.antenna1ToolStripMenuItem.CheckOnClick = true;
            this.antenna1ToolStripMenuItem.CheckState = System.Windows.Forms.CheckState.Checked;
            this.antenna1ToolStripMenuItem.Name = "antenna1ToolStripMenuItem";
            this.antenna1ToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.D1)));
            this.antenna1ToolStripMenuItem.Size = new System.Drawing.Size(372, 22);
            this.antenna1ToolStripMenuItem.Text = "Antenna #1";
            this.antenna1ToolStripMenuItem.Click += new System.EventHandler(this.antenna1ToolStripMenuItem_Click);
            // 
            // antenna2ToolStripMenuItem
            // 
            this.antenna2ToolStripMenuItem.CheckOnClick = true;
            this.antenna2ToolStripMenuItem.Name = "antenna2ToolStripMenuItem";
            this.antenna2ToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.D2)));
            this.antenna2ToolStripMenuItem.Size = new System.Drawing.Size(372, 22);
            this.antenna2ToolStripMenuItem.Text = "Antenna #2";
            this.antenna2ToolStripMenuItem.Click += new System.EventHandler(this.antenna2ToolStripMenuItem_Click);
            // 
            // antenna3ToolStripMenuItem
            // 
            this.antenna3ToolStripMenuItem.CheckOnClick = true;
            this.antenna3ToolStripMenuItem.Name = "antenna3ToolStripMenuItem";
            this.antenna3ToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.D3)));
            this.antenna3ToolStripMenuItem.Size = new System.Drawing.Size(372, 22);
            this.antenna3ToolStripMenuItem.Text = "Antenna #3";
            this.antenna3ToolStripMenuItem.Click += new System.EventHandler(this.antenna3ToolStripMenuItem_Click);
            // 
            // antenna4ToolStripMenuItem
            // 
            this.antenna4ToolStripMenuItem.CheckOnClick = true;
            this.antenna4ToolStripMenuItem.Name = "antenna4ToolStripMenuItem";
            this.antenna4ToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.D4)));
            this.antenna4ToolStripMenuItem.Size = new System.Drawing.Size(372, 22);
            this.antenna4ToolStripMenuItem.Text = "Antenna #4";
            this.antenna4ToolStripMenuItem.Click += new System.EventHandler(this.antenna4ToolStripMenuItem_Click);
            // 
            // readOnAllConnectedAntennasToolStripMenuItem
            // 
            this.readOnAllConnectedAntennasToolStripMenuItem.CheckOnClick = true;
            this.readOnAllConnectedAntennasToolStripMenuItem.Name = "readOnAllConnectedAntennasToolStripMenuItem";
            this.readOnAllConnectedAntennasToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.A)));
            this.readOnAllConnectedAntennasToolStripMenuItem.Size = new System.Drawing.Size(372, 22);
            this.readOnAllConnectedAntennasToolStripMenuItem.Text = "Read on all connected antennas";
            this.readOnAllConnectedAntennasToolStripMenuItem.Click += new System.EventHandler(this.readOnAllConnectedAntennasToolStripMenuItem_Click);
            // 
            // bankToReadToolStripMenuItem
            // 
            this.bankToReadToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.reservedToolStripMenuItem,
            this.ePCToolStripMenuItem,
            this.tIDToolStripMenuItem,
            this.userToolStripMenuItem});
            this.bankToReadToolStripMenuItem.Name = "bankToReadToolStripMenuItem";
            this.bankToReadToolStripMenuItem.Size = new System.Drawing.Size(253, 22);
            this.bankToReadToolStripMenuItem.Text = "GEN2 Bank to Read/Write...";
            this.bankToReadToolStripMenuItem.Click += new System.EventHandler(this.bankToReadToolStripMenuItem_Click);
            // 
            // reservedToolStripMenuItem
            // 
            this.reservedToolStripMenuItem.CheckOnClick = true;
            this.reservedToolStripMenuItem.Name = "reservedToolStripMenuItem";
            this.reservedToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.R)));
            this.reservedToolStripMenuItem.Size = new System.Drawing.Size(240, 22);
            this.reservedToolStripMenuItem.Text = "Reserved";
            this.reservedToolStripMenuItem.Click += new System.EventHandler(this.reservedToolStripMenuItem_Click);
            // 
            // ePCToolStripMenuItem
            // 
            this.ePCToolStripMenuItem.CheckOnClick = true;
            this.ePCToolStripMenuItem.Name = "ePCToolStripMenuItem";
            this.ePCToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.E)));
            this.ePCToolStripMenuItem.Size = new System.Drawing.Size(240, 22);
            this.ePCToolStripMenuItem.Text = "EPC";
            this.ePCToolStripMenuItem.Click += new System.EventHandler(this.ePCToolStripMenuItem_Click);
            // 
            // tIDToolStripMenuItem
            // 
            this.tIDToolStripMenuItem.CheckOnClick = true;
            this.tIDToolStripMenuItem.Name = "tIDToolStripMenuItem";
            this.tIDToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.T)));
            this.tIDToolStripMenuItem.Size = new System.Drawing.Size(240, 22);
            this.tIDToolStripMenuItem.Text = "TID";
            this.tIDToolStripMenuItem.Click += new System.EventHandler(this.tIDToolStripMenuItem_Click);
            // 
            // userToolStripMenuItem
            // 
            this.userToolStripMenuItem.CheckOnClick = true;
            this.userToolStripMenuItem.Name = "userToolStripMenuItem";
            this.userToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.U)));
            this.userToolStripMenuItem.Size = new System.Drawing.Size(240, 22);
            this.userToolStripMenuItem.Text = "User";
            this.userToolStripMenuItem.Click += new System.EventHandler(this.userToolStripMenuItem_Click);
            // 
            // baudRateToolStripMenuItem
            // 
            this.baudRateToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.baud9600MenuItem,
            this.baud19200MenuItem,
            this.baud38400MenuItem,
            this.baud115200MenuItem,
            this.baud230400MenuItem,
            this.baud460800MenuItem,
            this.baud921600MenuItem});
            this.baudRateToolStripMenuItem.Enabled = false;
            this.baudRateToolStripMenuItem.Name = "baudRateToolStripMenuItem";
            this.baudRateToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)(((System.Windows.Forms.Keys.Control | System.Windows.Forms.Keys.Shift)
                        | System.Windows.Forms.Keys.B)));
            this.baudRateToolStripMenuItem.Size = new System.Drawing.Size(253, 22);
            this.baudRateToolStripMenuItem.Text = "Baud Rate...";
            // 
            // baud9600MenuItem
            // 
            this.baud9600MenuItem.Checked = true;
            this.baud9600MenuItem.CheckOnClick = true;
            this.baud9600MenuItem.CheckState = System.Windows.Forms.CheckState.Checked;
            this.baud9600MenuItem.Name = "baud9600MenuItem";
            this.baud9600MenuItem.Size = new System.Drawing.Size(118, 22);
            this.baud9600MenuItem.Text = "9600";
            this.baud9600MenuItem.Click += new System.EventHandler(this.baud9600MenuItem_Click);
            // 
            // baud19200MenuItem
            // 
            this.baud19200MenuItem.CheckOnClick = true;
            this.baud19200MenuItem.Name = "baud19200MenuItem";
            this.baud19200MenuItem.Size = new System.Drawing.Size(118, 22);
            this.baud19200MenuItem.Text = "19200";
            this.baud19200MenuItem.Click += new System.EventHandler(this.baud19200MenuItem_Click);
            // 
            // baud38400MenuItem
            // 
            this.baud38400MenuItem.CheckOnClick = true;
            this.baud38400MenuItem.Name = "baud38400MenuItem";
            this.baud38400MenuItem.Size = new System.Drawing.Size(118, 22);
            this.baud38400MenuItem.Text = "38400";
            this.baud38400MenuItem.Click += new System.EventHandler(this.baud38400MenuItem_Click);
            // 
            // baud115200MenuItem
            // 
            this.baud115200MenuItem.CheckOnClick = true;
            this.baud115200MenuItem.Name = "baud115200MenuItem";
            this.baud115200MenuItem.Size = new System.Drawing.Size(118, 22);
            this.baud115200MenuItem.Text = "115200";
            this.baud115200MenuItem.Click += new System.EventHandler(this.baud115200MenuItem_Click);
            // 
            // baud230400MenuItem
            // 
            this.baud230400MenuItem.CheckOnClick = true;
            this.baud230400MenuItem.Name = "baud230400MenuItem";
            this.baud230400MenuItem.Size = new System.Drawing.Size(118, 22);
            this.baud230400MenuItem.Text = "230400";
            this.baud230400MenuItem.Click += new System.EventHandler(this.baud230400MenuItem_Click);
            // 
            // baud460800MenuItem
            // 
            this.baud460800MenuItem.CheckOnClick = true;
            this.baud460800MenuItem.Name = "baud460800MenuItem";
            this.baud460800MenuItem.Size = new System.Drawing.Size(118, 22);
            this.baud460800MenuItem.Text = "460800";
            this.baud460800MenuItem.Click += new System.EventHandler(this.baud460800MenuItem_Click);
            // 
            // baud921600MenuItem
            // 
            this.baud921600MenuItem.CheckOnClick = true;
            this.baud921600MenuItem.Name = "baud921600MenuItem";
            this.baud921600MenuItem.Size = new System.Drawing.Size(118, 22);
            this.baud921600MenuItem.Text = "921600";
            this.baud921600MenuItem.Click += new System.EventHandler(this.baud921600MenuItem_Click);
            // 
            // helpToolStripMenuItem
            // 
            this.helpToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.aboutToolStripMenuItem,
            this.demoToolAssistantToolStripMenuItem});
            this.helpToolStripMenuItem.Name = "helpToolStripMenuItem";
            this.helpToolStripMenuItem.ShortcutKeys = ((System.Windows.Forms.Keys)((System.Windows.Forms.Keys.Alt | System.Windows.Forms.Keys.H)));
            this.helpToolStripMenuItem.ShowShortcutKeys = false;
            this.helpToolStripMenuItem.Size = new System.Drawing.Size(47, 21);
            this.helpToolStripMenuItem.Text = "Help";
            this.helpToolStripMenuItem.Click += new System.EventHandler(this.helpToolStripMenuItem_Click);
            // 
            // aboutToolStripMenuItem
            // 
            this.aboutToolStripMenuItem.DropDownItems.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.demoApplicationToolStripMenuItem,
            this.thingMagicReaderToolStripMenuItem});
            this.aboutToolStripMenuItem.Name = "aboutToolStripMenuItem";
            this.aboutToolStripMenuItem.Size = new System.Drawing.Size(205, 22);
            this.aboutToolStripMenuItem.Text = "About";
            this.aboutToolStripMenuItem.Click += new System.EventHandler(this.aboutToolStripMenuItem_Click);
            // 
            // demoApplicationToolStripMenuItem
            // 
            this.demoApplicationToolStripMenuItem.Name = "demoApplicationToolStripMenuItem";
            this.demoApplicationToolStripMenuItem.Size = new System.Drawing.Size(190, 22);
            this.demoApplicationToolStripMenuItem.Text = "Demo Application";
            this.demoApplicationToolStripMenuItem.Click += new System.EventHandler(this.demoApplicationToolStripMenuItem_Click);
            // 
            // thingMagicReaderToolStripMenuItem
            // 
            this.thingMagicReaderToolStripMenuItem.Enabled = false;
            this.thingMagicReaderToolStripMenuItem.Name = "thingMagicReaderToolStripMenuItem";
            this.thingMagicReaderToolStripMenuItem.Size = new System.Drawing.Size(190, 22);
            this.thingMagicReaderToolStripMenuItem.Text = "ThingMagic Reader";
            this.thingMagicReaderToolStripMenuItem.Click += new System.EventHandler(this.thingMagicReaderToolStripMenuItem_Click);
            // 
            // demoToolAssistantToolStripMenuItem
            // 
            this.demoToolAssistantToolStripMenuItem.Name = "demoToolAssistantToolStripMenuItem";
            this.demoToolAssistantToolStripMenuItem.Size = new System.Drawing.Size(205, 22);
            this.demoToolAssistantToolStripMenuItem.Text = "Demo Tool Assistant...";
            this.demoToolAssistantToolStripMenuItem.Click += new System.EventHandler(this.demoToolAssistantToolStripMenuItem_Click);
            // 
            // writeData
            // 
            this.writeData.AllowDrop = true;
            this.writeData.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.writeData.ForeColor = System.Drawing.SystemColors.InactiveCaption;
            this.writeData.Location = new System.Drawing.Point(85, 710);
            this.writeData.Name = "writeData";
            this.writeData.Size = new System.Drawing.Size(652, 20);
            this.writeData.TabIndex = 20;
            this.writeData.Text = "Type Tag Data for Write Data here (Hex String - No Spaces)";
            this.writeData.TextChanged += new System.EventHandler(this.writeData_TextChanged);
            // 
            // writeTagData
            // 
            this.writeTagData.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.writeTagData.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.writeTagData.DialogResult = System.Windows.Forms.DialogResult.Cancel;
            this.writeTagData.Enabled = false;
            this.writeTagData.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.writeTagData.Location = new System.Drawing.Point(751, 710);
            this.writeTagData.Name = "writeTagData";
            this.writeTagData.Size = new System.Drawing.Size(172, 23);
            this.writeTagData.TabIndex = 21;
            this.writeTagData.Text = "Write Data (Single)";
            this.writeTagData.UseVisualStyleBackColor = false;
            this.writeTagData.Click += new System.EventHandler(this.writeTagData_Click);
            // 
            // label11
            // 
            this.label11.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.label11.AutoSize = true;
            this.label11.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label11.Location = new System.Drawing.Point(10, 578);
            this.label11.Name = "label11";
            this.label11.Size = new System.Drawing.Size(61, 13);
            this.label11.TabIndex = 34;
            this.label11.Text = "Select EPC";
            // 
            // label12
            // 
            this.label12.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.label12.AutoSize = true;
            this.label12.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label12.Location = new System.Drawing.Point(10, 611);
            this.label12.Name = "label12";
            this.label12.Size = new System.Drawing.Size(56, 13);
            this.label12.TabIndex = 35;
            this.label12.Text = "Write EPC";
            // 
            // label13
            // 
            this.label13.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.label13.AutoSize = true;
            this.label13.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label13.Location = new System.Drawing.Point(10, 714);
            this.label13.Name = "label13";
            this.label13.Size = new System.Drawing.Size(58, 13);
            this.label13.TabIndex = 36;
            this.label13.Text = "Write Data";
            // 
            // readerAddr
            // 
            this.readerAddr.ForeColor = System.Drawing.SystemColors.InactiveCaption;
            this.readerAddr.Location = new System.Drawing.Point(114, 38);
            this.readerAddr.Name = "readerAddr";
            this.readerAddr.Size = new System.Drawing.Size(118, 20);
            this.readerAddr.TabIndex = 1;
            this.readerAddr.Text = "COM1";
            this.readerAddr.TextChanged += new System.EventHandler(this.readerAddr_TextChanged);
            // 
            // fakeExit
            // 
            this.fakeExit.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.fakeExit.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.fakeExit.DialogResult = System.Windows.Forms.DialogResult.Cancel;
            this.fakeExit.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.fakeExit.Location = new System.Drawing.Point(899, 535);
            this.fakeExit.Name = "fakeExit";
            this.fakeExit.Size = new System.Drawing.Size(24, 23);
            this.fakeExit.TabIndex = 38;
            this.fakeExit.Text = "Exit";
            this.fakeExit.UseVisualStyleBackColor = false;
            this.fakeExit.Visible = false;
            this.fakeExit.Click += new System.EventHandler(this.fakeExit_Click);
            // 
            // label5
            // 
            this.label5.AutoSize = true;
            this.label5.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label5.Location = new System.Drawing.Point(271, 182);
            this.label5.Name = "label5";
            this.label5.Size = new System.Drawing.Size(46, 13);
            this.label5.TabIndex = 39;
            this.label5.Text = "Protocol";
            this.label5.Click += new System.EventHandler(this.label5_Click);
            // 
            // addrReadData
            // 
            this.addrReadData.AllowDrop = true;
            this.addrReadData.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.addrReadData.ForeColor = System.Drawing.SystemColors.InactiveCaption;
            this.addrReadData.Location = new System.Drawing.Point(85, 642);
            this.addrReadData.Name = "addrReadData";
            this.addrReadData.Size = new System.Drawing.Size(285, 20);
            this.addrReadData.TabIndex = 16;
            this.addrReadData.Text = "Type Byte Address of Memory to Read and Write here";
            this.addrReadData.TextChanged += new System.EventHandler(this.addrReadData_TextChanged);
            // 
            // readTagData
            // 
            this.readTagData.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.readTagData.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.readTagData.Enabled = false;
            this.readTagData.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.readTagData.Location = new System.Drawing.Point(751, 642);
            this.readTagData.Name = "readTagData";
            this.readTagData.Size = new System.Drawing.Size(172, 23);
            this.readTagData.TabIndex = 18;
            this.readTagData.Text = "Read Data (Single)";
            this.readTagData.UseVisualStyleBackColor = false;
            this.readTagData.Click += new System.EventHandler(this.readTagData_Click);
            // 
            // label14
            // 
            this.label14.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.label14.AutoSize = true;
            this.label14.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label14.Location = new System.Drawing.Point(10, 645);
            this.label14.Name = "label14";
            this.label14.Size = new System.Drawing.Size(69, 13);
            this.label14.TabIndex = 40;
            this.label14.Text = "Byte Address";
            // 
            // label15
            // 
            this.label15.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.label15.AutoSize = true;
            this.label15.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label15.Location = new System.Drawing.Point(9, 679);
            this.label15.Name = "label15";
            this.label15.Size = new System.Drawing.Size(59, 13);
            this.label15.TabIndex = 43;
            this.label15.Text = "Read Data";
            // 
            // readData
            // 
            this.readData.AllowDrop = true;
            this.readData.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.readData.BackColor = System.Drawing.SystemColors.Control;
            this.readData.ForeColor = System.Drawing.SystemColors.ControlText;
            this.readData.Location = new System.Drawing.Point(85, 676);
            this.readData.Name = "readData";
            this.readData.ReadOnly = true;
            this.readData.Size = new System.Drawing.Size(652, 20);
            this.readData.TabIndex = 19;
            this.readData.TextChanged += new System.EventHandler(this.readData_TextChanged);
            // 
            // byteCountTextBox
            // 
            this.byteCountTextBox.AllowDrop = true;
            this.byteCountTextBox.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.byteCountTextBox.ForeColor = System.Drawing.SystemColors.InactiveCaption;
            this.byteCountTextBox.Location = new System.Drawing.Point(446, 642);
            this.byteCountTextBox.Name = "byteCountTextBox";
            this.byteCountTextBox.Size = new System.Drawing.Size(291, 20);
            this.byteCountTextBox.TabIndex = 17;
            this.byteCountTextBox.Text = "Type Number of Bytes to read for Read Data here";
            this.byteCountTextBox.TextChanged += new System.EventHandler(this.byteCountTextBox_TextChanged);
            // 
            // label16
            // 
            this.label16.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.label16.AutoSize = true;
            this.label16.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label16.Location = new System.Drawing.Point(381, 645);
            this.label16.Name = "label16";
            this.label16.Size = new System.Drawing.Size(59, 13);
            this.label16.TabIndex = 45;
            this.label16.Text = "Byte Count";
            // 
            // label17
            // 
            this.label17.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Left)));
            this.label17.AutoSize = true;
            this.label17.Font = new System.Drawing.Font("Microsoft Sans Serif", 11.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label17.Location = new System.Drawing.Point(10, 541);
            this.label17.Name = "label17";
            this.label17.Size = new System.Drawing.Size(193, 18);
            this.label17.TabIndex = 48;
            this.label17.Text = "Access Commands Section";
            // 
            // label10
            // 
            this.label10.AutoSize = true;
            this.label10.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label10.Location = new System.Drawing.Point(9, 131);
            this.label10.Name = "label10";
            this.label10.Size = new System.Drawing.Size(162, 13);
            this.label10.TabIndex = 49;
            this.label10.Text = "Searching highlighted Protocols..";
            // 
            // gen2Label
            // 
            this.gen2Label.AutoSize = true;
            this.gen2Label.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.gen2Label.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.gen2Label.Location = new System.Drawing.Point(180, 130);
            this.gen2Label.Name = "gen2Label";
            this.gen2Label.Size = new System.Drawing.Size(38, 15);
            this.gen2Label.TabIndex = 50;
            this.gen2Label.Text = "GEN2";
            this.gen2Label.Click += new System.EventHandler(this.gen2Label_Click);
            // 
            // isoLabel
            // 
            this.isoLabel.AutoSize = true;
            this.isoLabel.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.isoLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.isoLabel.Location = new System.Drawing.Point(230, 130);
            this.isoLabel.Name = "isoLabel";
            this.isoLabel.Size = new System.Drawing.Size(73, 15);
            this.isoLabel.TabIndex = 51;
            this.isoLabel.Text = "ISO18000-6B";
            this.isoLabel.Click += new System.EventHandler(this.isoLabel_Click);
            // 
            // ipx64Label
            // 
            this.ipx64Label.AutoSize = true;
            this.ipx64Label.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.ipx64Label.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.ipx64Label.Location = new System.Drawing.Point(315, 130);
            this.ipx64Label.Name = "ipx64Label";
            this.ipx64Label.Size = new System.Drawing.Size(67, 15);
            this.ipx64Label.TabIndex = 52;
            this.ipx64Label.Text = "IPX (64KHz)";
            // 
            // ipx256Label
            // 
            this.ipx256Label.AutoSize = true;
            this.ipx256Label.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.ipx256Label.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.ipx256Label.Location = new System.Drawing.Point(394, 130);
            this.ipx256Label.Name = "ipx256Label";
            this.ipx256Label.Size = new System.Drawing.Size(73, 15);
            this.ipx256Label.TabIndex = 53;
            this.ipx256Label.Text = "IPX (256KHz)";
            // 
            // allAvailableProtocolLabel
            // 
            this.allAvailableProtocolLabel.AutoSize = true;
            this.allAvailableProtocolLabel.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.allAvailableProtocolLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.allAvailableProtocolLabel.Location = new System.Drawing.Point(479, 130);
            this.allAvailableProtocolLabel.Name = "allAvailableProtocolLabel";
            this.allAvailableProtocolLabel.Size = new System.Drawing.Size(111, 15);
            this.allAvailableProtocolLabel.TabIndex = 54;
            this.allAvailableProtocolLabel.Text = "All available protocols";
            // 
            // label18
            // 
            this.label18.AutoSize = true;
            this.label18.Font = new System.Drawing.Font("Microsoft Sans Serif", 11.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label18.Location = new System.Drawing.Point(9, 150);
            this.label18.Name = "label18";
            this.label18.Size = new System.Drawing.Size(102, 18);
            this.label18.TabIndex = 55;
            this.label18.Text = "Query Section";
            // 
            // totalTimeElapsedTextBox
            // 
            this.totalTimeElapsedTextBox.Location = new System.Drawing.Point(857, 149);
            this.totalTimeElapsedTextBox.Name = "totalTimeElapsedTextBox";
            this.totalTimeElapsedTextBox.ReadOnly = true;
            this.totalTimeElapsedTextBox.Size = new System.Drawing.Size(58, 20);
            this.totalTimeElapsedTextBox.TabIndex = 10;
            this.totalTimeElapsedTextBox.Text = "0";
            this.totalTimeElapsedTextBox.TextChanged += new System.EventHandler(this.totalTimeElapsedTextBox_TextChanged);
            // 
            // inLabel
            // 
            this.inLabel.AutoSize = true;
            this.inLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.inLabel.Location = new System.Drawing.Point(840, 153);
            this.inLabel.Name = "inLabel";
            this.inLabel.Size = new System.Drawing.Size(15, 13);
            this.inLabel.TabIndex = 58;
            this.inLabel.Text = "in";
            this.inLabel.Click += new System.EventHandler(this.inLabel_Click);
            // 
            // totalTagsReadTextBox
            // 
            this.totalTagsReadTextBox.Location = new System.Drawing.Point(794, 149);
            this.totalTagsReadTextBox.Name = "totalTagsReadTextBox";
            this.totalTagsReadTextBox.ReadOnly = true;
            this.totalTagsReadTextBox.Size = new System.Drawing.Size(40, 20);
            this.totalTagsReadTextBox.TabIndex = 9;
            this.totalTagsReadTextBox.Text = "0";
            this.totalTagsReadTextBox.TextChanged += new System.EventHandler(this.totalTagsReadTextBox_TextChanged);
            // 
            // totalTagsReadLabel
            // 
            this.totalTagsReadLabel.AutoSize = true;
            this.totalTagsReadLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.totalTagsReadLabel.Location = new System.Drawing.Point(705, 153);
            this.totalTagsReadLabel.Name = "totalTagsReadLabel";
            this.totalTagsReadLabel.Size = new System.Drawing.Size(87, 13);
            this.totalTagsReadLabel.TabIndex = 60;
            this.totalTagsReadLabel.Text = "Total Tags Read";
            this.totalTagsReadLabel.Click += new System.EventHandler(this.totalTagsReadLabel_Click);
            // 
            // commandTimeLabel
            // 
            this.commandTimeLabel.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.commandTimeLabel.AutoSize = true;
            this.commandTimeLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.commandTimeLabel.Location = new System.Drawing.Point(752, 577);
            this.commandTimeLabel.Name = "commandTimeLabel";
            this.commandTimeLabel.Size = new System.Drawing.Size(80, 13);
            this.commandTimeLabel.TabIndex = 61;
            this.commandTimeLabel.Text = "Command Time";
            // 
            // commandTotalTimeTextBox
            // 
            this.commandTotalTimeTextBox.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.commandTotalTimeTextBox.Location = new System.Drawing.Point(838, 574);
            this.commandTotalTimeTextBox.Name = "commandTotalTimeTextBox";
            this.commandTotalTimeTextBox.ReadOnly = true;
            this.commandTotalTimeTextBox.Size = new System.Drawing.Size(75, 20);
            this.commandTotalTimeTextBox.TabIndex = 13;
            this.commandTotalTimeTextBox.Text = "0";
            // 
            // secondsLabel
            // 
            this.secondsLabel.AutoSize = true;
            this.secondsLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.secondsLabel.Location = new System.Drawing.Point(917, 153);
            this.secondsLabel.Name = "secondsLabel";
            this.secondsLabel.Size = new System.Drawing.Size(12, 13);
            this.secondsLabel.TabIndex = 57;
            this.secondsLabel.Text = "s";
            this.secondsLabel.Click += new System.EventHandler(this.secondsLabel_Click);
            // 
            // commandSecLabel
            // 
            this.commandSecLabel.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Bottom | System.Windows.Forms.AnchorStyles.Right)));
            this.commandSecLabel.AutoSize = true;
            this.commandSecLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.commandSecLabel.Location = new System.Drawing.Point(915, 579);
            this.commandSecLabel.Name = "commandSecLabel";
            this.commandSecLabel.Size = new System.Drawing.Size(12, 13);
            this.commandSecLabel.TabIndex = 63;
            this.commandSecLabel.Text = "s";
            // 
            // label19
            // 
            this.label19.AutoSize = true;
            this.label19.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label19.Location = new System.Drawing.Point(378, 182);
            this.label19.Name = "label19";
            this.label19.Size = new System.Drawing.Size(59, 13);
            this.label19.TabIndex = 64;
            this.label19.Text = "RSSI(dBm)";
            this.label19.Click += new System.EventHandler(this.label19_Click);
            // 
            // WarningLabel
            // 
            this.WarningLabel.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.WarningLabel.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.WarningLabel.Location = new System.Drawing.Point(180, 152);
            this.WarningLabel.Name = "WarningLabel";
            this.WarningLabel.Size = new System.Drawing.Size(173, 16);
            this.WarningLabel.TabIndex = 65;
            this.WarningLabel.Text = "No Warning";
            this.WarningLabel.TextAlign = System.Drawing.ContentAlignment.TopCenter;
            // 
            // label20
            // 
            this.label20.AutoSize = true;
            this.label20.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label20.Location = new System.Drawing.Point(753, 182);
            this.label20.Name = "label20";
            this.label20.Size = new System.Drawing.Size(28, 13);
            this.label20.TabIndex = 66;
            this.label20.Text = "EPC";
            // 
            // label21
            // 
            this.label21.AutoSize = true;
            this.label21.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label21.Location = new System.Drawing.Point(527, 182);
            this.label21.Name = "label21";
            this.label21.Size = new System.Drawing.Size(36, 13);
            this.label21.TabIndex = 67;
            this.label21.Text = "FREQ";
            this.label21.Click += new System.EventHandler(this.label21_Click);
            // 
            // buttonSalvaDati
            // 
            this.buttonSalvaDati.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.buttonSalvaDati.Location = new System.Drawing.Point(808, 100);
            this.buttonSalvaDati.Name = "buttonSalvaDati";
            this.buttonSalvaDati.Size = new System.Drawing.Size(121, 22);
            this.buttonSalvaDati.TabIndex = 68;
            this.buttonSalvaDati.Text = "Save Data";
            this.buttonSalvaDati.UseVisualStyleBackColor = false;
            this.buttonSalvaDati.Click += new System.EventHandler(this.buttonSalvaDati_Click);
            // 
            // labelFrequenza
            // 
            this.labelFrequenza.AutoSize = true;
            this.labelFrequenza.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.labelFrequenza.Location = new System.Drawing.Point(6, 109);
            this.labelFrequenza.Name = "labelFrequenza";
            this.labelFrequenza.Size = new System.Drawing.Size(86, 13);
            this.labelFrequenza.TabIndex = 70;
            this.labelFrequenza.Text = "Frequenza (KHz)";
            this.labelFrequenza.Click += new System.EventHandler(this.labelFrequenza_Click);
            // 
            // frequenza
            // 
            this.frequenza.Location = new System.Drawing.Point(94, 105);
            this.frequenza.Name = "frequenza";
            this.frequenza.Size = new System.Drawing.Size(49, 20);
            this.frequenza.TabIndex = 69;
            this.frequenza.Text = "870000";
            this.frequenza.TextChanged += new System.EventHandler(this.frequenza_TextChanged);
            // 
            // label22
            // 
            this.label22.AutoSize = true;
            this.label22.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label22.Location = new System.Drawing.Point(149, 109);
            this.label22.Name = "label22";
            this.label22.Size = new System.Drawing.Size(92, 13);
            this.label22.TabIndex = 71;
            this.label22.Text = "Frequenza2 (KHz)";
            // 
            // textBox1
            // 
            this.textBox1.Location = new System.Drawing.Point(241, 105);
            this.textBox1.Multiline = true;
            this.textBox1.Name = "textBox1";
            this.textBox1.Size = new System.Drawing.Size(43, 20);
            this.textBox1.TabIndex = 72;
            this.textBox1.Text = "868000";
            this.textBox1.TextChanged += new System.EventHandler(this.textBox1_TextChanged);
            // 
            // textBox2
            // 
            this.textBox2.Location = new System.Drawing.Point(397, 105);
            this.textBox2.Multiline = true;
            this.textBox2.Name = "textBox2";
            this.textBox2.Size = new System.Drawing.Size(39, 20);
            this.textBox2.TabIndex = 74;
            this.textBox2.Text = "12";
            this.textBox2.TextChanged += new System.EventHandler(this.textBox2_TextChanged);
            // 
            // label23
            // 
            this.label23.AutoSize = true;
            this.label23.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label23.Location = new System.Drawing.Point(290, 109);
            this.label23.Name = "label23";
            this.label23.Size = new System.Drawing.Size(106, 13);
            this.label23.TabIndex = 73;
            this.label23.Text = "numero di Frequenze";
            this.label23.Click += new System.EventHandler(this.label23_Click);
            // 
            // textBox3
            // 
            this.textBox3.Location = new System.Drawing.Point(486, 105);
            this.textBox3.Multiline = true;
            this.textBox3.Name = "textBox3";
            this.textBox3.Size = new System.Drawing.Size(39, 20);
            this.textBox3.TabIndex = 76;
            this.textBox3.Text = "00.00";
            // 
            // label24
            // 
            this.label24.AutoSize = true;
            this.label24.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label24.Location = new System.Drawing.Point(443, 109);
            this.label24.Name = "label24";
            this.label24.Size = new System.Drawing.Size(42, 13);
            this.label24.TabIndex = 75;
            this.label24.Text = "esterno";
            // 
            // label25
            // 
            this.label25.AutoSize = true;
            this.label25.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.label25.Location = new System.Drawing.Point(330, 182);
            this.label25.Name = "label25";
            this.label25.Size = new System.Drawing.Size(28, 13);
            this.label25.TabIndex = 77;
            this.label25.Text = "EXT";
            // 
            // turnOnSearch
            // 
            this.turnOnSearch.BackColor = System.Drawing.SystemColors.ButtonFace;
            this.turnOnSearch.Enabled = false;
            this.turnOnSearch.Font = new System.Drawing.Font("Microsoft Sans Serif", 8.25F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.turnOnSearch.Location = new System.Drawing.Point(777, 12);
            this.turnOnSearch.Name = "turnOnSearch";
            this.turnOnSearch.Size = new System.Drawing.Size(152, 25);
            this.turnOnSearch.TabIndex = 78;
            this.turnOnSearch.Text = "TURN-ON Search";
            this.turnOnSearch.UseVisualStyleBackColor = false;
            this.turnOnSearch.Click += new System.EventHandler(this.turnOnSearch_Click);
            // 
            // readWriteDemo
            // 
            this.AcceptButton = this.initReader;
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.BackColor = System.Drawing.SystemColors.ControlLight;
            this.BackgroundImageLayout = System.Windows.Forms.ImageLayout.Center;
            this.CancelButton = this.fakeExit;
            this.ClientSize = new System.Drawing.Size(935, 752);
            this.Controls.Add(this.turnOnSearch);
            this.Controls.Add(this.label25);
            this.Controls.Add(this.textBox3);
            this.Controls.Add(this.label24);
            this.Controls.Add(this.textBox2);
            this.Controls.Add(this.label23);
            this.Controls.Add(this.textBox1);
            this.Controls.Add(this.label22);
            this.Controls.Add(this.labelFrequenza);
            this.Controls.Add(this.frequenza);
            this.Controls.Add(this.buttonSalvaDati);
            this.Controls.Add(this.label21);
            this.Controls.Add(this.label20);
            this.Controls.Add(this.WarningLabel);
            this.Controls.Add(this.label19);
            this.Controls.Add(this.commandSecLabel);
            this.Controls.Add(this.commandTotalTimeTextBox);
            this.Controls.Add(this.commandTimeLabel);
            this.Controls.Add(this.totalTagsReadLabel);
            this.Controls.Add(this.totalTagsReadTextBox);
            this.Controls.Add(this.inLabel);
            this.Controls.Add(this.secondsLabel);
            this.Controls.Add(this.totalTimeElapsedTextBox);
            this.Controls.Add(this.label18);
            this.Controls.Add(this.allAvailableProtocolLabel);
            this.Controls.Add(this.ipx256Label);
            this.Controls.Add(this.ipx64Label);
            this.Controls.Add(this.isoLabel);
            this.Controls.Add(this.gen2Label);
            this.Controls.Add(this.label10);
            this.Controls.Add(this.label17);
            this.Controls.Add(this.byteCountTextBox);
            this.Controls.Add(this.label16);
            this.Controls.Add(this.readData);
            this.Controls.Add(this.label15);
            this.Controls.Add(this.readTagData);
            this.Controls.Add(this.addrReadData);
            this.Controls.Add(this.label14);
            this.Controls.Add(this.label5);
            this.Controls.Add(this.fakeExit);
            this.Controls.Add(this.readerAddr);
            this.Controls.Add(this.label13);
            this.Controls.Add(this.label12);
            this.Controls.Add(this.label11);
            this.Controls.Add(this.writeTagData);
            this.Controls.Add(this.writeData);
            this.Controls.Add(this.label9);
            this.Controls.Add(this.regioncombo);
            this.Controls.Add(this.writeTagButton);
            this.Controls.Add(this.writeTag);
            this.Controls.Add(this.label8);
            this.Controls.Add(this.label7);
            this.Controls.Add(this.label6);
            this.Controls.Add(this.label3);
            this.Controls.Add(this.stopReads);
            this.Controls.Add(this.startRead);
            this.Controls.Add(this.fontSize);
            this.Controls.Add(this.label4);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.timeout);
            this.Controls.Add(this.readTagID);
            this.Controls.Add(this.readTag);
            this.Controls.Add(this.initReader);
            this.Controls.Add(this.tagIDtoFind);
            this.Controls.Add(this.menuStrip1);
            this.ForeColor = System.Drawing.SystemColors.ControlText;
            this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
            this.MainMenuStrip = this.menuStrip1;
            this.Name = "readWriteDemo";
            this.Text = "ThingMagic M6e Read Write Demo Tool";
            this.Load += new System.EventHandler(this.readWriteDemo_Load);
            this.menuStrip1.ResumeLayout(false);
            this.menuStrip1.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TextBox tagIDtoFind;
        private System.Windows.Forms.Button initReader;
        private System.Windows.Forms.Button readTag;
        private System.Windows.Forms.TextBox timeout;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.Label label2;
        public System.Windows.Forms.TextBox readTagID;      
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.TextBox fontSize;
        private System.Windows.Forms.Button turnOnSearch;
        private System.Windows.Forms.Button startRead;
        private System.Windows.Forms.Button stopReads;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.Label label6;
        private System.Windows.Forms.Label label7;
        private System.Windows.Forms.Label label8;
        private System.Windows.Forms.TextBox writeTag;
        private System.Windows.Forms.Button writeTagButton;
        private System.Windows.Forms.SaveFileDialog saveFileDialog1;
        private System.Windows.Forms.ComboBox regioncombo;
        private System.Windows.Forms.Label label9;
        private System.Windows.Forms.MenuStrip menuStrip1;
        private System.Windows.Forms.ToolStripMenuItem fileToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem saveToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem editToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem copyCtrlCToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem pasteCtrlVToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem helpToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem aboutToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem exitToolStripMenuItem1;
        private System.Windows.Forms.ToolStripMenuItem toolStripMenuItem1;
        private System.Windows.Forms.ToolStripMenuItem bankToReadToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem reservedToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem ePCToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem tIDToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem userToolStripMenuItem;
        private System.Windows.Forms.TextBox writeData;
        private System.Windows.Forms.Button writeTagData;
        private System.Windows.Forms.Label label11;
        private System.Windows.Forms.Label label12;
        private System.Windows.Forms.Label label13;
        private System.Windows.Forms.ToolStripMenuItem disconnectReaderMenuItem;
        private System.Windows.Forms.ToolStripSeparator toolStripMenuItem3;
        private System.Windows.Forms.TextBox readerAddr;
        private System.Windows.Forms.Button fakeExit;
        private System.Windows.Forms.ToolStripMenuItem protocolToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem gEN2ToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem iSO180006BToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem iPX64KHzToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem iPX256KHzToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem readOnAllAvailableProtocolsToolStripMenuItem;
        private System.Windows.Forms.Label label5;
        private System.Windows.Forms.ToolStripMenuItem readOnAntennaNumberToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem antenna1ToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem antenna2ToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem antenna3ToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem antenna4ToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem readOnAllConnectedAntennasToolStripMenuItem;
        private System.Windows.Forms.TextBox addrReadData;
        private System.Windows.Forms.Button readTagData;
        private System.Windows.Forms.Label label14;
        private System.Windows.Forms.Label label15;
        private System.Windows.Forms.TextBox readData;
        private System.Windows.Forms.TextBox byteCountTextBox;
        private System.Windows.Forms.Label label16;
        private System.Windows.Forms.Label label17;
        private System.Windows.Forms.ToolStripMenuItem demoApplicationToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem thingMagicReaderToolStripMenuItem;
        private System.Windows.Forms.Label label10;
        private System.Windows.Forms.Label gen2Label;
        private System.Windows.Forms.Label isoLabel;
        private System.Windows.Forms.Label ipx64Label;
        private System.Windows.Forms.Label ipx256Label;
        private System.Windows.Forms.Label allAvailableProtocolLabel;
        private System.Windows.Forms.Label label18;
        private System.Windows.Forms.TextBox totalTimeElapsedTextBox;
        private System.Windows.Forms.Label inLabel;
        private System.Windows.Forms.TextBox totalTagsReadTextBox;
        private System.Windows.Forms.Label totalTagsReadLabel;
        private System.Windows.Forms.Label commandTimeLabel;
        private System.Windows.Forms.TextBox commandTotalTimeTextBox;
        private System.Windows.Forms.Label secondsLabel;
        private System.Windows.Forms.Label commandSecLabel;
        private System.Windows.Forms.ToolStripMenuItem baudRateToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem baud9600MenuItem;
        private System.Windows.Forms.ToolStripMenuItem baud19200MenuItem;
        private System.Windows.Forms.ToolStripMenuItem baud38400MenuItem;
        private System.Windows.Forms.ToolStripMenuItem baud115200MenuItem;
        private System.Windows.Forms.ToolStripMenuItem baud230400MenuItem;
        private System.Windows.Forms.ToolStripMenuItem baud460800MenuItem;
        private System.Windows.Forms.ToolStripMenuItem baud921600MenuItem;
        private System.Windows.Forms.ToolStripMenuItem rFPowerLevelToolStripMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power15dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power16dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power17dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power18dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power19dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power20dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power21dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power22dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power23dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power24dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power25dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power26dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power27dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power28dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power29dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power30dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power31dBmMenuItem;
        private System.Windows.Forms.ToolStripMenuItem power31p5dBmMenuItem;
        private System.Windows.Forms.Label label19;
        private System.Windows.Forms.ToolStripMenuItem demoToolAssistantToolStripMenuItem;
        private System.Windows.Forms.Label WarningLabel;
        private System.Windows.Forms.Label label20;
        private System.Windows.Forms.Label label21;
        private System.Windows.Forms.ToolStripTextBox toolStripTextBox1;
        private System.Windows.Forms.Button buttonSalvaDati;  //ST
        private System.Windows.Forms.SaveFileDialog saveFileDialogFileTesto;  //ST
        private System.Windows.Forms.Label labelFrequenza;  //ST
        private System.Windows.Forms.TextBox frequenza;  //ST
        private System.Windows.Forms.Label label22;     //EM frequenza2
        private System.Windows.Forms.TextBox textBox1;
        private System.Windows.Forms.TextBox textBox2;
        private System.Windows.Forms.Label label23;
        private System.Windows.Forms.TextBox textBox3;
        private System.Windows.Forms.Label label24;
        private System.Windows.Forms.Label label25;  //altro parametro
    }
}

