package org.evomaster.core.problem.asyncapi.auth

import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.problem.enterprise.auth.NoAuth

/**
 * An AsyncAPI action with no authentication set up.
 *
 * Authentication in AsyncAPI is a property of the connection to the broker rather than of an
 * individual message: a security scheme is declared on a server, and the client is already
 * authenticated by the time a message is published. So there is nothing per-action to vary
 * yet, and every action carries this.
 */
class AsyncApiNoAuth : AuthenticationInfo(NoAuth.NAME), NoAuth
