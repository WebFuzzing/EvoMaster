package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.Lazy
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
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
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.service.fitness.RestFitness
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.time.ExecutionPhaseController
import org.evomaster.core.search.service.time.TimeBoxedPhase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * Service class used to verify HTTP semantics properties.
 */
class HttpSemanticsService : TimeBoxedPhase{

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

    @Inject
    private lateinit var epc: ExecutionPhaseController

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

    override fun applyPhase() {
        applyHttpSemanticsPhase()
    }

    override fun hasPhaseTimedOut(): Boolean {
        return epc.hasPhaseTimedOut(ExecutionPhaseController.Phase.ADDITIONAL_ORACLES)
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


        if(hasPhaseTimedOut()) return
        // – 2xx GET on K : follow by success 2xx DELETE, should then give 404 on GET k (adding up to 2 calls)
        deleteShouldDelete()

        if(hasPhaseTimedOut()) return
        // –  A repeated followup PUT with 201 on same endpoint should not return 201 (must enforce 200 or 204)
        putRepeatedCreated()

        if(hasPhaseTimedOut()) return
        sideEffectsOfFailedModification()

        if(hasPhaseTimedOut()) return
        partialUpdatePut()

        if(hasPhaseTimedOut()) return
        mergePatchSideEffect()

        if(hasPhaseTimedOut()) return
        misleadingCreatePut()

        if(hasPhaseTimedOut()) return
        nonIdempotentPut()

        if(hasPhaseTimedOut()) return
        // – invalid location, leading to a 404 when doing a follow up GET
        invalidLocation()

        if(hasPhaseTimedOut()) return
        invalidAllow()
    }

