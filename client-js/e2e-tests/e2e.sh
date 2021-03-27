#!/bin/bash

# Make sure to kill all sub-processes on exit
trap 'kill $(jobs -p)' EXIT


SCRIPT_FOLDER_LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_FOLDER_LOCATION || exit 1

RUN="bash ./e2e_runner.sh"

$RUN /client-js/integration-tests/build/src/books-api   em-main.js   app-driver.js  50
