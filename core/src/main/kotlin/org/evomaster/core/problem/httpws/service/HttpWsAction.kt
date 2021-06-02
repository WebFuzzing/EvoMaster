package org.evomaster.core.problem.httpws.service

import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.httpws.service.auth.NoAuth
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.StructuralElement

abstract class HttpWsAction(
        var auth: AuthenticationInfo = NoAuth(),
        val parameters: MutableList<Param>
) : Action(parameters){

        override fun getChildren(): List<Param> = parameters
}