    /**
     * For each path, make a single OPTIONS call. The Allow header must not list
     * verbs that are not declared in the schema (ignoring OPTIONS and HEAD).
     */
    private fun invalidAllow() {

        // OPTIONS has no schema template, so the resource sampler cannot build such an
        // individual. We build it directly, reusing the shared search global state.
        val globalState = individualsInSolution.firstOrNull()?.individual?.searchGlobalState ?: return

        val actions = actionDefinitions.distinctBy { it.path }

        for (a in actions) {

            if (hasPhaseTimedOut()) return

            val pathVariables = a.parameters
                .filterIsInstance<PathParam>()
                .map { it.copy() }
                .toMutableList()

            val path = a.path.copy()
            val options = RestCallAction("${HttpVerb.OPTIONS}:$path", HttpVerb.OPTIONS, path, pathVariables, a.auth)
            options.doInitialize(randomness)

            val ind = RestIndividual(mutableListOf(options), SampleType.HTTP_SEMANTICS)
            ind.doGlobalInitialize(globalState)
            prepareEvaluateAndSave(ind)
        }
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

            if(hasPhaseTimedOut()) return

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

    private fun prepareEvaluateAndSave(ind: RestIndividual): EvaluatedIndividual<RestIndividual>? {
        ind.modifySampleType(SampleType.HTTP_SEMANTICS)
        ind.ensureFlattenedStructure()

        val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(ind)
        if (evaluatedIndividual == null) {
            log.warn("Failed to evaluate constructed individual in HTTP semantics testing phase")
            return null
        }

        archive.addIfNeeded(evaluatedIndividual)
        return evaluatedIndividual
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

            if(hasPhaseTimedOut()) return

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


    /**
     * A failed PUT/PATCH (4xx) must not mutate the resource state.
     * For each path with PUT/PATCH and GET, and for each distinct K in 4xx:
     * - find T (smallest individual ending with GET 2xx on same path), slice after it
     * - append a PUT/PATCH targeting K, then a final GET to check state is unchanged
     * - K==401: use a 2xx PUT/PATCH with no auth; K==403: different auth than the GET
     * - K==404: special - no T, prepend GET->(PUT/PATCH 404)->GET, all expecting 404
     * - otherwise (400, 409, ...): copy a 4xx action with same auth as the GET
     */
    private fun sideEffectsOfFailedModification() {

        val verbs = listOf(HttpVerb.PUT, HttpVerb.PATCH)

        for (verb in verbs) {

            val modifyOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, verb)

            modifyOperations.forEach { modOp ->

                if(hasPhaseTimedOut()) return

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
                        404  -> handle404SideEffect(verb, modOp.path, getDef)
                        else -> handleSideEffectOfFailedModification(verb, k, modOp.path, getDef)
                    }
                }
            }
        }
    }

    /**
     * Handles K==404: the resource did not exist before the call, PUT/PATCH also returned 404,
     * and the final GET must still return 404 — anything else is a side effect.
     *
     *   GET       /path  → 404  (resource absent)
     *   PUT|PATCH /path  → 404
     *   GET       /path  → ???  (oracle: must still be 404)
     */
    private fun handle404SideEffect(verb: HttpVerb, path: RestPath, getDef: RestCallAction) {

        val kEval = RestIndividualSelectorUtils.findIndividuals(individualsInSolution, verb, path, status = 404)
            .minByOrNull { it.individual.size() } ?: return

        val ind = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(kEval, verb, path, status = 404)

        val actions = ind.seeMainExecutableActions()
        val last = actions.last() // the PUT/PATCH [404]

        val getBefore = builder.createBoundActionFor(getDef, last)
        ind.addMainActionInEmptyEnterpriseGroup(actions.size - 1, getBefore)

        val getAfter = builder.createBoundActionFor(getDef, last)
        ind.addMainActionInEmptyEnterpriseGroup(-1, getAfter)

        prepareEvaluateAndSave(ind)
    }

    /**
     * Handles all non-404 4xx cases (K==400, K==401, K==403, K==409, …).
     * Starts from T (smallest clean GET 2xx individual), appends the K-returning PUT/PATCH
     * bound to that GET's resolved path, then appends a final GET to verify state is unchanged.
     *
     * Action template:
     * - K==401/403 : prefer a 2xx PUT/PATCH (valid body, wrong auth); fall back to the K action
     * - otherwise  : use the K-returning action directly (correct auth, body that causes K)
     *
     * Auth override:
     * - K==401     → NoAuth
     * - K==403     → a different authenticated user
     * - otherwise  → same auth as the GET (failure is due to body content, not access rights)
     */
    private fun handleSideEffectOfFailedModification(verb: HttpVerb, k: Int, path: RestPath, getDef: RestCallAction) {

        // T: smallest individual ending with GET 2xx on the same path
        val T = RestIndividualSelectorUtils.findAndSlice(
            individualsInSolution, HttpVerb.GET, path, statusGroup = StatusGroup.G_2xx
        ).minByOrNull { it.size() } ?: return

        val actionTemplate = when {
            k == 401 || k == 403 ->
                // prefer a 2xx action (valid body); fall back to the K action if no 2xx exists
                RestIndividualSelectorUtils.findIndividuals(
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
            else ->
                RestIndividualSelectorUtils.findIndividuals(
                    individualsInSolution, verb, path, status = k
                ).flatMap { ei ->
                    ei.evaluatedMainActions().mapNotNull { ea ->
                        (ea.action as? RestCallAction)
                            ?.takeIf { it.verb == verb && it.path.isEquivalent(path) }
                    }
                }.firstOrNull()
        } ?: return

        val ind = T.copy() as RestIndividual
        val getAction = ind.seeMainExecutableActions().last().copy() as RestCallAction

        val templateCopy = actionTemplate.copy() as RestCallAction
        templateCopy.forceNewTaints()
        templateCopy.resetLocalIdRecursively()

        val modifyCopy = builder.createBoundActionFor(templateCopy, getAction)
        when (k) {
            401 -> modifyCopy.auth = HttpWsNoAuth()
            403 -> {
                val otherAuths = sampler.authentications
                    .getAllOthers(getAction.auth.name, HttpWsAuthenticationInfo::class.java)
                if (otherAuths.isEmpty()) return
                modifyCopy.auth = otherAuths.first()
            }
            else -> modifyCopy.auth = getAction.auth
        }

        getAction.forceNewTaints()
        getAction.resetLocalIdRecursively()

        val getAfter = builder.createBoundActionFor(getDef, getAction)

        ind.addMainActionInEmptyEnterpriseGroup(action = modifyCopy)
        ind.addMainActionInEmptyEnterpriseGroup(action = getAfter)

        ind.ensureFlattenedStructure()
        Lazy.assert { ind.verifyValidity(); true }

        prepareEvaluateAndSave(ind)
    }

    /**
     * HTTP_PARTIAL_UPDATE_PUT oracle: PUT makes a full replacement, not a partial update.
     * If only some fields should be modified, PATCH must be used instead.
     *
     * Sequence checked:
     *   PUT /X  body=B  ->  2xx
     *   GET /X          ->  response body must match exactly B
     *                       (no field from a previous state should bleed through)
     *
     * Finds the shortest 2xx PUT individual, slices it to end at that PUT,
     * then appends a bound GET on the same resolved path to verify the full replacement.
     */
    private fun partialUpdatePut() {

        val putOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.PUT)

        putOperations.forEach { putOp ->

            val getDef = actionDefinitions.find { it.verb == HttpVerb.GET && it.path == putOp.path }
                ?: return@forEach

            val successPuts = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution, HttpVerb.PUT, putOp.path, statusGroup = StatusGroup.G_2xx
            )
            if (successPuts.isEmpty()) return@forEach

            val ind = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                successPuts.minBy { it.individual.size() },
                HttpVerb.PUT, putOp.path, statusGroup = StatusGroup.G_2xx
            )

            val last = ind.seeMainExecutableActions().last() // the PUT 2xx
            val getAfter = builder.createBoundActionFor(getDef, last)
            ind.addMainActionInEmptyEnterpriseGroup(-1, getAfter)

            prepareEvaluateAndSave(ind)
        }
    }

    /**
     * HTTP_INVALID_MERGE_PATCH oracle (RFC 7386): a partial merge-patch must not change fields
     * absent from the request body. Slice an individual at a 2xx PATCH (keeping the creation),
     * then wrap that PATCH with a GET before and after:
     *   [...create...]  GET /X  ->  PATCH /X (2xx)  ->  GET /X
     */
    private fun mergePatchSideEffect() {

        val patchOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.PATCH)

        patchOperations.forEach { patchOp ->

            if (hasPhaseTimedOut()) return

            val getDef = actionDefinitions.find { it.verb == HttpVerb.GET && it.path == patchOp.path }
                ?: return@forEach

            val successPatches = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution, HttpVerb.PATCH, patchOp.path, statusGroup = StatusGroup.G_2xx
            )
            if (successPatches.isEmpty()) return@forEach

            for (candidate in successPatches.sortedBy { it.individual.size() }) {

                if (hasPhaseTimedOut()) return

                val ind = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                    candidate, HttpVerb.PATCH, patchOp.path, statusGroup = StatusGroup.G_2xx
                )

                val patch = ind.seeMainExecutableActions().last()
                val size = ind.seeMainExecutableActions().size

                // bind the GET to the PATCH's resolved path (so usingSameResolvedPath holds). if the
                // PATCH takes its id from a creation's Location header, also link the GET to it
                val creator = patch.usePreviousLocationId?.let { locId ->
                    ind.seeMainExecutableActions().firstOrNull {
                        (it.verb == HttpVerb.POST || it.verb == HttpVerb.PUT)
                            && it.saveCreatedResourceLocation && it.creationLocationId() == locId
                    }
                }
                val getBefore = builder.createBoundActionFor(getDef, patch)
                creator?.saveAndLinkLocationTo(getBefore)
                ind.addMainActionInEmptyEnterpriseGroup(size - 1, getBefore)

                val getAfter = builder.createBoundActionFor(getDef, patch)
                creator?.saveAndLinkLocationTo(getAfter)
                ind.addMainActionInEmptyEnterpriseGroup(-1, getAfter)

                val ei = prepareEvaluateAndSave(ind)
                if (ei != null && DetectedFaultUtils.getDetectedFaultCategories(ei)
                        .contains(ExperimentalFaultCategory.HTTP_INVALID_MERGE_PATCH)) {
                    return@forEach
                }
            }
        }
    }


    /**
     * HTTP_MISLEADING_CREATE_PUT oracle: a PUT that returns 201 claims it created a new resource.
     * If so, a GET on the same path immediately before the PUT should return 404 (or at least
     * not 2xx), because the resource should not exist yet.
     *
     * Sequence checked:
     *   [...resource creation via POST/PUT...]
     *   GET /X  -> 2xx  (resource exists)
     *   PUT /X -> 201  (BUG: claims creation, but resource already existed -> should be 200/204)
     *
     * Starts from the smallest individual ending with GET 2xx on the path, then appends a copy of an existing
     * PUT 201 action (so its body is known valid) rebound to the GET's resolved path and auth.
     */
    private fun misleadingCreatePut() {

        val putOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.PUT)

        putOperations.forEach { putOp ->

            if (hasPhaseTimedOut()) return

            // template: an existing PUT 201 on this path (its body is known valid).
            val putTemplate = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution, HttpVerb.PUT, putOp.path, status = 201
            ).asSequence().flatMap { ei ->
                ei.evaluatedMainActions().asSequence().mapNotNull { ea ->
                    val a = ea.action as? RestCallAction ?: return@mapNotNull null
                    val r = ea.result as? RestCallResult ?: return@mapNotNull null
                    if (a.verb == HttpVerb.PUT && a.path.isEquivalent(putOp.path)
                        && r.getStatusCode() == 201) a else null
                }
            }.firstOrNull() ?: return@forEach

            // T: smallest individual ending with GET 2xx (resource exists after creation)
            val T = RestIndividualSelectorUtils.findAndSlice(
                individualsInSolution, HttpVerb.GET, putOp.path, statusGroup = StatusGroup.G_2xx
            ).minByOrNull { it.size() } ?: return@forEach

            val ind = T.copy() as RestIndividual
            val getAction = ind.seeMainExecutableActions().last() // the GET 2xx

            // copy the PUT 201 (preserves valid body), rebind to the GET's path and auth
            val putAction = putTemplate.copy() as RestCallAction
            putAction.resetLocalIdRecursively()
            putAction.forceNewTaints()
            putAction.auth = getAction.auth
            putAction.bindToSamePathResolution(getAction)
            ind.addMainActionInEmptyEnterpriseGroup(-1, putAction)

            prepareEvaluateAndSave(ind)
        }
    }

    /**
     * HTTP_NON_IDEMPOTENT_PUT oracle: PUT must be idempotent — applying it once or N times must
     * leave the resource in the same state. Calling the same PUT twice and observing different
     * resource state in two GET responses indicates a bug (e.g. a "deposit" that accumulates).
     *
     * Sequence checked:
     *   [...resource creation...]
     *   PUT  /X       -> 2xx        (the 1st PUT, already in T)
     *   GET  /X (or ancestor) -> 2xx  (state after 1st PUT)
     *   PUT  /X       → 2xx        (a COPY of the 1st PUT, same body)
     *   GET  /X (or ancestor) -> 2xx  (state after 2nd PUT)
     *
     * The two GETs must observe identical state; if any number/boolean leaf field differs,
     * idempotency is broken. Strings are intentionally ignored to avoid flakiness from
     * timestamps/UUIDs etc.
     *
     * For endpoints like PUT /accounts/{id}/deposit, the GET is taken on the closest ancestor
     * (e.g. GET /accounts/{id}) so the resource state can be observed.
     */
    private fun nonIdempotentPut() {

        val putOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.PUT)

        putOperations.forEach { putOp ->

            if (hasPhaseTimedOut()) return

            // GET on the same path, or longest ancestor with a GET
            val getDef = actionDefinitions.find { it.verb == HttpVerb.GET && it.path == putOp.path }
                ?: actionDefinitions
                    .filter { it.verb == HttpVerb.GET && it.path.isSameOrAncestorOf(putOp.path) }
                    .maxByOrNull { it.path.levels() }
                ?: return@forEach

            // T: smallest individual ending with PUT 2xx on this path
            val ind = RestIndividualSelectorUtils.findAndSlice(
                individualsInSolution, HttpVerb.PUT, putOp.path, statusGroup = StatusGroup.G_2xx
            ).minByOrNull { it.size() } ?: return@forEach

            val firstPut = ind.seeMainExecutableActions().last() // PUT 2xx (1st)

            // GET after the 1st PUT: bound to firstPut's resolved path and auth
            val get1 = builder.createBoundActionFor(getDef, firstPut)

            // 2nd PUT: exact copy of the 1st PUT (same body) to test idempotency of that request
            val secondPut = firstPut.copy() as RestCallAction
            secondPut.resetLocalIdRecursively()

            // GET after the 2nd PUT
            val get2 = builder.createBoundActionFor(getDef, firstPut)

            ind.addMainActionInEmptyEnterpriseGroup(-1, get1)
            ind.addMainActionInEmptyEnterpriseGroup(-1, secondPut)
            ind.addMainActionInEmptyEnterpriseGroup(-1, get2)

            prepareEvaluateAndSave(ind)
        }
    }


    /**
     * HTTP_INVALID_LOCATION oracle: any response carrying a Location header must point
     * to a resource that actually exists — a follow-up GET on that Location must not
     * return 404.
     *
     * Sequence built:
     *   [...]
     *   ANY /X  -> response with Location header L
     *   GET  L  -> oracle target: must NOT be 404
     *
     */
    private data class LocationCandidate(
        val individual: EvaluatedIndividual<RestIndividual>,
        val sourceIndex: Int
    )

    /**
     * Every action in [ei] whose response carried a non-blank Location header,
     * paired with its index in [RestIndividual.seeMainExecutableActions].
     */
    private fun locationCandidatesIn(
        ei: EvaluatedIndividual<RestIndividual>
    ): List<LocationCandidate> {
        val evaluated = ei.evaluatedMainActions()
        val candidates = mutableListOf<LocationCandidate>()
        for (idx in evaluated.indices) {
            val ea = evaluated[idx]
            ea.action as? RestCallAction ?: continue
            val r = ea.result as? RestCallResult ?: continue
            if (r.getLocation().isNullOrBlank()) continue
            candidates.add(LocationCandidate(ei, idx))
        }
        return candidates
    }

    private fun invalidLocation() {

        val candidates = individualsInSolution.asSequence()
            .flatMap { ei -> locationCandidatesIn(ei) }
            .groupBy {
                val source = it.individual.individual.seeMainExecutableActions()[it.sourceIndex]
                source.verb to source.path
            }
            .values
            .map { group -> group.minBy { it.individual.individual.size() } }

        for (candidate in candidates) {

            if (hasPhaseTimedOut()) return

            val ind = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                candidate.individual.individual, candidate.sourceIndex
            )
            val creator = ind.seeMainExecutableActions().last()

            // runtime URL is resolved from the Location header
            // (relative/absolute, possibly with query params). We do not bind to a schema
            // The path here is a structural placeholder; the real URL comes from chainState.
            val getAction = RestCallAction(
                id = "GET:LOCATION-FOLLOWUP",
                verb = HttpVerb.GET,
                path = RestPath("/"),
                parameters = mutableListOf(),
                auth = creator.auth
            )
            getAction.doInitialize(randomness)
            getAction.forceNewTaints()

            try {
                // TODO: RestCallAction.creationLocationId() currently restricts location-id generation
                //  to POST/PUT and throws otherwise, so this branch silently no-ops on other verbs.
                //  After that restriction is refactored to allow any verb whose response carried a
                //  Location header, this catch can be dropped and the oracle will fire for all verbs.
                creator.saveAndLinkLocationTo(getAction)
            } catch (e: IllegalArgumentException) {
                continue
            }

            // add getAction as a last operation
            ind.addMainActionInEmptyEnterpriseGroup(-1, getAction)

            prepareEvaluateAndSave(ind)
        }
    }
}
