#!/usr/bin/env python3
# This module starts the progressive search
# for RFID tags and outputs the best parameters for
# the reader.

from __future__ import print_function
import time
from datetime import datetime
import mercury

def saveSamples(filename, tag, power):
  with open(filename, 'a') as sampleFile:
    listToStr = '{}'.format(power)+','+'{}'.format(tag.epc)+','+'{:1.1f}'.format(tag.antenna)+','+'{:15.20f}'.format(tag.frequency)+','+'{:15.20f}'.format(tag.rssi)+','+'{:15.20f}'.format(tag.phase)+','+'{:15.20f}'.format(tag.timestamp)+'\n'
    sampleFile.write(listToStr)

reader = mercury.Reader("tmr:///dev/ttyUSB0", baudrate=115200)
print(reader.get_model())
print(reader.get_supported_regions())

# Settings for progressive scan
# Frequency settings
changeFrequency = False
freq1 = 870000
freq2 = 868000
nFreq = 12

# Asynchronous timeouts
async_on = 100
async_off = 2000

# Declaring variables for reading power (centi_dBm)
deltaPower = 10
maxPower = 2800
power = 1500

filename = "progressiveTagSearch.dat"
region = "EU3"
protocol = "GEN2"

# Here we set the correct parameters, once the progressive search is done
reader.set_region(region)

if changeFrequency:
  freqList = reader.get_hop_table()

  if (freq1 > freq2) and (freq2 > 0):
    deltaFreq = (freq1 - freq2) / nFreq
    for idx in range(len(freqList)):
      freqList[idx] = freq2 + (idx % (nFreq+1)) * deltaFreq
  elif (freq2 > freq1) and (freq2 > 0):
    deltaFreq = (freq2 - freq1) / nFreq
    for idx in range(len(freqList)):
      freqList[idx] = freq1 + (idx % (nFreq+1)) * deltaFreq
  else:
    if (freq1 >= 840000) and (freq1 <= 960000):
      for idx in range(len(freqList)):
        freqList[idx] = freq1
    else:
      print("Error: freq1 [840-960]MHz")
    
      if (freq2 >= 840000 and freq2 <= 960000) or (freq2 == 0):
        if (freq2 > 0):
          for idx in range(2,12,2):
            freqList[idx] = freq2
      else:
        print("Error: 840MHz < freq2 < 960MHz oppure freq2 = 0 per disabilitarla")

  reader.set_hop_table(freqList)

while power <= maxPower:
  reader.set_read_plan([1], protocol, read_power=power)
  tags = reader.read(async_on)
  for tag in tags:
    saveSamples(filename, tag, power)
  power = power + deltaPower