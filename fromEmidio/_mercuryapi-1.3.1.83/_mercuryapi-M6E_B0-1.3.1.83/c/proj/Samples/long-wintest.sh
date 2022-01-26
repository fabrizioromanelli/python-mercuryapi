#!/bin/sh
# Endless loop on Windows test scripts

export URI=${URI:-tmr:///COM1}
export program=${program:-demo-Debug/demo.exe}

while [ 1 ]; do
  echo Iteration: `date`
  tests/runtests.sh
done
