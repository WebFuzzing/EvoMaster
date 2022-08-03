package org.evomaster.core.problem.httpws.service

import org.evomaster.core.problem.api.service.ApiWsAction
import org.evomaster.core.problem.httpws.service.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.service.auth.NoAuth
import org.evomaster.core.problem.api.service.param.Param

abstract class HttpWsAction(
    override var auth: HttpWsAuthenticationInfo = NoAuth(),
    parameters: MutableList<Param>,
    localId : String,
    dependentActions : MutableList<String>
) : ApiWsAction(auth, parameters, localId, dependentActions)