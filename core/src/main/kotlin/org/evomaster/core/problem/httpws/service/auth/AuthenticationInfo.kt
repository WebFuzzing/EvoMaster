package org.evomaster.core.problem.httpws.service.auth

//should be immutable

open class AuthenticationInfo(
        val name: String,
        val headers: List<AuthenticationHeader>,
        val cookieLogin: CookieLogin?,
        val jsonTokenPostLogin: JsonTokenPostLogin?) {

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
}