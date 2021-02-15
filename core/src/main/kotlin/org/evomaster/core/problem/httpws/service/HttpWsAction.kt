package org.evomaster.core.problem.httpws.service

import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.search.Action

abstract class HttpWsAction(
        var auth: AuthenticationInfo = NoAuth()
) : Action