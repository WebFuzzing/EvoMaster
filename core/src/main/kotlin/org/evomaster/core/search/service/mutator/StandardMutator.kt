package org.evomaster.core.search.service.mutator

import org.evomaster.core.EMConfig
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.GeneIdUtil
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Individual.GeneFilter.ALL
import org.evomaster.core.search.Individual.GeneFilter.NO_SQL
import org.evomaster.core.search.gene.*

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
        return individual.seeGenes(filterMutate).filter { it.isMutable() }
    }

    /**
     * Select genes to mutate based on Archive, there are several options:
     *      1. remove bad genes
     *      2. select good genes,
     *      3. recent good genes, similar with feed-back sampling
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
            /*
             TODO
             NOTE THAT gene.archiveMutation(...) is required to extend for archive-based mutation
             */
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

        val candidatesMap = genesToMutate.map { it to GeneIdUtil.generateGeneId(individual, it) }.toMap()

        val genes = when(config.geneSelectionMethod){
            EMConfig.ArchiveGeneSelectionMethod.AWAY_BAD -> selectGenesAwayBad(genesToMutate,candidatesMap,evi)
            EMConfig.ArchiveGeneSelectionMethod.APPROACH_GOOD -> selectGenesApproachGood(genesToMutate,candidatesMap,evi)
            EMConfig.ArchiveGeneSelectionMethod.FEED_BACK -> selectGenesFeedback(genesToMutate, candidatesMap, evi)
            EMConfig.ArchiveGeneSelectionMethod.NONE -> {
                emptyList()
            }
        }

        if (genes.isEmpty())
            return selectGenesByOneDivNum(genesToMutate, genesToMutate.size)

        return genes

    }

    private fun selectGenesAwayBad(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{
        //remove genes from candidate that has "bad" history with 90%, i.e., timesOfNoImpacts is not 0
        val genes =  genesToMutate.filter { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.timesOfNoImpacts?.let {
                it == 0 || (it > 0 && randomness.nextBoolean(0.1))
            }?:false
        }
        if(genes.isNotEmpty())
            return selectGenesByOneDivNum(genes, genes.size)
        else
            return emptyList()
    }

    private fun selectGenesApproachGood(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{

        val sortedByCounter = genesToMutate.toList().sortedBy { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.timesOfImpact
        }

        selectGenesWithSorted(genesToMutate, sortedByCounter).apply {
            return selectGenesByOneDivNum(this, size)
        }

    }

    private fun selectGenesWithSorted(genesToMutate: List<Gene>, sortedGeneCounter: List<Gene>) : List<Gene>{
        val size = (genesToMutate.size * config.perOfCandidateGenesToMutate).let {
            if(it > 1.0) it.toInt() else 1
        }

        return genesToMutate.filter { sortedGeneCounter.subList(0, size).contains(it) }
    }

    private fun selectGenesFeedback(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{
        val notVisited =  genesToMutate.filter { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.let {
                it.timesToManipulate == 0
            }?:false
        }
        if(notVisited.isNotEmpty())
            return selectGenesByOneDivNum(notVisited, notVisited.size)

        val zero = genesToMutate.filter { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.let {
                it.counter == 0 && it.timesToManipulate > 0
            }?:false
        }

        /*
            TODO: shall we control the size in case of a large size of zero?
         */
        if(zero.isNotEmpty()){
            return zero
        }

        val sortedByCounter = genesToMutate.toList().sortedByDescending { g->
            evi.impactsOfGenes[candidatesMap.getValue(g)]?.counter
        }

        selectGenesWithSorted(genesToMutate, sortedByCounter).apply {
            return selectGenesByOneDivNum(this, size)
        }
    }
}