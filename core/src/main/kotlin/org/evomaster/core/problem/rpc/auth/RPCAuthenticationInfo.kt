package org.evomaster.core.problem.rpc.auth

import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.search.action.Action

open class RPCAuthenticationInfo(
    name: String,
    /**
     * key of auth which would be used by driver to employ correct auth info
     * the key is based on index of auth specified in driver
     */
    val isGlobal: Boolean,
    val authIndex: Int
): AuthenticationInfo(name)