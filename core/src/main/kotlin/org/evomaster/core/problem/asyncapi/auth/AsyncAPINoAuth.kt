package org.evomaster.core.problem.asyncapi.auth

import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.problem.enterprise.auth.NoAuth

/**
 * Default broker authentication for AsyncAPI black-box.  Broker SASL/TLS auth
 * is out of scope for the starter slice; this lets actions be constructed
 * without a hard auth dependency.
 */
class AsyncAPINoAuth : AuthenticationInfo("AsyncAPINoAuth"), NoAuth
