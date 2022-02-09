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

# reader = mercury.Reader("tmr:///dev/ttyUSB0", baudrate=115200)
# print(reader.get_model())
# print(reader.get_supported_regions())

# # Here we set the correct parameters, once the search (progressive or simple) is done
# reader.set_region("EU3")
# reader.set_read_plan([1], "GEN2", read_power=1900)
# print(reader.read())

# filename = "samples.dat"
# signal.signal(signal.SIGINT, signal_handler)
# # epc corresponds to the Electronic Product Code, e.g.: b'E2000087071401930700D206'
# # phase of the tag response
# # antenna indicates where the tag was read, e.g.: 1
# # rssi is the strength of the signal recieved from the tag, e.g.: -65
# # frequency the tag was read with
# # e.g.: reader.start_reading(lambda tag: print(tag.epc, tag.antenna, tag.read_count, tag.rssi, datetime.fromtimestamp(tag.timestamp)))
# # reader.start_reading(lambda tag: print(tag.epc, tag.antenna, tag.frequency, tag.rssi, tag.phase, tag.timestamp))
# reader.start_reading(lambda tag: saveSamples(filename, tag))
# signal.pause()
# print("Reading in progress...")
# reader.stop_reading()

#
# Beginning stub code
#

class Tag:
  epc = ""
  phase = 0.0
  antenna = 0
  frequency = 0.0
  rssi = 0.0
  timestamp = None

def reading_thread(function):
  tag = Tag()
  while True:
    tag.epc = "b'E20000870714019'"
    tag.antenna = 2
    tag.phase = 22.3 + uniform(-1.0, 1.0)
    tag.frequency = 0.2 + uniform(-1.0, 1.0)/10
    tag.rssi = -62 + uniform(-1.0, 1.0)
    tag.timestamp = time.time()
    function(tag)
    tag.epc = "b'E20000921412168'"
    tag.phase = 2.1 + uniform(-1.0, 1.0)
    tag.frequency = 0.1 + uniform(-1.0, 1.0)/10
    tag.rssi = -52 + uniform(-1.0, 1.0)
    tag.timestamp = time.time()
    function(tag)
    tag.epc = "b'E20000772242290'"
    tag.phase = 1.0 + uniform(-1.0, 1.0)
    tag.frequency = 0.5 + uniform(-1.0, 1.0)/10
    tag.rssi = -88 + uniform(-1.0, 1.0)
    tag.timestamp = time.time()
    function(tag)
    time.sleep(.1)

def start_reading(function):
  """Start reading stub.
  This function stubs the functionality of the reading from mercury
  sensors.
  """
  thread = Thread(target = reading_thread, args = (function, ))
  thread.setDaemon(True)
  thread.start()

def stop_reading():
  main_thread = threading.current_thread()
  for t in threading.enumerate():
    if t is main_thread:
     continue
  return

signal.signal(signal.SIGINT, signal_handler)
filename = "samples.dat"
# start_reading(lambda tag: print(tag.epc, tag.antenna, tag.frequency, tag.rssi, tag.phase, tag.timestamp))
start_reading(lambda tag: saveSamples(filename, tag))
print("Reading in progress...")
signal.pause()
stop_reading()