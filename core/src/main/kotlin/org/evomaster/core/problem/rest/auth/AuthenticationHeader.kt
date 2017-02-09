package org.evomaster.core.problem.rest.auth

//should be immutable

class AuthenticationHeader(val name: String, val value: String) {

    init {
        if(name.isBlank()){
            throw IllegalArgumentException("Blank name")
        }
        //can values be blank? maybe...
    }
}