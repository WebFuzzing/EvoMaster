package org.evomaster.core.problem.httpws.service

import org.evomaster.core.problem.api.service.ApiWsAction
import org.evomaster.core.problem.api.service.auth.AuthenticationInfo
import org.evomaster.core.problem.api.service.auth.NoAuth
import org.evomaster.core.problem.api.service.param.Param

abstract class HttpWsAction(
    auth: AuthenticationInfo = NoAuth(),
    parameters: MutableList<Param>
) : ApiWsAction(auth, parameters)