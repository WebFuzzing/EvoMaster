package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.fitness.RestFitness
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * Service class used to verify HTTP semantics properties.
 */
class HttpSemanticsService {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HttpSemanticsService::class.java)
    }

    /**
     * Archive including test cases
     */
    @Inject
    private lateinit var archive: Archive<RestIndividual>

    @Inject
    private lateinit var sampler: AbstractRestSampler

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var fitness: RestFitness

    @Inject
    private lateinit var idMapper: IdMapper

    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions: List<RestCallAction>

    /**
     * Individuals in the solution.
     * Derived from archive.
     */
    private lateinit var individualsInSolution: List<EvaluatedIndividual<RestIndividual>>

    //TODO quite a few code here seem duplicate for SecurityRest... to consider common abstract class

    @PostConstruct
    private fun postInit() {

        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>

    }



    fun applyHttpSemanticsPhase(): Solution<RestIndividual>{

        individualsInSolution = this.archive.extractSolution().individuals

        addForHttpSemantics()

        return archive.extractSolution()
    }

    private fun addForHttpSemantics() {

//        – invalid location, leading to a 404 when doing a follow up GET
//        – PUT with different status from 2xx should have no side-effects. Can be verified with before and after GET. PATCH can be tricky
//        – PUT for X, and then GET on it, should return exactly X (eg, check no partial updates)
//        – PUT if creating, must get 201. That means a previous GET must return 404 (or at least not a 2xx) .
//        – JSON-Merge-Patch: partial update should not impact other fields. Can have GET, PATCH, and GET to verify it


        // – 2xx GET on K : follow by success 2xx DELETE, should then give 404 on GET k (adding up to 2 calls)
        deleteShouldDelete()

        // –  A repeated followup PUT with 201 on same endpoint should not return 201 (must enforce 200 or 204)
        putRepeatedCreated()
    }

    /**
     * Checking
     * PUT /X 201
     * PUT /x 201
     *
     * can't create twice. second time expected an update, ie, either 200 or 204, or 4xx if not allowed
     */
    private fun putRepeatedCreated() {

        val putOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.PUT)

        putOperations.forEach { put ->

            val creates = RestIndividualSelectorUtils.findAndSlice(
                individualsInSolution,
                HttpVerb.PUT,
                put.path,
                status = 201
            )
            if(creates.isEmpty()){
                return@forEach
            }
            val ind = creates.minBy { it.size() }

            val put201 = ind.seeMainExecutableActions().last()
            val copy = put201.copy() as RestCallAction
            copy.resetLocalIdRecursively()
            ind.addMainActionInEmptyEnterpriseGroup(-1, copy)

            prepareEvaluateAndSave(ind)
        }
    }

    private fun prepareEvaluateAndSave(ind: RestIndividual) {
        ind.modifySampleType(SampleType.HTTP_SEMANTICS)
        ind.ensureFlattenedStructure()

        val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(ind)
        if (evaluatedIndividual == null) {
            log.warn("Failed to evaluate constructed individual in HTTP semantics testing phase")
            return
        }

        archive.addIfNeeded(evaluatedIndividual)
    }


    /**
     * Checking bugs like:
     * GET    /X 2xx
     * DELETE /X 2xx
     * GET    /X 2xx
     */
    private fun deleteShouldDelete() {

        val deleteOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.DELETE)

        deleteOperations.forEach { del ->

            val successDelete = RestIndividualSelectorUtils.findAndSlice(
                individualsInSolution,
                HttpVerb.DELETE,
                del.path,
                statusGroup = StatusGroup.G_2xx
            )
            if(successDelete.isEmpty()){
                return@forEach
            }

            val okDelete = successDelete.minBy { it.size() }

            val actions = okDelete.seeMainExecutableActions()

            val last = actions[actions.size - 1]

            //does it have a previous GET call on it in previous action?
            val hasPreviousGet = okDelete.size() > 1
                    && actions[actions.size - 2].let {
                        it.verb == HttpVerb.GET && it.path == del.path && it.usingSameResolvedPath(last)
                                && ! it.auth.isDifferentFrom(last.auth)
                        }

            val previous = if(!hasPreviousGet){
                val getDef = actionDefinitions.find { it.verb == HttpVerb.GET && it.path == del.path }
                    ?: return@forEach // we have a DELETE but no GET on this endpoint?
                val getOp = getDef.copy() as RestCallAction
                getOp.doInitialize(randomness)
                getOp.bindToSamePathResolution(last)
                getOp.auth = last.auth
                //TODO: what if the GET needs WM handling?
                okDelete.addMainActionInEmptyEnterpriseGroup(actions.size - 1, getOp)
                getOp
            } else {
                actions[actions.size - 2]
            }

            //we want to have same GET call before and after the 2xx DELETE
            val after = previous.copy() as RestCallAction
            after.resetLocalIdRecursively()
            okDelete.addMainActionInEmptyEnterpriseGroup(-1, after)

            prepareEvaluateAndSave(okDelete)
        }
    }

}