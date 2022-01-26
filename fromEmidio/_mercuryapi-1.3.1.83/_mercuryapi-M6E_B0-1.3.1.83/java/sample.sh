#!/bin/sh
#
# Wrapper script to run sample program with the right classpath and library path.
dir=$(dirname $0)
class=$1
shift
java -Djava.library.path=$dir -cp ${dir}/mercuryapi.jar:${dir}/demo.jar samples.${class} "$@"
