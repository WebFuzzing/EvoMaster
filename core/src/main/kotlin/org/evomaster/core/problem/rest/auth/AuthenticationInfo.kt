package org.evomaster.core.problem.rest.auth

//should be immutable

open class AuthenticationInfo(val name: String, val headers: List<AuthenticationHeader>) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Blank name")
        }
        if(headers.isEmpty() && name != "NoAuth"){
            throw IllegalArgumentException("Empty headers")
        }
    }
}