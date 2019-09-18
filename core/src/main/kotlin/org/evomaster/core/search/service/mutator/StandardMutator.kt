package org.evomaster.core.search.service.mutator

import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Individual.GeneFilter.ALL
import org.evomaster.core.search.Individual.GeneFilter.NO_SQL
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.ImpactMutationSelection
import org.evomaster.core.search.impact.ImpactUtils

/**
 * make the standard mutator open for extending the mutator,
 *
 * e.g., in order to handle resource rest individual
 */
open class StandardMutator<T> : Mutator<T>() where T : Individual {


    override fun doesStructureMutation(individual : T): Boolean {
        return individual.canMutateStructure() &&
                config.maxTestSize > 1 && // if the maxTestSize is 1, there is no point to do structure mutation
                randomness.nextBoolean(config.structureMutationProbability)
    }

    override fun genesToMutation(individual : T, evi: EvaluatedIndividual<T>) : List<Gene> {
        val filterMutate = if (config.generateSqlDataWithSearch) ALL else NO_SQL
        return individual.seeGenes(filterMutate).filter { it.isMutable() }.filter { !it.reachOptimal() }
    }

    /**
     * Select genes to mutate based on Archive, there are several options:
     *      1. avoid to mutate genes that has less impacts
     *      2. prefer to genes that has more impacts
     *      3. prefer to genes that has recent improvements
     */
    override fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>) : List<Gene>{
        val genesToMutate = genesToMutation(individual, evi)
        if(genesToMutate.isEmpty()) return mutableListOf()

        if(randomness.nextBoolean(config.probOfArchiveMutation))
            return selectGenesByArchive(genesToMutate, individual, evi)
        return selectGenesByDefault(genesToMutate, individual)
    }

    private fun selectGenesByDefault(genesToMutate : List<Gene>,  individual: T) : List<Gene>{
        val filterN = when (config.geneMutationStrategy) {
            ONE_OVER_N -> ALL
            ONE_OVER_N_BIASED_SQL -> NO_SQL
        }
        val n = Math.max(1, individual.seeGenes(filterN).filter { it.isMutable() }.count())
        return selectGenesByOneDivNum(genesToMutate, n)
    }

    private fun selectGenesByOneDivNum(genesToMutate : List<Gene>, n : Int): List<Gene>{
        val genesToSelect = mutableListOf<Gene>()

        val p = 1.0 / n

        var mutated = false

        /*
            no point in returning a copy that is not mutated,
            as we do not use xover
         */
        while (!mutated) { //

            for (gene in genesToMutate) {

                if (!randomness.nextBoolean(p)) {
                    continue
                }

                genesToSelect.add(gene)

                mutated = true
            }
        }
        return genesToSelect
    }

    private fun innerMutate(individual: EvaluatedIndividual<T>, mutatedGene: MutatedGeneSpecification?) : T{

        val individualToMutate = individual.individual

        if(doesStructureMutation(individualToMutate)){
            val copy = (if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual) individualToMutate.next(structureMutator) else individualToMutate.copy()) as T
            structureMutator.mutateStructure(copy, mutatedGene)
            return copy
        }

        val copy = (if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual) individualToMutate.next(this) else individualToMutate.copy()) as T

        val allGenes = copy.seeGenes().flatMap { it.flatView() }

        val selectGeneToMutate = selectGenesToMutate(copy, individual)

        if(selectGeneToMutate.isEmpty())
            return copy

        for (gene in selectGeneToMutate){
            mutatedGene?.mutatedGenes?.add(gene)

            if (randomness.nextBoolean(config.probOfArchiveMutation)){
                gene.archiveMutation(randomness, allGenes, apc, config.geneSelectionMethod, null, ImpactUtils.generateGeneId(copy, gene), individual)
            }else
                gene.standardMutation(randomness, apc, allGenes)
        }

        return copy
    }

    override fun mutate(individual: EvaluatedIndividual<T>, mutatedGenes: MutatedGeneSpecification?): T {

        // First mutate the individual
        val mutatedIndividual = innerMutate(individual, mutatedGenes)

        postActionAfterMutation(mutatedIndividual)

        return mutatedIndividual
    }

    override fun postActionAfterMutation(mutatedIndividual: T) {

        Lazy.assert {
            DbActionUtils.verifyForeignKeys(
                    mutatedIndividual.seeInitializingActions().filterIsInstance<DbAction>())
        }

        // repair the initialization actions (if needed)
        mutatedIndividual.repairInitializationActions(randomness)

        //Check that the repair was successful
        Lazy.assert { mutatedIndividual.verifyInitializationActions() }

    }

    /**
     * Apply archive-based mutation to select genes to mutate
     */
    private fun selectGenesByArchive(genesToMutate : List<Gene>, individual: T, evi: EvaluatedIndividual<T>) : List<Gene>{

        val candidatesMap = genesToMutate.map { it to ImpactUtils.generateGeneId(individual, it) }.toMap()

        val genes = when(config.geneSelectionMethod){
            ImpactMutationSelection.AWAY_BAD -> selectGenesAwayBad(genesToMutate,candidatesMap,evi)
            ImpactMutationSelection.APPROACH_GOOD -> selectGenesApproachGood(genesToMutate,candidatesMap,evi)
            ImpactMutationSelection.FEED_BACK -> selectGenesFeedback(genesToMutate, candidatesMap, evi)
            ImpactMutationSelection.NONE -> {
                selectGenesByOneDivNum(genesToMutate, genesToMutate.size)
            }
        }

        assert(genes.isNotEmpty())
        return listOf(randomness.choose(genes))

    }

    private fun selectGenesAwayBad(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{

        val genes =  genesToMutate.toList().map { g->
            Pair(g, evi.getImpactOfGenes().getValue(candidatesMap.getValue(g)))
        }

        ImpactUtils.selectGenesAwayBad(genes, config.perOfCandidateGenesToMutate).let {
            return selectGenesByOneDivNum(it, it.size)
        }
    }

    private fun selectGenesApproachGood(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{

        val genes =  genesToMutate.toList().map { g->
            Pair(g, evi.getImpactOfGenes().getValue(candidatesMap.getValue(g)))
        }

        ImpactUtils.selectApproachGood(genes, config.perOfCandidateGenesToMutate).let {
            return selectGenesByOneDivNum(it, it.size)
        }
    }


    private fun selectGenesFeedback(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{
        val genes =  genesToMutate.toList().map { g->
            Pair(g, evi.getImpactOfGenes().getValue(candidatesMap.getValue(g)))
        }

        ImpactUtils.selectFeedback(genes, config.perOfCandidateGenesToMutate).let {
            return selectGenesByOneDivNum(it, it.size)
        }
    }

    private fun decideCandidateSize(genesToMutate: List<Gene>) = (genesToMutate.size * config.perOfCandidateGenesToMutate).run {
        if(this < 1.0) 1 else this.toInt()
    }
}