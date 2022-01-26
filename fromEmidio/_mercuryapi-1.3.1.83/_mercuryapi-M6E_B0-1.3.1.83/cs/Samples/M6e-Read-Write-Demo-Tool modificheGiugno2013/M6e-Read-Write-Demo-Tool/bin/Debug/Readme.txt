This is a simple demonstration of the ThingMagic Mercury 6e Reader
with respect to reading EPC's and reading and writing data for 
various protocols using the C# desktop application. The application
uses the Mercury API built for the .net framework. 

Note: this software is specifically designed for the M6e reader and 
may or may not support other ThingMagic products.

To run, double-click on the M6e-Read-Write-Demo-Tool.exe file, or on the 
command line, run M6e-Read-Write-Demo-Tool.exe after navigating to the 
containing folder.

The sources for this program can be found in the Mercury API package
under cs/Samples/M6e-Read-Write-Demo-Tool. The application was built with
Microsoft Visual C# 2008 Express Edition. 

Initialize:

Once the program is up and running, enter the reader COM port (COMx 
or comx), click the "Initialize Reader" button and wait for the button
to turn Green and be disabled. This indicates the Reader is ready for 
RF operations. By default the Reader is configured to read GEN2 tags
on Antenna 1. Be sure to have an antenna connected to Port 1 before 
performing RF operations or change the antenna port to another port
connected to an antenna (read below). 

Read Once:

Click on the "Read Once" button to read EPC once per click. To change
the reader configuration to read other protocols click on Option/Protocol... 
and select one or more of the other protocols supported by the M6e. Selecting
Read on all available protocols will enable the reader to search for all 
protocols supported by the M6e. The protocol currently under search will 
turn green on the software window for indication. The protocol of the 
tag read will be seen under Protocol in the Query Section.

To change the reader configuration to read on other antennas click on 
Option/Read on... and select an Antenna #. Make sure to have an antenna 
connected to the port you are trying to read before the RF operation. 
Selecting Read on all connected antennas will enable the reader to detect 
antennas on the various ports and perform reads on those ports. 
The port number reading a particular tag will be seen under Antenna # 
in the Query Section.

Start/Stop Reads:

Click on the "Start Reads" button to start asynchronous reads on the 
connected reader. The button will promptly change to "Stop Reads" 
and must be clicked to stop the asynchronous reads. To change the 
reader configuration to a different antenna or protocol follow the same 
procedure as mentioned in the Read Once section.

Clear:

Click on the "Clear" button to clear the Read Tag ID screen at any time. 

The Total Tags Read will be enabled only in case of Read Once as the 
Start Reads initiates an asynchronous reads, streaming tags as they are
seen.

Write GEN2 EPC (Single):

A write EPC operation can be performed on a single tag by entering the 
EPC in the "Write EPC" field and clicking on the "Write GEN2 EPC (Single)"
button. Only a single protocol and antenna must be selected to perform
this operation and they can be selected under Option/Protocol... and Option/
Read on... respectively.
Note:
1. Selection of a tag is not supported for writing an EPC.
2. Write EPC is not supported for protocols other than GEN2. Use "Write Data
(Single)" for other protocols, if writing EPC is supported.

Read Data (Single):

Click on the "Read Data (single)" button to read data from the tag in the 
field. The "Select EPC" field can be used to provide a selection criteria 
on the tag to be operated on. The "Byte Address" field specifies the 
address in bytes for the memory space that is to be read. The "Byte Count"
field specifies the number of bytes to be read in this operation. For GEN2
tags, the default memory bank read is the Kill Password. To change the 
memory bank to read click on Option/Bank to Read/Write... and select 
the desired memory bank. Only a single protocol and antenna must be selected 
to perform this operation and they can be selected under Option/Protocol... 
and Option/Read on... respectively.
Note: The Selection criteria must be the Tag EPC and must be specified for 
ISO18K-6B tags. For Gen2, if the selection criteria is not mentioned, the 
reader will read data from the first seen tag.

Write Data (Single):

A write Data operation can be performed on a single tag by entering 
valid number of data bytes in the "Write Data" field and clicking on 
the "Write Data (Single)" button. The "Select EPC" field can be used 
to provide a selection criteria on the tag to be operated on. 
The "Byte Address" field specifies the address in bytes for the memory 
space that is to be written to. For GEN2 tags, the default memory 
bank written is the Kill Password. To change the memory bank to write to 
click on Option/Bank to Read/Write... and select the desired memory bank. 
Only a single protocol and antenna must be selected to perform this 
operation and they can be selected under Option/Protocol... and 
Option/Read on... respectively.
Note: The Selection criteria must be the Tag EPC and must be specified for 
ISO18K-6B tags. For Gen2, if the selection criteria is not mentioned, the 
reader will write data to the first seen tag.

Other useful reader configurations can be found under Option and are enabled
only when the reader connection is established. 

For more information, visit http://www.thingmagic.com/, or contact 
support@thingmagic.com




 





