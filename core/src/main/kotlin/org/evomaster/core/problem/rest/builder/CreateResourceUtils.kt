package org.evomaster.core.problem.rest.builder

import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction


/**
 * POST/PUT operations can create new resources.
 * What are the ids of these newly created resources?
 * Typically, 2 options:
 * 1) returned in a HTTP Location header
 * 2) in a field of body response
 * Either way, such info is dynamically generated, and it would not
 * be known before executing the test.
 *
 * Once a test is executed, the needed info to make such a decision will be
 * stored in [RestCallResult.HEURISTICS_FOR_CHAINED_LOCATION]
 */
object CreateResourceUtils {

    /**
     * Given two actions in sequence, [before] and [after], setup a creation link.
     * This means that the POST [before] is supposed to create a resource dynamically, which is then used
     * by [after].
     * eg:
     * before: POST   /products
     * after:  DELETE /products/{id}
     *
     * In case the two actions are on the same path, the [after] is linked to the creator of [before],
     * if any
     */
    fun linkDynamicCreateResource(
        before: RestCallAction,
        after: RestCallAction
    ) {
        if(before.verb != HttpVerb.POST && before.verb != HttpVerb.PUT){
            throw IllegalArgumentException("Before action is neither a POST nor a PUT. It is a ${before.verb}")
        }

        if (!before.path.isEquivalent(after.path)) {
            /*
                eg
                POST /x
                GET  /x/{id}
             */
            before.saveCreatedResourceLocation = true
            after.usePreviousLocationId = before.postLocationId()
        } else {
            /*
                eg
                POST /x
                POST /x/{id}/y
                GET  /x/{id}/y
                not going to save the position of last POST, as same as target

                however, might also be in the case of:
                PUT /x/{id}
                GET /x/{id}
             */
            before.saveCreatedResourceLocation = false

            // the target (eg GET) needs to use the location of first POST, or more correctly
            // the same location used for the last POST (in case there is a deeper chain)
            after.usePreviousLocationId = before.usePreviousLocationId
        }
    }


    /**
     * Check if two actions are on same resource.
     * This is not necessarily simple, as path resolution might depend on dynamic info
     * coming from previous actions (e.g., a POST create)
     */
    fun doesResolveToSamePath(a: RestCallAction, b: RestCallAction) : Boolean {

        if(a.usePreviousLocationId == null && b.usePreviousLocationId == null) {
            return a.resolvedOnlyPath() == b.resolvedOnlyPath()
        }

        if(a.usePreviousLocationId != b.usePreviousLocationId) {
            //different dynamic info
            return false
        }

        //TODO this might need more thinking... eg, how handled variables resolutions in chained calls???
        return true
    }
}