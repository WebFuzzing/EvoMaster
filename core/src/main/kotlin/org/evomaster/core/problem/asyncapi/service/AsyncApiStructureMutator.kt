package org.evomaster.core.problem.asyncapi.service

import com.google.inject.Inject
import org.evomaster.core.database.sql.SqlInsertBuilder
import org.evomaster.core.problem.api.service.ApiWsStructureMutator
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification

/**
 * Changes how many messages a test publishes: one more, drawn from the action templates, or one
 * fewer. Which messages, and what they say, is left to the gene mutations.
 */
class AsyncApiStructureMutator : ApiWsStructureMutator() {

    @Inject
    private lateinit var sampler: AsyncApiSampler

    override fun mutateStructure(
        individual: Individual,
        evaluatedIndividual: EvaluatedIndividual<*>,
        mutatedGenes: MutatedGeneSpecification?,
        targets: Set<Int>
    ) {
        if (individual !is AsyncApiIndividual) {
            throw IllegalArgumentException(
                "Invalid: individual type to be mutated with AsyncApiStructureMutator should be" +
                        " AsyncApiIndividual but ${individual::class.java.simpleName}"
            )
        }

        if (!individual.canMutateStructure()) return
        if (config.maxTestSize == 1) return

        addOrRemoveAMessage(individual, mutatedGenes)

        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    private fun addOrRemoveAMessage(individual: AsyncApiIndividual, mutatedGenes: MutatedGeneSpecification?) {

        val size = individual.seeMainExecutableActions().size

        //a test keeps at least one message, and never grows past what the user allowed
        val canAdd = size < config.maxTestSize
        val canRemove = size > 1

        if (!canAdd && !canRemove) {
            return
        }

        if (canAdd && (!canRemove || randomness.nextBoolean())) {
            val added = sampler.sampleRandomAction()
            individual.addAction(action = added)
            mutatedGenes?.addRemovedOrAddedByAction(
                added,
                individual.seeFixedMainActions().indexOf(added),
                null,
                false,
                size
            )
        } else {
            val chosen = randomness.choose(individual.seeMainActionComponents().indices)
            val removed = individual.seeMainExecutableActions()[chosen]
            mutatedGenes?.addRemovedOrAddedByAction(
                removed,
                individual.seeFixedMainActions().indexOf(removed),
                null,
                true,
                size
            )
            individual.removeAction(chosen)
        }
    }

    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        addInitializingActions(individual, mutatedGenes, sampler)
    }

    override fun getSqlInsertBuilder(): SqlInsertBuilder? {
        return sampler.sqlInsertBuilder
    }
}
