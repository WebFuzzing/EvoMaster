package org.evomaster.core.problem.graphql.service

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.api.service.ApiWsStructureMutator
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/*
    TODO: here there is quite bit of code that is similar to REST.
    Once this is stable, should refactoring to avoid duplication
 */
class GraphQLStructureMutator : ApiWsStructureMutator() {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(GraphQLStructureMutator::class.java)
    }

    @Inject
    private lateinit var sampler: GraphQLSampler


    override fun mutateStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {

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
        addInitializingActions(individual, mutatedGenes, sampler)
    }

    private fun mutateForRandomType(ind: GraphQLIndividual, mutatedGenes: MutatedGeneSpecification?) {

        val main = ind.seeMainActionComponents() as List<EnterpriseActionGroup<*>>

        if (main.size == 1) {
            val sampledAction = sampler.sampleRandomAction(0.05) as GraphQLAction

            ind.addGQLAction(action= sampledAction)

            Lazy.assert {
                sampledAction.hasLocalId()
            }
            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(sampledAction, ind.seeAllActions().size, localId = sampledAction.getLocalId(), false, ind.seeAllActions().size)

            return
        }

        if (randomness.nextBoolean() || main.size == config.maxTestSize) {

            //delete one at random
            log.trace("Deleting action from test")
            val chosen = randomness.choose(main.indices)

            //save mutated genes
            val removedActions = main[chosen].flatten()
            for(a in removedActions) {
                /*
                    FIXME: how does position work when adding/removing a subtree?
                 */
                mutatedGenes?.addRemovedOrAddedByAction(a, chosen, localId = main[chosen].getLocalId(),true, chosen)
            }
            ind.removeGQLActionAt(chosen)

        } else {

            //add one at random
            log.trace("Adding action to test")
            val sampledAction = sampler.sampleRandomAction(0.05) as GraphQLAction

            val chosen = randomness.choose(main.indices)
            ind.addGQLAction(chosen, sampledAction)


            Lazy.assert {
                sampledAction.hasLocalId()
            }

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(sampledAction, chosen, localId = sampledAction.getLocalId(), false, chosen)
        }

    }

    override fun getSqlInsertBuilder(): SqlInsertBuilder? {
        return sampler.sqlInsertBuilder
    }
}
