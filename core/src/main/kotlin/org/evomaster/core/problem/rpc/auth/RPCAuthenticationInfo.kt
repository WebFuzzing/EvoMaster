package org.evomaster.core.problem.rpc.auth

import org.evomaster.core.problem.api.service.auth.AuthenticationInfo

open class RPCAuthenticationInfo(
    name: String,
    /**
     * key of auth which would be used by driver to employ correct auth info
     * the key is based on index of auth specified in driver
     */
    val key: Int,
    val isGlobal: Boolean,
    val authInRequest: MutableMap<String, String>?,
    val endpoint: String?
): AuthenticationInfo(name)