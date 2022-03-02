#!/usr/bin/env python3
from __future__ import print_function
import time
# from datetime import datetime
from threading import Thread
import threading
from random import uniform
import sys
import signal
import mercury

def signal_handler(sig, frame):
  sys.exit(0)

def saveSamples(filename, tag):
  with open(filename, 'a') as sampleFile:
    listToStr = '{}'.format(tag.epc)+','+'{:1.1f}'.format(tag.antenna)+','+'{:15.20f}'.format(tag.frequency)+','+'{:15.20f}'.format(tag.rssi)+','+'{:15.20f}'.format(tag.phase)+','+'{:15.20f}'.format(tag.timestamp)+'\n'
    sampleFile.write(listToStr)

try:
  reader = mercury.Reader("tmr:///dev/ttyUSB0", baudrate=115200)
except:
  print("dev/ttyUSB0 not configured, trying dev/ttyUSB1...")
  reader = mercury.Reader("tmr:///dev/ttyUSB1", baudrate=115200)

print(reader.get_model())
print(reader.get_supported_regions())

power = 2140
freq1 = 866300
freq2 = 866300
nFreq = 12
filename = "samples.dat"
region = "EU3"
protocol = "GEN2"

# Here we set the correct parameters, once the search (progressive or simple) is done
if True:
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

reader.set_region(region)
reader.set_read_plan([1], protocol, read_power=power)
print(reader.read())


signal.signal(signal.SIGINT, signal_handler)
# epc corresponds to the Electronic Product Code, e.g.: b'E2000087071401930700D206'
# phase of the tag response
# antenna indicates where the tag was read, e.g.: 1
# rssi is the strength of the signal recieved from the tag, e.g.: -65
# frequency the tag was read with
# e.g.: reader.start_reading(lambda tag: print(tag.epc, tag.antenna, tag.read_count, tag.rssi, datetime.fromtimestamp(tag.timestamp)))
# reader.start_reading(lambda tag: print(tag.epc, tag.antenna, tag.frequency, tag.rssi, tag.phase, tag.timestamp))
reader.start_reading(lambda tag: saveSamples(filename, tag))
signal.pause()
print("Reading in progress...")
reader.stop_reading()

#
# Beginning stub code
#

# class Tag:
#   epc = ""
#   phase = 0.0
#   antenna = 0
#   frequency = 0.0
#   rssi = 0.0
#   timestamp = None

# def reading_thread(function):
#   tag = Tag()
#   while True:
#     tag.epc = "b'E20000870714019'"
#     tag.antenna = 2
#     tag.phase = 22.3 + uniform(-1.0, 1.0)
#     tag.frequency = 0.2 + uniform(-1.0, 1.0)/10
#     tag.rssi = -62 + uniform(-1.0, 1.0)
#     tag.timestamp = time.time()
#     function(tag)
#     tag.epc = "b'E20000921412168'"
#     tag.phase = 2.1 + uniform(-1.0, 1.0)
#     tag.frequency = 0.1 + uniform(-1.0, 1.0)/10
#     tag.rssi = -52 + uniform(-1.0, 1.0)
#     tag.timestamp = time.time()
#     function(tag)
#     tag.epc = "b'E20000772242290'"
#     tag.phase = 1.0 + uniform(-1.0, 1.0)
#     tag.frequency = 0.5 + uniform(-1.0, 1.0)/10
#     tag.rssi = -88 + uniform(-1.0, 1.0)
#     tag.timestamp = time.time()
#     function(tag)
#     time.sleep(.1)
# 
# def start_reading(function):
#   """Start reading stub.
#   This function stubs the functionality of the reading from mercury
#   sensors.
#   """
#   thread = Thread(target = reading_thread, args = (function, ))
#   thread.setDaemon(True)
#   thread.start()
# 
# def stop_reading():
#   main_thread = threading.current_thread()
#   for t in threading.enumerate():
#     if t is main_thread:
#      continue
#   return
# 
# signal.signal(signal.SIGINT, signal_handler)
# filename = "samples.dat"
# # start_reading(lambda tag: print(tag.epc, tag.antenna, tag.frequency, tag.rssi, tag.phase, tag.timestamp))
# start_reading(lambda tag: saveSamples(filename, tag))
# print("Reading in progress...")
# signal.pause()
# stop_reading()