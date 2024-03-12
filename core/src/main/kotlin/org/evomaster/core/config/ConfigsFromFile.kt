package org.evomaster.core.config

import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.auth.LoginEndpointDto

class ConfigsFromFile {

    var configs = mutableMapOf<String,String>()

    var auth = mutableListOf<AuthenticationDto>()

    var authTemplate: AuthenticationDto? = null

    fun validateAndNormalizeAuth(){

        if(authTemplate != null){
            normalize()
        }

        validate()
    }

    private fun normalize() {

        val t = authTemplate!!

        t.fixedHeaders.forEach { h ->
            auth.forEach { a ->
                if(a.fixedHeaders.none { it.name == h.name }){
                    a.fixedHeaders.add(h)
                }
            }
        }

        if(t.loginEndpointAuth != null){
            val template = t.loginEndpointAuth!!
            auth.filter { it.loginEndpointAuth!=null }
                    .forEach { a ->
                        val base = a.loginEndpointAuth!!
                        applyMissingFields(template, base, LoginEndpointDto::class.java)
                    }
        }
    }

    private fun <T> applyMissingFields(template: T, base: T, klass: Class<T>){

        //TODO
        for(f in klass.declaredFields) {
            val t = f.get(template) ?: continue
            val b = f.get(base)
            if(b == null){
                TODO should check type and apply recursion if needed
            }
        }
    }

    private fun validate(){
        /*
            actually this is maybe not needed... as we do validation directly when instantiating the auth objects
         */
    }

}