#!/bin/bash

# where the SUT is located
SUT_FOLDER=$1
# the name of EM Driver, which we are going to start in a new process, before running the process of EM
DRIVER_NAME=$2
# the name of the actual SUT Controller class. only needed in JS to setup the needed --jsControllerPath option
CONTROLLER_NAME=$3
# fitness evaluation budget
FITNESS_EVALUATIONS_BUDGET=$4
# check minimum number of targets that should had been covered
AT_LEAST_EXPECTED=$5
NPARAMS=5

echo Executing White-Box E2E for $SUT_FOLDER

# Make sure to kill all sub-processes on exit
trap 'kill $(jobs -p)' EXIT

# Current folder of the script
SCRIPT_FOLDER_LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

PROJECT_ROOT=$SCRIPT_FOLDER_LOCATION/../..

JAR=$PROJECT_ROOT/core/target/evomaster.jar

if [ -f "$JAR" ]; then
    echo "Located EM jar file at: $JAR"
else
    echo "ERROR. JAR file of EM not found at: $JAR"
    exit 1
fi

DRIVER=$PROJECT_ROOT$SUT_FOLDER/$DRIVER_NAME
CONTROLLER_LOCATION=$PROJECT_ROOT$SUT_FOLDER/$CONTROLLER_NAME


if [ -f "$DRIVER" ]; then
    echo "Located Driver file at: $DRIVER"
else
    echo "ERROR. Driver file not found at: $DRIVER"
    exit 1
fi

TEST_NAME="evomaster-e2e-test"
OUTPUT_FOLDER=$SCRIPT_FOLDER_LOCATION/generated
TEST_LOCATION=$OUTPUT_FOLDER/$TEST_NAME.js

# Deleting previously generated tests, if any
rm -f $OUTPUT_FOLDER/*-test.js
mkdir -p $OUTPUT_FOLDER


#  Bit tricky... it has happened sometimes that 40100 gives issues on CI...
#  Ideally should get an ephemeral port, but hard to extract it from NodeJS (eg, could
#  print it on console, and then read it back here).
#  As workaround, we can use a random port, "hoping" it is available (with should be 99.99% of
#  the times)
PORT=$((20000 + $RANDOM % 40000))

echo Using Controller Port $PORT

# Starting  NodeJS Driver in the background
PORT=$PORT node $DRIVER &
PID=$!

# give enough time to start
sleep 10

java -jar $JAR --seed 42 --maxActionEvaluations $FITNESS_EVALUATIONS_BUDGET  --stoppingCriterion FITNESS_EVALUATIONS --testSuiteSplitType NONE --outputFolder $OUTPUT_FOLDER --testSuiteFileName $TEST_NAME --jsControllerPath $CONTROLLER_LOCATION --sutControllerPort $PORT

# stop driver, which was run in background
kill $PID

if [ -f "$TEST_LOCATION" ]; then
    echo "Test suite correctly generated at: $TEST_LOCATION"
else
    echo "ERROR. Failed to locate generated tests at: $TEST_LOCATION"
    exit 1
fi

# run the tests
cd $SCRIPT_FOLDER_LOCATION || exit 1
npm i
npm run test

if [ $? -ne 0 ] ; then
   echo "ERROR: failed to run the generated tests."
   exit 1
fi

COVERED=` cat $TEST_LOCATION | grep "Covered targets" | cut -c 20-`

if [ $COVERED -ge $AT_LEAST_EXPECTED ]; then
    echo "Target coverage: $COVERED"
else
    echo "ERROR. Achieved not enough target coverage: $COVERED"
    exit 1
fi

# check for text in file
N=$#

if [ $N -gt $NPARAMS ]; then
  Z=("$@")
  A=${Z[@]:$NPARAMS}

  for K in $A; do
    echo "Checking for text $K"
    FOUND=`cat $TEST_LOCATION | grep "$K" | wc -l`
    if [ $FOUND -eq 0 ]; then
      echo "ERROR. Not found text: $K"
      exit 1
    fi
  done
fi

echo All checks have successfuly completed for this test


