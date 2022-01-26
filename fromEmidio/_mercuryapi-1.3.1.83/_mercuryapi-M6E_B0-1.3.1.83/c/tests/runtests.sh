#!/bin/sh

URI=${URI:-tmr:///dev/ttyUSB0}
testdir=$(dirname $0)
program=${program:-./demo}

if [ $# -gt 0 ]
then
    tests="$@"
else
    tests=${testdir}/*.script
fi

passed=0
failed=0
faillist=""


for testname in ${tests}
do
    printf "Running ${testname}\n"
    keyname=${testname%.script}.key
    outname=${testname%.script}.output
    ${program} ${URI} < ${testname} > ${outname}
    dos2unix ${outname}
    if [ $? -ne 0 ]
    then
	printf "Program execution failed running test ${testname}.\n"
        failed=$((${failed} + 1))
	faillist="${faillist} ${testname}"
	continue
    fi

    if [ ! -f ${keyname} ]
    then
	printf "No key file for ${testname}\n"
    else
        ${testdir}/regmatch.py ${outname} ${keyname} 
        if [ $? -ne 0 ]
        then
	    printf "Test ${testname} failed\n"
    	    diff -u ${keyname}.clean ${outname}
            failed=$((${failed} + 1))
	    faillist="${faillist} ${testname}"
        else
	    passed=$((${passed} + 1))
        fi
    fi
done

printf "${passed} tests passed\n"
printf "${failed} tests failed\n"
if [ ${failed} -gt 0 ]
then
    printf "Failed tests: ${faillist}\n"
    exit 1
fi
