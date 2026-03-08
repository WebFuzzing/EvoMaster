package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
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

    @Inject
    private lateinit var builder: RestIndividualBuilder

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

        sideEffectsOfFailedModification()
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
                getOp.forceNewTaints()
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


    private fun sideEffectsOfFailedModification() {

        val verbs = listOf(HttpVerb.PUT, HttpVerb.PATCH)

        for (verb in verbs) {

            val modifyOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, verb)

            modifyOperations.forEach { modOp ->

                val getDef = actionDefinitions.find { it.verb == HttpVerb.GET && it.path == modOp.path }
                    ?: return@forEach

                val failedModifyEvals = RestIndividualSelectorUtils.findIndividuals(
                    individualsInSolution,
                    verb,
                    modOp.path,
                    statusGroup = StatusGroup.G_4xx
                )
                if (failedModifyEvals.isEmpty()) return@forEach

                // gather distinct 4xx status codes observed on this verb+path
                val distinctCodes = failedModifyEvals.flatMap { ei ->
                    ei.evaluatedMainActions().mapNotNull { ea ->
                        val a = ea.action as? RestCallAction ?: return@mapNotNull null
                        val r = ea.result as? RestCallResult ?: return@mapNotNull null
                        if (a.verb == verb && a.path.isEquivalent(modOp.path)
                            && StatusGroup.G_4xx.isInGroup(r.getStatusCode())
                        ) r.getStatusCode() else null
                    }
                }.distinct()

                for (k in distinctCodes) {
                    when (k) {
                        401, 403 -> handle401Or403SideEffect(verb, k, modOp.path)
                        else     -> addGetAroundFailedModification(verb, k, modOp.path, getDef, failedModifyEvals)
                    }
                }
            }
        }
    }

    /**
     * Handles K==401 and K==403.
     *
     *  1. Find T — smallest individual ending with a clean GET 2xx on [path]
     *  2. Find a 2xx PUT/PATCH action as the body template or fall back to the K action if no 2xx exists
     *  3. Copy the template and override auth:
     *       K==401 → NoAuth   (expected to trigger 401)
     *       K==403 → a different authenticated user
     *  4. Append the modified PUT/PATCH after the GET in T, then append another GET
     */
    private fun handle401Or403SideEffect(verb: HttpVerb, k: Int, path: RestPath) {

        // GET schema definition — needed to create the GET after via builder
        val getDef = actionDefinitions.find { it.verb == HttpVerb.GET && it.path.isEquivalent(path) }
            ?: return

        // T: smallest clean individual ending with GET 2xx (no prior PUT/PATCH on same path)
        val T = RestIndividualSelectorUtils.findAndSlice(
            individualsInSolution, HttpVerb.GET, path, statusGroup = StatusGroup.G_2xx
        ).filter { ind ->
            val actions = ind.seeMainExecutableActions()
            actions.subList(0, actions.size - 1).none {
                (it.verb == HttpVerb.PUT || it.verb == HttpVerb.PATCH) && it.path.isEquivalent(path)
            }
        }.minByOrNull { it.size() } ?: return

        // find a 2xx PUT/PATCH action to use as the body template
        // 401/403 action itself if no 2xx exists
        val successAction = RestIndividualSelectorUtils.findIndividuals(
            individualsInSolution, verb, path, statusGroup = StatusGroup.G_2xx
        ).flatMap { ei ->
            ei.evaluatedMainActions().mapNotNull { ea ->
                val a = ea.action as? RestCallAction ?: return@mapNotNull null
                val r = ea.result as? RestCallResult ?: return@mapNotNull null
                if (a.verb == verb && a.path.isEquivalent(path) && StatusGroup.G_2xx.isInGroup(r.getStatusCode()))
                    a else null
            }
        }.firstOrNull()
            ?: RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution, verb, path, status = k
            ).flatMap { ei ->
                ei.evaluatedMainActions().mapNotNull { ea ->
                    (ea.action as? RestCallAction)
                        ?.takeIf { it.verb == verb && it.path.isEquivalent(path) }
                }
            }.firstOrNull()
            ?: return

        val ind = T.copy() as RestIndividual
        val getAction = ind.seeMainExecutableActions().last().copy() as RestCallAction // the GET 2xx at the end of T
        val successCopy = successAction.copy() as RestCallAction

        successCopy.forceNewTaints()
        successCopy.resetLocalIdRecursively()


        // we override auth afterwards to achieve no-auth (401) or different-user (403)
        val modifyCopy = builder.createBoundActionFor(successCopy, getAction)
        when (k) {
            401 -> modifyCopy.auth = HttpWsNoAuth()
            403 -> {
                val otherAuths = sampler.authentications
                    .getAllOthers(getAction.auth.name, HttpWsAuthenticationInfo::class.java)
                if (otherAuths.isEmpty()) return
                modifyCopy.auth = otherAuths.first()
            }
        }
        getAction.forceNewTaints()
        getAction.resetLocalIdRecursively()

        val getAfter = builder.createBoundActionFor(getDef, getAction)


        ind.addMainActionInEmptyEnterpriseGroup(action = modifyCopy)
        ind.addMainActionInEmptyEnterpriseGroup(action = getAfter)



        ind.ensureFlattenedStructure()
        org.evomaster.core.Lazy.assert { ind.verifyValidity(); true }

        prepareEvaluateAndSave(ind)
    }

    /**
     * Takes the smallest individual in [candidates] where [verb] on [path] returned [k],
     * slices it at that action, then inserts a GET immediately before it and appends
     * another GET immediately after it — both on the same resolved path and with the
     * same auth as the PUT/PATCH:
     *
     *   GET       /path  (same auth as PUT/PATCH)
     *   PUT|PATCH /path  [k]
     *   GET       /path  (same auth)
     */
    private fun addGetAroundFailedModification(
        verb: HttpVerb,
        k: Int,
        path: RestPath,
        getDef: RestCallAction,
        candidates: List<EvaluatedIndividual<RestIndividual>>
    ) {
        val kEval = RestIndividualSelectorUtils.findIndividuals(candidates, verb, path, status = k)
            .minByOrNull { it.individual.size() } ?: return

        val ind = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(kEval, verb, path, status = k)

        val actions = ind.seeMainExecutableActions()
        val last = actions.last() // the PUT/PATCH [k]

        // insert GET before the PUT/PATCH
        val getBefore = builder.createBoundActionFor(getDef, last)
        ind.addMainActionInEmptyEnterpriseGroup(actions.size - 1, getBefore)

        // append GET after the PUT/PATCH
        val getAfter = builder.createBoundActionFor(getDef, last)
        ind.addMainActionInEmptyEnterpriseGroup(-1, getAfter)

        prepareEvaluateAndSave(ind)
    }


}
