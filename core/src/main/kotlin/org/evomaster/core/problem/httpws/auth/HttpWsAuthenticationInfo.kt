package org.evomaster.core.problem.httpws.auth

import org.evomaster.core.problem.api.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.Action

/**
 * should be immutable
 *
 * AuthenticationInfo for http based SUT
 *
 */
open class HttpWsAuthenticationInfo(
    name: String,
    val headers: List<AuthenticationHeader>,
    val cookieLogin: CookieLogin?,
    val jsonTokenPostLogin: JsonTokenPostLogin?): AuthenticationInfo(name) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Blank name")
        }
        if(headers.isEmpty() && name != "NoAuth" && cookieLogin==null && jsonTokenPostLogin==null){
            throw IllegalArgumentException("Empty headers")
        }
        if(cookieLogin != null && jsonTokenPostLogin != null){
            //TODO maybe in future might want to support both...
            throw IllegalArgumentException("Specified both Cookie and Token based login. Choose just one.")
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