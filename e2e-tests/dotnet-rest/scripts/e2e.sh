#!/bin/bash

echo $(date) Starting to run E2E for C#

# Make sure to kill all sub-processes on exit
trap 'kill $(jobs -p)' EXIT


SCRIPT_FOLDER_LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_FOLDER_LOCATION || exit 1

### White-Box Testing ###
# Note that here we will run the instrumented version of the SUT, i.e
WB="bash ./e2e_wb_runner.sh"
    
$WB /e2e-tests/dotnet-rest/test/RestApis.Tests.HelloWorld  EmbeddedEvoMasterController.cs 100 true HelloWorld 500 Statement_HelloWorldController_00014_13
if [ $? -ne 0 ] ; then
   echo $(date) "ERROR: Test failed for HelloWorld. Exist status " $?
   exit 1
fi

$WB /e2e-tests/dotnet-rest/test/RestApis.Tests.Animals  EmbeddedEvoMasterController.cs 100 true Horse
if [ $? -ne 0 ] ; then
   echo $(date) "ERROR: Test failed for Animals. Exist status " $?
   exit 1
fi

$WB /e2e-tests/dotnet-rest/test/RestApis.Tests.ForAssertions  EmbeddedEvoMasterController.cs 100 false 42 hello 1000 2000 3000 66 bar xvalue yvalue true false simple-string simple-text 123 456 777 888
if [ $? -ne 0 ] ; then
   echo $(date) "ERROR: Test failed for ForAssertions. Exist status " $?
   exit 1
fi

$WB /e2e-tests/dotnet-rest/test/RestApis.Tests.Crud  EmbeddedEvoMasterController.cs  100 false FOO CREATED UPDATED PATCHED DELETED
if [ $? -ne 0 ] ; then
   echo $(date) "ERROR: Test failed for Crud."
   exit 1
fi

$WB /e2e-tests/dotnet-rest/test/RestApis.Tests.StringEquals  EmbeddedEvoMasterController.cs 500 false CONSTANT_OK CONSTANT_FAIL
if [ $? -ne 0 ] ; then
   echo $(date) "ERROR: Test failed for StringEquals."
   exit 1
fi


### Black-Box Testing ###
# Note that here we will run the original, NON-instrumented version of the SUT
#BB="bash ./e2e_bb_runner.sh"


echo $(date) All E2E are succesfully finished