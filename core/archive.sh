#! /bin/bash

cd target

case $1 in
	1)
	  echo "APPROACH_GOOD 0.5"
		java -jar evomaster.jar --stoppingCriterion FITNESS_EVALUATIONS --heuristicsForSQL false --generateSqlDataWithSearch false --enableTrackIndividual false --enableTrackEvaluatedIndividual true --probOfArchiveMutation 0.5 --geneSelectionMethod APPROACH_IMPACT --maxActionEvaluations 100000
		;;
  2)
	  echo "AWAY_BAD 0.5"
		java -jar evomaster.jar --stoppingCriterion FITNESS_EVALUATIONS --heuristicsForSQL false --generateSqlDataWithSearch false --enableTrackIndividual false --enableTrackEvaluatedIndividual true --probOfArchiveMutation 0.5 --geneSelectionMethod AWAY_NOIMPACT --maxActionEvaluations 100000
		;;
  3)
	  echo "FEED_BACK 0.5"
		java -jar evomaster.jar --stoppingCriterion FITNESS_EVALUATIONS --heuristicsForSQL false --generateSqlDataWithSearch false --enableTrackIndividual false --enableTrackEvaluatedIndividual true --probOfArchiveMutation 0.5 --geneSelectionMethod FEEDBACK_DIRECT --maxActionEvaluations 100000
		;;
  4)
	  echo "adaptive random 0.5"
		java -jar evomaster.jar --stoppingCriterion FITNESS_EVALUATIONS --heuristicsForSQL false --generateSqlDataWithSearch false --enableTrackIndividual false --enableTrackEvaluatedIndividual true --probOfArchiveMutation 0.5 --adaptiveGeneSelection RANDOM --maxActionEvaluations 100000
		;;
  5)
	  echo "adaptive guided enableArchiveGeneMutation 0.5"
		java -jar evomaster.jar --stoppingCriterion FITNESS_EVALUATIONS --heuristicsForSQL false --generateSqlDataWithSearch false --enableTrackIndividual false --enableTrackEvaluatedIndividual true --probOfArchiveMutation 0.75 --enableArchiveGeneMutation true --adaptiveGeneSelection GUIDED --maxActionEvaluations 100000
		;;
  *)
    exit 1
		;;
esac
