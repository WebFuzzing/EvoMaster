package org.evomaster.core.search.service.mutator

import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N
import org.evomaster.core.EMConfig.GeneMutationStrategy.ONE_OVER_N_BIASED_SQL
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.UpdateForBodyParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Individual.GeneFilter.ALL
import org.evomaster.core.search.Individual.GeneFilter.NO_SQL
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactInfoCollection.GeneMutationSelectionMethod
import org.evomaster.core.search.impact.impactInfoCollection.ImpactUtils
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

    override fun genesToMutation(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>) : List<Gene> {
        val filterMutate = if (config.generateSqlDataWithSearch) ALL else NO_SQL
        val mutable = individual.seeGenes(filterMutate).filter { it.isMutable() }
        if (!archiveMutator.enableArchiveGeneMutation())
            return mutable
        mutable.filter { !it.reachOptimal(targets) || !archiveMutator.withinNormal()}.let {
            if (it.isNotEmpty()) return it
        }
        return mutable
    }

    override fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?) : List<Gene>{
        val genesToMutate = genesToMutation(individual, evi, targets)
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
        }else{
            val enableAPC = config.weightBasedMutationRate && archiveMutator.enableArchiveSelection()
            while (mutated.isEmpty()){
                if (config.specializeSQLGeneSelection){
                    val noSQLGenes = individual.seeGenes(NO_SQL).filter { genesToMutate.contains(it) }
                    val sqlGenes = genesToMutate.filterNot { noSQLGenes.contains(it) }
                    mutated.addAll(mwc.selectSubGene(noSQLGenes, enableAPC, targets, null, individual, evi, forceNotEmpty = false, numOfGroup = 2))
                    mutated.addAll(mwc.selectSubGene(sqlGenes, enableAPC, targets, null, individual, evi, forceNotEmpty = false, numOfGroup = 2))
                }else{
                    mutated.addAll(mwc.selectSubGene(genesToMutate, enableAPC, targets, null, individual, evi, forceNotEmpty = false))
                }
            }
        }
        return mutated
    }

    private fun copyIndividualWithTracking(individual: EvaluatedIndividual<T>) : T{

        val individualToMutate = individual.individual

        val copy = (if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual)
            individualToMutate.next(this, maxLength = config.maxLengthOfTraces)
        else individualToMutate.copy()) as T

        return copy
    }

    private fun mutationPreProcessing(individual: T){

        for(a in individual.seeActions()){
            if(a !is RestCallAction){
                continue
            }
            val update = a.parameters.find { it is UpdateForBodyParam } as? UpdateForBodyParam
            if(update != null){
                a.parameters.removeIf{ it is BodyParam}
                a.parameters.removeIf{ it is UpdateForBodyParam}
                a.parameters.add(update.body)
            }
        }
    }

    private fun innerMutate(individual: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGene: MutatedGeneSpecification?) : T{

        val copy = copyIndividualWithTracking(individual)

        if(doesStructureMutation(individual.individual)){
            structureMutator.mutateStructure(copy, mutatedGene)
            return copy
        }

        mutationPreProcessing(copy)

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

            val additionInfo = if(selectionStrategy == SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT){
                val id = ImpactUtils.generateGeneId(copy, gene)
                //root gene impact
                val impact = individual.getImpact(copy, gene)
                AdditionalGeneSelectionInfo(config.adaptiveGeneSelectionMethod, impact, id, archiveMutator, individual,targets)
            }else if(archiveMutator.enableArchiveGeneMutation()){
                AdditionalGeneSelectionInfo(GeneMutationSelectionMethod.NONE, null, null, archiveMutator, individual,targets)
            }else null

            gene.standardMutation(randomness, apc, mwc, allGenes, selectionStrategy, archiveMutator.enableArchiveGeneMutation(), additionalGeneMutationInfo = additionInfo)
        }
        return copy
    }

    override fun mutate(individual: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?): T {

        //  mutate the individual
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