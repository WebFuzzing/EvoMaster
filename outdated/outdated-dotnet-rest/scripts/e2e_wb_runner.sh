#!/bin/bash

# where the SUT is located
SUT_FOLDER=$1
# the name of EM Driver, which we are going to start in a new process, before running the process of EM.
# However, as it is started with Dotnet command, this value is used only for validation (eg checking that we
# are using the right folder)
DRIVER_NAME=$2

# How many fitness evaluations for the search
BUDGET=$3

# whether to export covered targets
EXPORT_CTARGETS=$4

NPARAMS=4

echo $(date) Executing White-Box E2E for $SUT_FOLDER

# Make sure to kill all sub-processes on exit
trap 'kill $(jobs -p)' EXIT

# Current folder of the script
SCRIPT_FOLDER_LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

PROJECT_ROOT=$SCRIPT_FOLDER_LOCATION/../../..

JAR=$PROJECT_ROOT/core/target/evomaster.jar

if [ -f "$JAR" ]; then
    echo "Located EM jar file at: $JAR"
else
    echo "ERROR. JAR file of EM not found at: $JAR"
    exit 1
fi

DRIVER=$PROJECT_ROOT$SUT_FOLDER/$DRIVER_NAME


if [ -f "$DRIVER" ]; then
    echo "Located Driver file at: $DRIVER"
else
    echo "ERROR. Driver file not found at: $DRIVER"
    exit 1
fi

TEST_NAME="EvoMasterE2ETest"
OUTPUT_FOLDER=$PROJECT_ROOT$SUT_FOLDER/generated
TEST_LOCATION=$OUTPUT_FOLDER/$TEST_NAME.cs

TARGET_FILE="coveredTargets.txt"
TARGET_LOCATION=$OUTPUT_FOLDER/$TARGET_FILE

# Deleting previously generated tests, if any
rm -f $OUTPUT_FOLDER/*Test.cs
rm -f $TARGET_LOCATION
mkdir -p $OUTPUT_FOLDER


#  Bit tricky... it has happened sometimes that 40100 gives issues on CI...
#  Ideally should get an ephemeral port, but hard to extract it from NodeJS (eg, could
#  print it on console, and then read it back here).
#  As workaround, we can use a random port, "hoping" it is available (with should be 99.99% of
#  the times)
PORT=$((20000 + $RANDOM % 40000))
echo Using Controller Port $PORT


# Starting Driver in the background
echo $(date) Starting Driver
cd $PROJECT_ROOT$SUT_FOLDER || exit 1
dotnet build
dotnet run $PORT &
PID=$!

# give enough time to start
sleep 60

echo $(date) Starting EvoMaster
java -jar $JAR --seed 42 --maxActionEvaluations $BUDGET  --stoppingCriterion FITNESS_EVALUATIONS --testSuiteSplitType NONE --outputFolder $OUTPUT_FOLDER --testSuiteFileName $TEST_NAME  --sutControllerPort $PORT --coveredTargetFile $TARGET_LOCATION --exportCoveredTarget=$EXPORT_CTARGETS

# stop driver, which was run in background
kill $PID

if [ -f "$TEST_LOCATION" ]; then
    echo $(date) "Test suite correctly generated at: $TEST_LOCATION"
else
    echo $(date) "ERROR. Failed to locate generated tests at: $TEST_LOCATION"
    exit 1
fi


# run the tests
echo $(date) Running the generated tests
dotnet test --filter $TEST_NAME

if [ $? -ne 0 ] ; then
   echo "ERROR: failed to run the generated tests."
   echo "The generated test was:"
   cat $TEST_LOCATION
   exit 1
fi


# check for text in file
echo $(date) Validating the generated tests
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


doExport=${EXPORT_CTARGETS^^}

echo "whether to export covered targets: $doExport"
if [ "$doExport" = "TRUE" ]; then
    if [ -f "$TARGET_LOCATION" ]; then
        echo $(date) "Covered targets correctly generated at: $TARGET_LOCATION"
        FOUND=`cat $TARGET_LOCATION | grep "Line_at_" | wc -l`
        if [ $FOUND -eq 0 ]; then
          echo "ERROR. Not found covered line targets"
          exit 1
        fi
    else
        echo $(date) "ERROR. Failed to locate covered targets at: $TARGET_LOCATION"
        exit 1
    fi
fi;

echo $(date) All checks have successfuly completed for this test


