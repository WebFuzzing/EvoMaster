package org.evomaster.core.problem.httpws.auth

import org.evomaster.core.problem.api.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.action.Action

/**
 * should be immutable
 *
 * AuthenticationInfo for http based SUT
 *
 */
open class HttpWsAuthenticationInfo(
    name: String,
    val headers: List<AuthenticationHeader>,
    val endpointCallLogin: EndpointCallLogin?
): AuthenticationInfo(name) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Blank name")
        }
        //FIXME "NoAuth"
        if(headers.isEmpty() && name != "NoAuth" && endpointCallLogin==null){
            throw IllegalArgumentException("Empty headers")
        }
    }

    /**
     * @return whether to exclude auth check (401 status code in the response) for the [action]
     */
    fun excludeAuthCheck(action: Action) : Boolean{
        if (action is RestCallAction && jsonTokenPostLogin != null){
            return action.getName() == "POST:${jsonTokenPostLogin.endpoint}"
        }
        return false
    }
}