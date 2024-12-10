package org.evomaster.core.problem.httpws

import org.evomaster.core.problem.api.ApiWsAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth

abstract class HttpWsAction(
    override var auth: HttpWsAuthenticationInfo = HttpWsNoAuth(),
    parameters: MutableList<Param>
) : ApiWsAction(auth, parameters)