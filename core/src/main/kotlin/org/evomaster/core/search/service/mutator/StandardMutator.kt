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
import org.evomaster.core.search.gene.regex.*
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlNullable
import java.math.BigDecimal
import java.math.RoundingMode

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

        //TODO update the archive-based mutation here

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

    private fun innerMutate(individual: EvaluatedIndividual<T>, mutatedGene: MutableList<Gene>) : T{

        val individualToMutate = individual.individual

        if(doesStructureMutation(individualToMutate)){
            val copy = (if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual) individualToMutate.next(structureMutator!!) else individualToMutate.copy()) as T
            structureMutator.mutateStructure(copy)
            return copy
        }

        val copy = (if(config.enableTrackIndividual || config.enableTrackEvaluatedIndividual) individualToMutate.next(this) else individualToMutate.copy()) as T

        val allGenes = copy.seeGenes().flatMap { it.flatView() }

        val selectGeneToMutate = selectGenesToMutate(copy, individual)

        if(selectGeneToMutate.isEmpty())
            return copy

        for (gene in selectGeneToMutate){
            mutatedGene.add(gene)
            gene.standardMutation(randomness, apc, allGenes)
        }

        postActionAfterMutation(individualToMutate)

        return copy
    }

    override fun mutate(individual: EvaluatedIndividual<T>, mutatedGenes: MutableList<Gene>): T {

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
}