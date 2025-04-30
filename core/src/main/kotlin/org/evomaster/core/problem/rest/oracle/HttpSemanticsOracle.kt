package org.evomaster.core.problem.rest.oracle

import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.search.action.ActionResult

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
}