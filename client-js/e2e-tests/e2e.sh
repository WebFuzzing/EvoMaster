#!/bin/bash

# Make sure to kill all sub-processes on exit
trap 'kill $(jobs -p)' EXIT


SCRIPT_FOLDER_LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_FOLDER_LOCATION || exit 1

RUN="bash ./e2e_runner.sh"

$RUN /client-js/integration-tests/build/src/books-api            em-main.js   app-driver.js  50
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for books-api. Exist status " $?
   exit 1
fi

$RUN /client-js/integration-tests/build/src/for-assertions-api   em-main.js   app-driver.js  50  42 hello 1000 2000 3000 66 bar xvalue yvalue true false simple-string simple-text 123 456 777 888 
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for for-assertions-api. Exist status " $?
   exit 1
fi

