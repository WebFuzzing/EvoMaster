package org.evomaster.core.search.service.mutator

import com.google.inject.Inject
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
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator

/**
 * make the standard mutator open for extending the mutator,
 *
 * e.g., in order to handle resource rest individual
 */
open class StandardMutator<T> : Mutator<T>() where T : Individual {

    @Inject
    private lateinit var archiveMutator: ArchiveMutator

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

    /**
     * with a probability, select genes to mutate based on Archive
     */
    override fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?) : List<Gene>{
        val genesToMutate = genesToMutation(individual, evi)
        if(genesToMutate.isEmpty()) return mutableListOf()

        if(archiveMutator.applyArchiveSelection()){
            archiveMutator.selectGenesByArchive(genesToMutate, individual, evi,targets, mutatedGenes).let {
                if (it.isNotEmpty()) return it
            }
        }

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

            if (config.probOfArchiveMutation > 0.0 || archiveMutator.enableArchiveGeneMutation()){
                val id = ImpactUtils.generateGeneId(copy, gene)
                val impact = individual.getImpactOfGenes()[id]
                gene.archiveMutation(randomness, allGenes, apc, config.geneSelectionMethod, impact, id, archiveMutator, individual,targets )
            } else {
                gene.standardMutation(randomness, apc, allGenes)
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