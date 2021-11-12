#!/bin/bash

if [ $# != 1 ]
 then
	echo Usage: $0 NAME
	exit 1
fi

NAME=$1
DIR=results/$NAME

rm -fr $DIR

N=10000


COMMAND="$EVOMASTER --outputFolder $DIR  --maxFitnessEvaluations $N --writeStatistics=true \
--statisticsFile=$DIR/statistics.csv --appendToStatisticsFile true  \
--statisticsColumnId $NAME"

for i in {1..30}; do
    for alg in "RANDOM" "WTS" "MOSA" "MIO"; do
        $COMMAND --alg $alg --seed $i  --testSuiteFileName EM_${NAME}_${alg}_${i}_Test
    done
done

