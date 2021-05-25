#!/bin/bash

SUT_FOLDER=$1
DRIVER_NAME=$2
CONTROLLER_NAME=$3
AT_LEAST_EXPECTED=$4

echo Executing E2E for $SUT_FOLDER

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

java -jar $JAR --seed 42 --maxActionEvaluations 20000  --stoppingCriterion FITNESS_EVALUATIONS --testSuiteSplitType NONE --outputFolder $OUTPUT_FOLDER --testSuiteFileName $TEST_NAME --jsControllerPath $CONTROLLER_LOCATION -sutControllerPort $PORT

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

COVERED=` cat $TEST_LOCATION | grep "Covered targets" | cut -c 20-`

if [ $COVERED -ge $AT_LEAST_EXPECTED ]; then
    echo "Target coverage: $COVERED"
else
    echo "ERROR. Achieved not enough target coverage: $COVERED"
    exit 1
fi