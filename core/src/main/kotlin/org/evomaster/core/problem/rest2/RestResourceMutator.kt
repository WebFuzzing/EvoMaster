package org.evomaster.core.problem.rest2

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.mutator.StandardMutator

class RestResourceMutator : StandardMutator<RestIndividualII>() {

    override fun innerMutate(individual: RestIndividualII): RestIndividualII {
        val copy = individual.copy() as RestIndividualII
        if (individual.canMutateStructure() &&
                randomness.nextBoolean(config.structureMutationProbability) && config.maxTestSize > 1) {
            //usually, either delete an action, or add a new random one
            structureMutator.mutateStructure(copy)
            return copy
        }

        //ignore sql
//        val filter = if (config.generateSqlDataWithSearch) Individual.GeneFilter.ALL
//        else Individual.GeneFilter.NO_SQL
//
//        val genesToMutate = copy.seeGenes(filter).filter(Gene::isMutable)
//
        val genesToMutate = copy.getResourceCalls().flatMap { it.seeGenes() }.filter(Gene::isMutable)
        val allGenes = copy.seeGenes().flatMap { it.flatView() }

        if (genesToMutate.isEmpty()) {
            return copy
        }

        /*
        TODO: this likely will need experiments and a better formula.
        The problem is that SQL could introduce a huge amount of genes, slowing
        down the search
        */

        val n = Math.max(1, genesToMutate.size)

        val p = 1.0 / n

        var mutated = false

        while (!mutated) { //no point in returning a copy that is not mutated

            for (gene in genesToMutate) {

                if (!randomness.nextBoolean(p)) {
                    continue
                }

                if (gene is DisruptiveGene<*> && !randomness.nextBoolean(gene.probability)) {
                    continue
                }

                mutateGene(gene, allGenes)

                mutated = true
            }
        }

        if (javaClass.desiredAssertionStatus()) {
            //TODO refactor if/when Kotlin will support lazy asserts
            assert(DbActionUtils.verifyForeignKeys(
                    individual.seeInitializingActions().filterIsInstance<DbAction>()))
        }

        //repair genes in each resource call
        copy.getResourceCalls().forEach(RestResourceCalls::repairGenesAfterMutation)
        return copy

    }
}