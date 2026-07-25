package org.evomaster.core.problem.rpc.auth

import org.evomaster.core.problem.enterprise.auth.NoAuth

/**
 * representing RPCAction without auth setup
 */
class RPCNoAuth : RPCAuthenticationInfo(NoAuth.NAME, true,  -1), NoAuth