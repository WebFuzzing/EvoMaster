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

        val candidatesMap = genesToMutate.map { it to ImpactUtils.generateGeneId(individual, it) }.toMap()

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
        //remove last 10%
        val removedSize = decideCandidateSize(genesToMutate)

        val genes =  genesToMutate.toList().sortedBy { g->
            evi.getImpactOfGenes()[candidatesMap.getValue(g)]?.timesOfNoImpacts
        }.subList(0, genesToMutate.size - removedSize)

        return if(genes.isNotEmpty())
            selectGenesByOneDivNum(genes, genes.size)
        else
            emptyList()
    }

    private fun selectGenesApproachGood(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{

        val size = decideCandidateSize(genesToMutate)

        val genes = genesToMutate.toList().sortedByDescending { g->
            evi.getImpactOfGenes()[candidatesMap.getValue(g)]?.timesOfImpact
        }.subList(0, size)

        return if(genes.isNotEmpty())
            selectGenesByOneDivNum(genes, genes.size)
        else
            emptyList()

    }


    private fun selectGenesFeedback(genesToMutate: List<Gene>, candidatesMap : Map<Gene, String>, evi: EvaluatedIndividual<T>): List<Gene>{
        val notVisited =  genesToMutate.filter { g->
            evi.getImpactOfGenes()[candidatesMap.getValue(g)]?.let {
                it.timesToManipulate == 0
            }?:false
        }
        if(notVisited.isNotEmpty())
            return selectGenesByOneDivNum(notVisited, notVisited.size)

        val zero = genesToMutate.filter { g->
            evi.getImpactOfGenes()[candidatesMap.getValue(g)]?.let {
                it.counter == 0 && it.timesToManipulate > 0
            }?:false
        }

        /*
            TODO: shall we control the size in case of a large size of zero?
         */
        if(zero.isNotEmpty()){
            return zero
        }

        val size = decideCandidateSize(genesToMutate)

        val genes = genesToMutate.toList().sortedBy { g->
            evi.getImpactOfGenes()[candidatesMap.getValue(g)]?.counter
        }.subList(0, size)

        return if(genes.isNotEmpty())
            selectGenesByOneDivNum(genes, genes.size)
        else
            emptyList()
    }

    private fun decideCandidateSize(genesToMutate: List<Gene>) = (genesToMutate.size * config.perOfCandidateGenesToMutate).run {
        if(this < 1.0) 1 else this.toInt()
    }
}