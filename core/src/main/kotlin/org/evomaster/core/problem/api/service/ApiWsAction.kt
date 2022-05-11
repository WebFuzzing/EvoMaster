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
    val parameters: MutableList<Param>
) : Action(parameters){

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, all: List<Action>) {
        seeGenes().forEach { it.randomize(randomness, forceNewValue) }
    }
}