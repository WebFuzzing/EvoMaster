package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.StructureMutator


class RestStructureMutator : StructureMutator() {

    @Inject
    private lateinit var sampler: RestSampler


    override fun mutateStructure(individual: Individual) {
        if (individual !is RestIndividual) {
            throw IllegalArgumentException("Invalid individual type")
        }

        if (!individual.canMutateStructure()) {
            return // nothing to do
        }

        when (individual.sampleType) {
            SampleType.RANDOM -> mutateForRandomType(individual)

            SampleType.SMART_GET_COLLECTION -> mutateForSmartGetCollection(individual)

            SampleType.SMART -> throw IllegalStateException(
                    "SMART sampled individuals shouldn't be marked for structure mutations")

            //this would be a bug
            else -> throw IllegalStateException("Cannot handle sample type ${individual.sampleType}")
        }
    }

    private fun mutateForSmartGetCollection(ind: RestIndividual) {
        /*
            recall: in this case, we have 1 or more POST on same
            collection, followed by a single GET
         */

        (0 until ind.actions.size - 1).forEach {
            val a = ind.actions[it]
            assert(a !is RestCallAction || a.verb == HttpVerb.POST)
        }
        assert({ val a = ind.actions.last(); a is RestCallAction && a.verb == HttpVerb.GET }())

        if ((randomness.nextBoolean() && ind.actions.size > 2) ||
                ind.actions.size == config.maxTestSize) {

            //delete one POST, but NOT the GET
            val chosen = randomness.nextInt(ind.actions.size - 1)
            ind.actions.removeAt(chosen)

        } else {

            val postTemplate = ind.actions[0] as RestCallAction
            val post = sampler.createActionFor(postTemplate, ind.actions.last() as RestCallAction)
            //as where it is inserted should not matter, let's add it at the beginning
            ind.actions.add(0, post)
        }
    }

    private fun mutateForRandomType(ind: RestIndividual) {

        if (ind.actions.size == 1) {
            ind.actions.add(sampler.sampleRandomAction(0.05))
            return
        }

        if (randomness.nextBoolean() || ind.actions.size == config.maxTestSize) {

            //delete one at random
            val chosen = randomness.nextInt(ind.actions.size)
            ind.actions.removeAt(chosen)

        } else {

            //add one at random
            val action = sampler.sampleRandomAction(0.05)
            val chosen = randomness.nextInt(ind.actions.size)
            ind.actions.add(chosen, action)
        }

    }
}