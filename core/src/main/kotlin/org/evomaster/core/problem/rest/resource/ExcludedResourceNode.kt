package org.evomaster.core.problem.rest.resource

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.search.service.Randomness

/**
 * resource node which contains actions that are not part of action clusters
 */
class ExcludedResourceNode(
    path : RestPath,
    actions: MutableList<RestCallAction>
) : RestResourceNode(path, actions, InitMode.NONE, false) {

    init {
        init()
    }


    // disable param info update with additional info
    override fun updateActionsWithAdditionalParams(action: RestCallAction){
        // do nothing
    }

    // disable its value binding with others
    override fun getPossiblyBoundParams(actionTemplate: String, withSql : Boolean, randomness: Randomness?) : List<ParamInfo> = listOf()
}