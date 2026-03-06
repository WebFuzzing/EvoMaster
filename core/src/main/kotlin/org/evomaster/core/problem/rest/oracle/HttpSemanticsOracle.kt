package org.evomaster.core.problem.rest.oracle

import com.google.gson.JsonParser
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.utils.GeneUtils

object HttpSemanticsOracle {


    fun hasRepeatedCreatePut(individual: RestIndividual,
                             actionResults: List<ActionResult>
    ): Boolean{

        if(individual.size() < 2){
            return false
        }
        val actions = individual.seeMainExecutableActions()
        val first = actions[actions.size - 2]  // PUT 201
        val second = actions[actions.size - 1] // PUT 201

        //both using PUT
        if(first.verb != HttpVerb.PUT || second.verb != HttpVerb.PUT){
            return false
        }

        //on same resource
        if(! first.usingSameResolvedPath(second)){
            return false
        }

        //with same auth
        if(first.auth.isDifferentFrom(second.auth)){
            /*
                this might require some explanation. What if instead of a parametric endpoint
                /x/{id}
                we have a static
                /x
                where different resources are based on auth info?
                in this latter case, 2 PUTs with 201 on /x could be fine if using different auths
             */
            return false
        }

        val res0 = actionResults.find { it.sourceLocalId == first.getLocalId() } as RestCallResult?
            ?: return false
        val res1 = actionResults.find { it.sourceLocalId == second.getLocalId() } as RestCallResult?
            ?: return false

        //both must be 201 CREATE
        if(res0.getStatusCode() != 201 || res1.getStatusCode() != 201){
            return false
        }

        return true
    }


    class NonWorkingDeleteResult(
        val checkingDelete: Boolean = false,
        val nonWorking: Boolean = false,
        val name: String = "",
        val index: Int = -1
    )

    fun hasNonWorkingDelete(individual: RestIndividual,
                            actionResults: List<ActionResult>
    ) : NonWorkingDeleteResult {

        if(individual.size() < 3){
            return NonWorkingDeleteResult()
        }

        val actions = individual.seeMainExecutableActions()

        val before = actions[actions.size - 3]  // GET 2xx
        val delete = actions[actions.size - 2]  // DELETE 2xx
        val after = actions[actions.size - 1]   // GET 2xx

        //check verbs
        if(before.verb != HttpVerb.GET || delete.verb != HttpVerb.DELETE ||  after.verb != HttpVerb.GET) {
            return NonWorkingDeleteResult()
        }

        //check path resolution
        if(!before.usingSameResolvedPath(delete) || !after.usingSameResolvedPath(delete)) {
            return NonWorkingDeleteResult()
        }

        val res0 = actionResults.find { it.sourceLocalId == before.getLocalId() } as RestCallResult?
            ?: return NonWorkingDeleteResult()
        val res1 = actionResults.find { it.sourceLocalId == delete.getLocalId() } as RestCallResult?
            ?: return NonWorkingDeleteResult()
        val res2 = actionResults.find { it.sourceLocalId == after.getLocalId() } as RestCallResult?
            ?: return NonWorkingDeleteResult()

        // GET followed by DELETE, both 2xx, so working fine
        val checkingDelete = StatusGroup.G_2xx.allInGroup(res0.getStatusCode(), res1.getStatusCode())
        // all fine, but repeated GET after DELETE wrongly returns 2xx with data, meaning DELETE didn't delete
        val nonWorking: Boolean = checkingDelete && StatusGroup.G_2xx.allInGroup(res2.getStatusCode())
                && !res2.getBody().isNullOrEmpty()

        return NonWorkingDeleteResult(checkingDelete, nonWorking, delete.getName(), actions.size - 2)
    }

