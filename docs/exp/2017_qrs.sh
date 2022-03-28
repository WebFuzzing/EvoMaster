#!/bin/bash

if [ $# != 1 ]
 then
	echo Usage: $0 NAME
	exit 1
fi

NAME=$1
DIR=results/$NAME

rm -fr $DIR

N=100000
#ALG=MIO
ALG=WTS

COMMAND="$EVOMASTER --outputFolder $DIR  --maxFitnessEvaluations $N --writeStatistics=true \
--statisticsFile=$DIR/statistics.csv --appendToStatisticsFile true --alg $ALG \
--statisticsColumnId $NAME"

for i in {1..30}; do
    $COMMAND --seed $i  --testSuiteFileName EM_${NAME}_${i}_Test
done

