#!/bin/bash

# Make sure to kill all sub-processes on exit
trap 'kill $(jobs -p)' EXIT


SCRIPT_FOLDER_LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_FOLDER_LOCATION || exit 1

### White-Box Testing ###
# Note that here we will run the instrumented version of the SUT, i.e, under /build
WB="bash ./e2e_wb_runner.sh"

$WB /client-js/integration-tests/build/src/books-api            em-main.js   app-driver.js 20000 50
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for books-api."
   exit 1
fi

$WB /client-js/integration-tests/build/src/for-assertions-api   em-main.js   app-driver.js 20000 50  42 hello 1000 2000 3000 66 bar xvalue yvalue true false simple-string simple-text 123 456 777 888
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for for-assertions-api."
   exit 1
fi

$WB /client-js/integration-tests/build/src/base-graphql   em-main.js   app-driver.js 20000 1 FOO
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for base-graphql."
   exit 1
fi

$WB /client-js/integration-tests/build/src/taint-string   em-main.js   app-driver.js 20000 1 OK_hello OK_foo
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for taint-string."
   exit 1
fi

$WB  /client-js/integration-tests/build/src/login-token   em-main.js   app-driver.js 20000 1 OK_check
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for login-token"
   exit 1
fi

$WB  /client-js/integration-tests/build/src/taint-squareBrackets   em-main.js   app-driver.js 20000 1 OK_FOUND_squareBrackets FAILED_squareBrackets OK_FOUND_array FAILED_array
if [ $? -ne 0 ] ; then
   echo "ERROR: Test failed for taint-squareBrackets"
   exit 1
fi


### Black-Box Testing ###
# Note that here we will run the original, NON-instrumented version of the SUT, i.e, under /src
BB="bash ./e2e_bb_runner.sh"


$BB /client-js/integration-tests/src/for-assertions-api  main.js  REST  42  hello 1000 2000 3000 66 bar xvalue yvalue true false simple-string simple-text 123 456 777 888
if [ $? -ne 0 ] ; then
   echo "ERROR: BB Test failed for for-assertions-api."
   exit 1
fi

$BB /client-js/integration-tests/src/status-api  main.js  REST ".status).toBe(200)"  ".status).toBe(400)"  ".status).toBe(404)"  ".status).toBe(500)"
if [ $? -ne 0 ] ; then
   echo "ERROR: BB Test failed for status-api."
   exit 1
fi

$BB /client-js/integration-tests/src/base-graphql  main.js GRAPHQL FOO
if [ $? -ne 0 ] ; then
   echo "ERROR: BB Test failed for base-graphql."
   exit 1
fi
