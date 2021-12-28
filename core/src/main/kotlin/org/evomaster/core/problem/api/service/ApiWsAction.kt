package org.evomaster.core.problem.api.service

import org.evomaster.core.problem.api.service.auth.AuthenticationInfo
import org.evomaster.core.problem.api.service.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.service.Randomness

abstract class ApiWsAction(
    open val auth: AuthenticationInfo,
    val parameters: MutableList<Param>
) : Action(parameters){

    override fun getChildren(): List<Param> = parameters

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, all: List<Action>) {
        seeGenes().forEach { it.randomize(randomness, forceNewValue) }
    }
}