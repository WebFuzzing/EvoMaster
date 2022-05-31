package org.evomaster.core.problem.api.service

import org.evomaster.core.problem.api.service.auth.AuthenticationInfo
import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.service.Randomness

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

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, all: List<Action>) {
        seeGenes().forEach {
            //TODO should refactor name
            it.doInitialize(randomness)
        //    it.randomize(randomness, forceNewValue)
        }
    }
}