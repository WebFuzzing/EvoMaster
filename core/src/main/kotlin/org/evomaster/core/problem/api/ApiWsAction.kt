package org.evomaster.core.problem.api

import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.action.Action

/**
 * an action for handling API
 */
abstract class ApiWsAction(
    /**
     * auth info
     */
    open val auth: AuthenticationInfo,
    /**
     * a list of param could be manipulated by evomaster
     */
    parameters: List<Param>
) : Action(parameters){

    val parameters : List<Param>
        get() { return children as List<Param>}

    fun addParam(param: Param){
        addChild(param)
    }

    /**
     * In some very special cases, we want to skip creating assertions on response bodies from the API
     */
    open fun shouldSkipAssertionsOnResponseBody() : Boolean{
        return false
    }

}