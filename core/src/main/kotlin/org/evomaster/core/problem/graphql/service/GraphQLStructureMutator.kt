package org.evomaster.core.problem.graphql.service

import com.google.inject.Inject
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.evomaster.core.search.service.mutator.StructureMutator
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/*
    TODO: here there is quite bit of code that is similar to REST.
    Once this is stable, should refactoring to avoid duplication
 */
class GraphQLStructureMutator : StructureMutator() {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(GraphQLStructureMutator::class.java)
    }

    @Inject
    private lateinit var sampler: GraphQLSampler


    override fun mutateStructure(individual: Individual,evaluated: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {

        if (individual !is GraphQLIndividual) {
            throw IllegalArgumentException("Invalid individual type")
        }

        if (!individual.canMutateStructure()) {
            return // nothing to do
        }

        when (individual.sampleType) {
            SampleType.RANDOM -> mutateForRandomType(individual, mutatedGenes)

            //TODO other kinds

            //this would be a bug
            else -> throw IllegalStateException("Cannot handle sample type ${individual.sampleType}")
        }

        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        /*
            TODO: this will be the same code as in REST.
            But as we are refactoring the handling of DBs, we can wait before doing it here
         */
    }

    private fun mutateForRandomType(ind: GraphQLIndividual, mutatedGenes: MutatedGeneSpecification?) {

        if (ind.seeActions().size == 1) {
            val sampledAction = sampler.sampleRandomAction(0.05) as GraphQLAction

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(sampledAction, ind.seeActions().size, false, ind.seeActions().size)

            ind.actions.add(sampledAction)

            return
        }

        if (randomness.nextBoolean() || ind.seeActions().size == config.maxTestSize) {

            //delete one at random
            log.trace("Deleting action from test")
            val chosen = randomness.nextInt(ind.seeActions().size)

            //save mutated genes
            val removedActions = ind.actions[chosen]
            mutatedGenes?.addRemovedOrAddedByAction(removedActions, chosen, true, chosen)

            ind.actions.removeAt(chosen)

        } else {

            //add one at random
            log.trace("Adding action to test")
            val sampledAction = sampler.sampleRandomAction(0.05) as GraphQLAction

            val chosen = randomness.nextInt(ind.seeActions().size)
            ind.actions.add(chosen, sampledAction)

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(sampledAction, chosen, false, chosen)
        }
    }
}