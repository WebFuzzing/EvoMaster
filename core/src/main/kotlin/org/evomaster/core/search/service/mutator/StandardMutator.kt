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
import kotlin.math.max
import kotlin.math.min

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

    override fun selectGenesToMutate(individual: T, evi: EvaluatedIndividual<T>, targets: Set<Int>, mutatedGenes: MutatedGeneSpecification?) : List<Gene>{
        val genesToMutate = genesToMutation(individual, evi)
        if(genesToMutate.isEmpty()) return mutableListOf()

        /*
            filterN decides which genes contribute to involve its weight,
            eg, sql genes will not used when NO_SQL is specified

            From:
            once we have something like “mr = d * (1/N) + (1-d) * (x(W))”, what to do with SQL genes?
            Maybe we do not need to treat them specially if Hyper Mutation (HM) and Adaptive Control (AC) will work fine.
            Ie, we can just use ONE_OVER_N and ignore ONE_OVER_N_BIASED_SQL
         */
        val filterN = when (config.geneMutationStrategy) {
            ONE_OVER_N -> ALL
            ONE_OVER_N_BIASED_SQL -> NO_SQL
        }
        val genesForMutationRate = individual.seeGenes(filterN).filter { it.isMutable() }

        //by default, weight of all mutable genes is 1
        val geneAndWeights = genesToMutate.map { Pair(it, 1) }.toMap().toMutableMap()

        /*
            mutation rate can be manipulated by different weight methods
            eg, only depends on static weight, or impact derived based on archive (archive-based solution)
         */
        if(config.adaptiveMutationRate){
            if(archiveMutator.enableArchiveSelection())
                archiveMutator.calculateWeightByArchive(genesToMutate, geneAndWeights, individual, evi, targets, mutatedGenes)
            else{
                genesForMutationRate.forEach {
                    geneAndWeights[it] = it.mutationWeight()
                }
            }
        }

        /*
            mutation space can be calculated by including
                - all mutable genes i.e., geneAndWeights
                - its subset (as below decided by filterN), e.g., gene in [genesForMutationRate], thus any of genes should be found in [geneAndWeights]
         */
        val mutationSpace = max(1, genesForMutationRate.sumBy { geneAndWeights[it]!! })

        /*
           From Andrea:
           having some scaling value d in [0,1], the mr could be something like:  mr = d * (1/N) + (1-d) * (x(W)),
           where sum_all(x(W)) = 1/N  and x(W) is some value that increases for higher weight W

           Man:
           since weight is adaptive, shall we make 'd' adaptive?
           is only one 'd' over search or specific to one individual?
       */
        val d = getTunableD()

        /*
            From Andrea:
            HM: instead of applying 1 mutation on average (ie, 1/N), we should have T mutations, where T<=N.
            T could start with something high, and then decrease gradually to 1 by the time the “focus search” starts.
            Man: starting percentage can be specified with EMConfig. but end can be 1% or 1/mutationSpace
         */
        val t = apc.getExploratoryValue(config.startingPerOfGenesToMutate, 0.1)

       return selectGene(genesToMutate, mutationSpace, geneAndWeights, d, t)
    }

    /**
     * select genes to mutate from [mutableGenes] which can be used to select gene from individual or select gene from eg ObjectGene
     */
    fun selectGene(mutableGenes : List<Gene>, mutationSpace: Int, genesAndWeights : MutableMap<Gene, Int>, d : Double, t : Double) : List<Gene>{
        val genesToSelect = mutableListOf<Gene>()

        var mutated = false

        /*
            no point in returning a copy that is not mutated,
            as we do not use xover
         */
        while (!mutated) { //

            for (gene in mutableGenes) {
                // in terms of an individual mutation, d and t should be fixed for all mutable gene
                if (!randomness.nextBoolean(adaptiveMutationRate(mutationSpace, genesAndWeights[gene]!!, d, t))) {
                    continue
                }

                genesToSelect.add(gene)

                mutated = true
            }
        }
        return genesToSelect

    }

    private fun adaptiveMutationRate(mutationSpace: Int,  weight: Int, d : Double, t : Double) : Double{

        //Hyper-Mutation (HM)
        val mr = 1.0/ mutationSpace
        if (!config.adaptiveMutationRate) return mr

        /*
         Man: with the function,
            t is to decide a number of genes to mutate which might be controlled with budget, i.e., DPC
            w is the weight of the gene
                where
                    'weight' is based on
                        - its static weight (e.g., static weight of ObjectGene is sum of its field)
                        - adaptive weight, e.g., the gene might be more impactful with archive-based solution
                    d is to decide the weight of getWeight(), e.g., d = 1, getWeight() contributes nothing to the w
         */
        val w = d + (1.0 - d)*(weight - 1)
        return min(config.maxMutationRate, t * w) // t * w is simplified from t * mutationSpace * w/mutationSpace
    }


    /**
     * d might related to adaptive
     */
    private fun getTunableD() : Double{
        return randomness.nextDouble()
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