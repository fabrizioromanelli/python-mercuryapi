#!/usr/bin/env python3
# This module starts the search (simple/progressive)
# for RFID tags and outputs the best parameters for 
# the reader.

from __future__ import print_function
import time
from datetime import datetime
import mercury
reader = mercury.Reader("tmr:///dev/ttyUSB0", baudrate=115200)

print(reader.get_model())
print(reader.get_supported_regions())

# Here we set the correct parameters, once the search (progressive or simple is done)
reader.set_region("EU3")
reader.set_read_plan([1], "GEN2", read_power=1900)
print(reader.read())

reader.start_reading(lambda tag: print(tag.epc, tag.antenna, tag.read_count, tag.rssi, datetime.fromtimestamp(tag.timestamp)))
time.sleep(1)
reader.stop_reading()