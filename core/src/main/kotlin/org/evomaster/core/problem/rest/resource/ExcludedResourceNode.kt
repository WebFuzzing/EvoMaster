package org.evomaster.core.problem.rest.resource

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestPath

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
    override fun getPossiblyBoundParams(actionTemplate: String, withSql : Boolean) : List<ParamInfo> = listOf()
}