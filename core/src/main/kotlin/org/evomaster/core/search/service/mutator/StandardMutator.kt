package org.evomaster.core.search.service.mutator

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
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy
import kotlin.math.max

/**
 * make the standard mutator open for extending the mutator,
 *
 * e.g., in order to handle resource rest individual
 */
open class StandardMutator<T> : Mutator<T>() where T : Individual {

    override fun doesStructureMutation(individual : T): Boolean {
        /**
         * disable structure mutation (add/remove) during focus search
         */
        if (config.disableStructureMutationDuringFocusSearch && apc.doesFocusSearch()){return false}

        return individual.canMutateStructure() &&
                config.maxTestSize > 1 && // if the maxTestSize is 1, there is no point to do structure mutation
                randomness.nextBoolean(config.structureMutationProbability)
    }

    override fun genesToMutation(individual : T, evi: EvaluatedIndividual<T>) : List<Gene> {
        val filterMutate = if (config.generateSqlDataWithSearch) ALL else NO_SQL
        val mutable = individual.seeGenes(filterMutate).filter { it.isMutable() }
        if (!archiveMutator.enableArchiveMutation())
            return mutable
        mutable.filter { !it.reachOptimal() || !archiveMutator.withinNormal()}.let {
            if (it.isNotEmpty()) return it
        }
        return mutable
    }

    override fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?) : List<Gene>{
        val genesToMutate = genesToMutation(individual, evi)
        if(genesToMutate.isEmpty()) return mutableListOf()

        val filterN = when (config.geneMutationStrategy) {
            ONE_OVER_N -> ALL
            ONE_OVER_N_BIASED_SQL -> NO_SQL
        }
        val mutated = mutableListOf<Gene>()

        if(!config.weightBasedMutationRate){
            val p = 1.0/ max(1, individual.seeGenes(filterN).filter { genesToMutate.contains(it) }.size)
            while (mutated.isEmpty()){
                genesToMutate.forEach { g->
                    if (randomness.nextBoolean(p))
                        mutated.add(g)
                }
            }
            return mutated
        }else{
            val enableAPC = config.weightBasedMutationRate && archiveMutator.enableArchiveSelection()
            return if (config.specializeSQLGeneSelection){
                val rest = !apc.doesFocusSearch() || randomness.nextBoolean()
                val sql = !apc.doesFocusSearch() || !rest

                val noSQLGenes = individual.seeGenes(NO_SQL).filter { genesToMutate.contains(it) }
                val sqlGenes = genesToMutate.filterNot { noSQLGenes.contains(it) }

                /**
                 * when focus search starts, only one is enabled.
                 */
                if (rest)
                    mutated.addAll(mwc.selectSubGene(noSQLGenes, enableAPC, targets, null, individual, evi))
                if (sql)
                    mutated.addAll(mwc.selectSubGene(sqlGenes, enableAPC, targets, null, individual, evi))

                mutated
            }else{
                mwc.selectSubGene(genesToMutate, enableAPC, targets, null, individual, evi)
            }
        }
    }

    private fun innerMutate(individual: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGene: MutatedGeneSpecification?) : T{

        val individualToMutate = individual.individual

        if(doesStructureMutation(individualToMutate)){
            val copy = (if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual) individualToMutate.next(structureMutator, maxLength = config.maxLengthOfTraces) else individualToMutate.copy()) as T
            structureMutator.mutateStructure(copy, mutatedGene)
            return copy
        }

        val copy = (if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual)
            individualToMutate.next(this, maxLength = config.maxLengthOfTraces)
        else individualToMutate.copy()) as T

        val allGenes = copy.seeGenes().flatMap { it.flatView() }

        val selectGeneToMutate = selectGenesToMutate(copy, individual, targets, mutatedGene)

        if(selectGeneToMutate.isEmpty())
            return copy

        for (gene in selectGeneToMutate){
            val isDb = copy.seeInitializingActions().any { it.seeGenes().contains(gene) }
            if (isDb){
                mutatedGene?.mutatedDbGenes?.add(gene)
                mutatedGene?.mutatedDbActionPosition?.add(copy.seeInitializingActions().indexOfFirst { it.seeGenes().contains(gene) })
            } else{
                mutatedGene?.mutatedGenes?.add(gene)
                mutatedGene?.mutatedPosition?.add(copy.seeActions().indexOfFirst { it.seeGenes().contains(gene) })
            }

            val selectionStrategy = if (!config.weightBasedMutationRate) SubsetGeneSelectionStrategy.DEFAULT
                        else if (archiveMutator.applyArchiveSelection()) SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT
                        else SubsetGeneSelectionStrategy.DETERMINISTIC_WEIGHT

            val enableAdaptiveMutation = config.probOfArchiveMutation > 0.0 || archiveMutator.enableArchiveGeneMutation()

            if (enableAdaptiveMutation || selectionStrategy == SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT){
                //root gene reference
                val id = ImpactUtils.generateGeneId(copy, gene)
                //root gene impact
                val impact = individual.getImpactOfGenes()[id]
                gene.standardMutation(randomness, apc, mwc, allGenes, selectionStrategy, enableAdaptiveMutation, AdditionalGeneSelectionInfo(config.adaptiveGeneSelectionMethod, impact, id, archiveMutator, individual,targets ))
            } else {
                gene.standardMutation(randomness, apc, mwc, allGenes, selectionStrategy)
            }
        }
        return copy
    }

    override fun mutate(individual: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?): T {

        // First mutate the individual
        val mutatedIndividual = innerMutate(individual, targets, mutatedGenes)

        postActionAfterMutation(mutatedIndividual)

        return mutatedIndividual
    }

    override fun postActionAfterMutation(mutatedIndividual: T) {

        Lazy.assert {
            DbActionUtils.verifyForeignKeys(
                    mutatedIndividual.seeInitializingActions().filterIsInstance<DbAction>())
        }

        Lazy.assert {
            mutatedIndividual.seeActions()
                    .flatMap { it.seeGenes() }
                    .all {
                        GeneUtils.verifyRootInvariant(it) &&
                                !GeneUtils.hasNonHandledCycles(it)
                    }
        }

        // repair the initialization actions (if needed)
        mutatedIndividual.repairInitializationActions(randomness)

        //Check that the repair was successful
        Lazy.assert { mutatedIndividual.verifyInitializationActions() }

    }

}