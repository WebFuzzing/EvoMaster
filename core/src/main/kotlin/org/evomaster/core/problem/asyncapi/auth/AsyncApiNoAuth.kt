package org.evomaster.core.problem.asyncapi.auth

import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.problem.enterprise.auth.NoAuth

/**
 * No authentication. In AsyncAPI, security is declared on the server and holds for the whole
 * connection, so there is nothing to vary per message yet.
 */
class AsyncApiNoAuth : AuthenticationInfo(NoAuth.NAME), NoAuth
