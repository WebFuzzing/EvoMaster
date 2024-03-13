package org.evomaster.core.config

import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.auth.LoginEndpointDto
import java.lang.reflect.Field
import java.lang.reflect.Modifier

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

    private fun applyMissingFields(template: Any, base: Any, klass: Class<*>){

        for(f in klass.declaredFields) {
            if(Modifier.isStatic(f.modifiers) || Modifier.isTransient(f.modifiers)){
                continue
            }
            val t = f.get(template) ?: continue
            val b = f.get(base)

            if(isNestedObject(f)){
                if(b == null){
                    //WARNING: ideally should make a copy, instead of shared reference.
                    //but, as in theory not modifying these DTOs, and just convert them into immutable instances,
                    //hopefully ll not be a problem
                    f.set(base,t)
                } else {
                    applyMissingFields(t,b,f.type)
                }
            } else {
                if(b==null){
                    //here there would be no need of a copy, as immutable
                    f.set(base,t)
                }
            }
        }
    }

    private fun isNestedObject(field : Field) : Boolean{

        val type = field.type

        if(java.lang.Boolean::class.java.isAssignableFrom(type)
            || java.lang.Boolean.TYPE == type
            || java.lang.String::class.java.isAssignableFrom(type)
            || java.lang.Number::class.java.isAssignableFrom(type)
            || type.isPrimitive
            || type.isEnum
            ){
            return false
        }
        //TODO should check for arrays and collections, if wants this method more complete,
        // although not necessary at the moment

        return true
    }

    private fun validate(){
        /*
            actually this is maybe not needed... as we do validation directly when instantiating the auth objects.
            so should only do checks here that would be lost after conversion
         */
        if(authTemplate!=null && authTemplate!!.name != null){
            throw IllegalArgumentException("Cannot specify the 'name' in the auth template")
        }
    }

}