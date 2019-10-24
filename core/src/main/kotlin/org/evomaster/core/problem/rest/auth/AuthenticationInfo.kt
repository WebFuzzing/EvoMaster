package org.evomaster.core.problem.rest.auth

//should be immutable

open class AuthenticationInfo(
        val name: String,
        val headers: List<AuthenticationHeader>,
        val cookieLogin: CookieLogin?) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Blank name")
        }
        if(headers.isEmpty() && name != "NoAuth" && cookieLogin==null){
            throw IllegalArgumentException("Empty headers")
        }
    }
}