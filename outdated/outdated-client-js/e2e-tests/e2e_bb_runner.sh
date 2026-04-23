#!/bin/bash

# WARNING: BB tests have to be simple, eg, basic static data, as they do not reset the SUT...
# Even if we were to disable assertions, still we could end up with different status codes...

# where the SUT is located
SUT_FOLDER=$1
# the main entry point for the SUT. it assumes it reads the environment variable PORT
SUT_MAIN=$2
# the problem type, eg REST and GRAPHQL
TYPE=$3

NPARAMS=3

echo Executing Black-Box E2E for $SUT_FOLDER

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


MAIN_LOCATION=$PROJECT_ROOT$SUT_FOLDER/$SUT_MAIN


if [ -f "$MAIN_LOCATION" ]; then
    echo "Located Main SUT entry file at: $MAIN_LOCATION"
else
    echo "ERROR. Main SUT entry not found at: $MAIN_LOCATION"
    exit 1
fi

TEST_NAME="evomaster-e2e-test"
OUTPUT_FOLDER=$SCRIPT_FOLDER_LOCATION/generated
TEST_LOCATION=$OUTPUT_FOLDER/$TEST_NAME.js

# Deleting previously generated tests, if any
rm -f $OUTPUT_FOLDER/*-test.js
mkdir -p $OUTPUT_FOLDER


#  Bit tricky... it has happened sometimes that hardcoded ports like 40100 and 8080 give issues on CI...
#  Ideally should get an ephemeral port, but hard to extract it from NodeJS (eg, could
#  print it on console, and then read it back here).
#  As workaround, we can use a random port, "hoping" it is available (with should be 99.99% of
#  the times)
PORT=$((20000 + $RANDOM % 40000))

echo Using SUT Port $PORT

# Starting  NodeJS Driver in the background
PORT=$PORT node $MAIN_LOCATION &
PID=$!


if [ "$TYPE" == "REST" ]; then
  PROBLEM=" --problemType REST --bbSwaggerUrl http://localhost:$PORT/swagger.json --bbTargetUrl http://localhost:$PORT"
elif [ "$TYPE" == "GRAPHQL" ]; then
  PROBLEM=" --problemType GRAPHQL --bbTargetUrl http://localhost:$PORT/graphql "
else
  echo "ERROR. Invalid problem type: $TYPE"
  exit 1
fi

# give enough time to start
sleep 10

java -jar $JAR --seed 42 --maxActionEvaluations 1000  --stoppingCriterion FITNESS_EVALUATIONS \
       --testSuiteSplitType NONE --outputFolder $OUTPUT_FOLDER --testSuiteFileName $TEST_NAME \
       --blackBox true $PROBLEM --outputFormat JS_JEST


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
   kill $PID
   exit 1
fi


# stop SUT, which was run in background, but only AFTER we run the generated tests... as those do not
# start the SUT by themselves in BB.
kill $PID


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


