package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.problem.api.ApiWsIndividual
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.api.service.ApiWsStructureMutator
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.mutator.MutatedGeneSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class RestStructureMutator : ApiWsStructureMutator() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestStructureMutator::class.java)
    }

    @Inject
    private lateinit var sampler: RestSampler


    override fun addInitializingActions(individual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?) {
        addInitializingActions(individual, mutatedGenes, sampler)
    }

    override fun mutateStructure(individual: Individual, evaluatedIndividual: EvaluatedIndividual<*>, mutatedGenes: MutatedGeneSpecification?, targets: Set<Int>) {
        if (individual !is RestIndividual) {
            throw IllegalArgumentException("Invalid individual type")
        }

        /*
            TODO should be enable the adding/removing of SQL commands here?
            Or should that be better to handle with DSE?
         */

        if (log.isTraceEnabled){
            log.trace("Structure will be mutated, yes? {} and the type is {}", individual.canMutateStructure(), individual.sampleType)
        }

        if (!individual.canMutateStructure()) {
            return // nothing to do
        }

        when (individual.sampleType) {
            SampleType.RANDOM -> mutateForRandomType(individual, mutatedGenes)

            SampleType.REST_SMART_GET_COLLECTION -> mutateForSmartGetCollection(individual, mutatedGenes)

            SampleType.SMART -> throw IllegalStateException(
                    "SMART sampled individuals shouldn't be marked for structure mutations")

            //this would be a bug
            else -> throw IllegalStateException("Cannot handle sample type ${individual.sampleType}")
        }

        if (config.trackingEnabled()) tag(individual, time.evaluatedIndividuals)
    }

    private fun mutateForSmartGetCollection(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?) {
        /*
            recall: in this case, we have 1 or more POST on same
            collection, followed by a single GET

            However, in case of path parameters (eg "/x/{id}/collection")
            before the collection endpoint, there might be one or more POSTs
            to setup the intermediary resources
         */

        (0 until ind.seeMainExecutableActions().size - 1).forEach {
            val a = ind.seeMainExecutableActions()[it]
            Lazy.assert{a.verb == HttpVerb.POST}
        }
        Lazy.assert{ val a = ind.seeAllActions().last(); a is RestCallAction && a.verb == HttpVerb.GET }

        val indices = ind.seeMainExecutableActions().indices
                .filter { i ->
                    val a = ind.seeMainExecutableActions()[i]
                    /*
                        one simple way to distinguish the POST on collection is that
                        they are not chaining a location, as GET is on same endpoint
                    */
                    !a.saveLocation && a.verb == HttpVerb.POST
                }

        if (indices.isEmpty()) {
            /*
                Nothing we can do here. Cannot delete a POST, and
                neither add a new one, as we have no template for
                it in the test to duplicate.
             */
            return
        }

        if (indices.size > 1 &&
                (randomness.nextBoolean() ||
                        ind.seeMainExecutableActions().size == config.maxTestSize)) {

            //delete one POST, but NOT the GET
            val chosen = randomness.choose(indices)

            //save mutated genes
            val removedActions = ind.getResourceCalls()[chosen].seeActions(ActionFilter.NO_SQL)
            Lazy.assert { removedActions.size == 1 }



            mutatedGenes?.addRemovedOrAddedByAction(
                removedActions.first(),
                ind.seeFixedMainActions().indexOf(removedActions.first()),
                localId = null,
                true,
                resourcePosition = chosen
            )

            ind.removeResourceCall(chosen)


        } else {
            //insert a new POST on the collection
            val idx = indices.last()

            val postTemplate = ind.seeMainExecutableActions()[idx]
            Lazy.assert{postTemplate.verb == HttpVerb.POST && !postTemplate.saveLocation}

            val post = sampler.createActionFor(postTemplate, ind.seeAllActions().last() as RestCallAction)


            /*
                where it is inserted should not matter, as long as
                it is before the last GET, but after all the other initializing
                POSTs
             */
            //ind.seeActions().add(idx, post)
            ind.addResourceCall(idx, RestResourceCalls(actions = mutableListOf(post), sqlActions = listOf()))

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(
                post,
                ind.seeFixedMainActions().indexOf(post),
                localId = null,
                false,
                resourcePosition = idx
            )
        }
    }

    private fun mutateForRandomType(ind: RestIndividual, mutatedGenes: MutatedGeneSpecification?) {

        if (ind.seeMainExecutableActions().size == 1) {
            val sampledAction = sampler.sampleRandomAction(0.05) as RestCallAction

            val pos = ind.seeMainExecutableActions().size


            //ind.seeActions().add(sampledAction)
            ind.addResourceCall(restCalls = RestResourceCalls(actions = mutableListOf(sampledAction), sqlActions = listOf()))

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(
                sampledAction,
                ind.seeFixedMainActions().indexOf(sampledAction),
                localId = null,
                false,
                pos
            )

            //if (config.enableCompleteObjects && (sampledAction is RestCallAction)) sampler.addObjectsForAction(sampledAction, ind)
            return
        }

        if (randomness.nextBoolean() || ind.seeMainExecutableActions().size == config.maxTestSize) {

            //delete one at random
            log.trace("Deleting action from test")
            val chosen = randomness.nextInt(ind.seeMainActionComponents().size)

            //save mutated genes
            val removedActions = ind.getResourceCalls()[chosen].seeActions(ActionFilter.NO_SQL)
            Lazy.assert { removedActions.size == 1 }
            mutatedGenes?.addRemovedOrAddedByAction(
                removedActions.first(),
               ind.seeFixedMainActions().indexOf(removedActions.first()),
                null,
                true,
                chosen
            )

            //ind.seeActions().removeAt(chosen)
            ind.removeResourceCall(chosen)

        } else {

            //add one at random
            log.trace("Adding action to test")
            val sampledAction = sampler.sampleRandomAction(0.05) as RestCallAction
            val chosen = randomness.nextInt(ind.seeMainActionComponents().size)
            //ind.seeActions().add(chosen, sampledAction)
            ind.addResourceCall(chosen, RestResourceCalls(actions = mutableListOf(sampledAction), sqlActions = listOf()))

            //save mutated genes
            mutatedGenes?.addRemovedOrAddedByAction(
                sampledAction,
                ind.seeFixedMainActions().indexOf(sampledAction),
                null,
                false,
                chosen
            )

            //if (config.enableCompleteObjects && (sampledAction is RestCallAction)) sampler.addObjectsForAction(sampledAction, ind)
            // BMR: Perhaps we could have a function for individual.addAction(action) which would cover both
            // adding the action and the associated objects and help encapsulate the individual more?
        }

    }

    override fun getSqlInsertBuilder(): SqlInsertBuilder? {
        return sampler.sqlInsertBuilder
    }

}
