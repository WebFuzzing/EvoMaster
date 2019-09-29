#! /bin/bash

cd target

case $1 in
	1)
	  echo "adaptive guided enableArchiveGeneMutation 0.5"
		java -jar evomaster.jar --writeStatistics=true --statisticsColumnId=catwatch --seed=1 --enableTrackIndividual=false --enableTrackEvaluatedIndividual=true --probOfArchiveMutation=0.5 --geneSelectionMethod=AWAY_NOIMPACT --maxActionEvaluations=100000 --stoppingCriterion=FITNESS_EVALUATIONS
		;;
  *)
    exit 1
		;;
esac