    fun hasSideEffectFailedModification(individual: RestIndividual,
                             actionResults: List<ActionResult>
    ): Boolean{

        if(individual.size() < 3){
            return false
        }

        val actions = individual.seeMainExecutableActions()

        val before  = actions[actions.size - 3]  // GET (before state)
        val modify  = actions[actions.size - 2]  // PUT or PATCH (failed modification)
        val after   = actions[actions.size - 1]  // GET (after state)

        // check verbs: GET, PUT|PATCH, GET
        if(before.verb != HttpVerb.GET) {
            return false
        }
        if(modify.verb != HttpVerb.PUT && modify.verb != HttpVerb.PATCH) {
            return false
        }
        if(after.verb != HttpVerb.GET) {
            return false
        }

        // all three must be on the same resolved path
        if(!before.usingSameResolvedPath(modify) || !after.usingSameResolvedPath(modify)) {
            return false
        }

        // the two GETs must use the same auth so the state comparison is meaningful.
        if(before.auth.isDifferentFrom(after.auth)) {
            return false
        }

        val resBefore = actionResults.find { it.sourceLocalId == before.getLocalId() } as RestCallResult?
            ?: return false
        val resModify = actionResults.find { it.sourceLocalId == modify.getLocalId() } as RestCallResult?
            ?: return false
        val resAfter = actionResults.find { it.sourceLocalId == after.getLocalId() } as RestCallResult?
            ?: return false

        // before GET must be 2xx
        if(!StatusGroup.G_2xx.isInGroup(resBefore.getStatusCode())) {
            return false
        }

        // PUT/PATCH must have failed with 4xx
        if(!StatusGroup.G_4xx.isInGroup(resModify.getStatusCode())) {
            return false
        }

        // after GET must be 2xx
        if(!StatusGroup.G_2xx.isInGroup(resAfter.getStatusCode())) {
            return false
        }

        val bodyBefore = resBefore.getBody()
        val bodyModify = extractRequestBody(modify)
        val bodyAfter = resAfter.getBody()

        // if both are null/empty, no side-effect detected
        if(bodyBefore.isNullOrEmpty() && bodyAfter.isNullOrEmpty()) {
            return false
        }

        // extract the field names sent in the PUT/PATCH request body
        val modifiedFieldNames = extractModifiedFieldNames(modify)

        // if we can identify specific fields, compare only those to avoid false positives from timestamps etc.
        if(modifiedFieldNames.isNotEmpty()
            && !bodyBefore.isNullOrEmpty()
            && !bodyAfter.isNullOrEmpty()
            && !bodyModify.isNullOrEmpty()) {
            return hasChangedModifiedFields(bodyBefore, bodyAfter, bodyModify, modifiedFieldNames)
        }

        return false
    }

    private fun extractRequestBody(modify: RestCallAction): String? {
        val bodyParam = modify.parameters.find { it is BodyParam } as BodyParam?
            ?: return null
        return bodyParam.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON)
    }

    /**
     * Checks the special K==404 side-effect pattern:
     *
     *   GET       /path  → 404   (resource does not exist before the call)
     *   PUT|PATCH /path  → 404   (failed modification - resource still not found)
     *   GET       /path  → ???   (should STILL be 404; anything else is a side-effect)
     */
    fun hasSideEffectIn404Modification(
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ): Boolean {

        if(individual.size() < 3) return false

        val actions = individual.seeMainExecutableActions()
        val before = actions[actions.size - 3]  // GET  (should be 404)
        val modify = actions[actions.size - 2]  // PUT or PATCH (should be 404)
        val after  = actions[actions.size - 1]  // GET  (oracle target)

        if(before.verb != HttpVerb.GET) return false
        if(modify.verb != HttpVerb.PUT && modify.verb != HttpVerb.PATCH) return false
        if(after.verb  != HttpVerb.GET) return false

        if(!before.usingSameResolvedPath(modify) || !after.usingSameResolvedPath(modify)) return false

        val resBefore = actionResults.find { it.sourceLocalId == before.getLocalId() } as RestCallResult?
            ?: return false
        val resModify = actionResults.find { it.sourceLocalId == modify.getLocalId() } as RestCallResult?
            ?: return false
        val resAfter  = actionResults.find { it.sourceLocalId == after.getLocalId()  } as RestCallResult?
            ?: return false

        if(resBefore.getStatusCode() != 404) return false
        if(resModify.getStatusCode() != 404) return false

        return resAfter.getStatusCode() != 404
    }

    /**
     * Extract field names from the PUT/PATCH request body.
     * These are the fields that the client attempted to modify.
     */
    private fun extractModifiedFieldNames(modify: RestCallAction): Set<String> {

        val bodyParam = modify.parameters.find { it is BodyParam } as BodyParam?
            ?: return emptySet()

        val gene = bodyParam.primaryGene()
        val objectGene = gene.getWrappedGene(ObjectGene::class.java) as ObjectGene?
            ?: if (gene is ObjectGene) gene else null

        if(objectGene == null){
            return emptySet()
        }

        return objectGene.fields.map { it.name }.toSet()
    }

    /**
     * Compares only the fields that were sent in the PUT/PATCH request.
     * Returns true if any of those fields changed between the before and after GET responses.
     *
     * NOTE: This only works when the request payload is a JSON object that directly
     * matches the resource structure. It does NOT support operation-based payloads
     * such as JSON Patch (RFC 6902).
     */
    internal fun hasChangedModifiedFields(
        bodyBefore: String,
        bodyAfter: String,
        bodyModify: String,
        fieldNames: Set<String>
    ): Boolean {

        try {
            val jsonBefore = JsonParser.parseString(bodyBefore)
            val jsonAfter = JsonParser.parseString(bodyAfter)
            val jsonModify = JsonParser.parseString(bodyModify)

            if(!jsonBefore.isJsonObject || !jsonAfter.isJsonObject || !jsonModify.isJsonObject){
                return false
            }

            val objBefore = jsonBefore.asJsonObject
            val objAfter = jsonAfter.asJsonObject
            val objModify = jsonModify.asJsonObject

            for(field in fieldNames){
                val valueBefore = objBefore.get(field)
                val valueAfter = objAfter.get(field)
                val valueModify = objModify.get(field)

                if(valueBefore != valueAfter && valueModify == valueAfter){
                    return true
                }
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }
}
