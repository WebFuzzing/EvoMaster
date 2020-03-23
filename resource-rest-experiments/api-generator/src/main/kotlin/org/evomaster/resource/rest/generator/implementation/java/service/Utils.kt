package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.model.RestMethod

/**
 * created by manzh on 2019-12-19
 */
object Utils {

    fun generateRestMethodName(method : RestMethod, resource : String) : String{
        return when(method){
            RestMethod.POST_VALUE ->"create${resource}ByValues"
            RestMethod.POST, RestMethod.POST_ID -> "create$resource"
            RestMethod.GET_ID ->  "get${resource}ById"
            RestMethod.GET_ALL -> "getAll$resource"
            RestMethod.GET_ALL_CON -> "getAllWithCondition$resource"
            RestMethod.DELETE -> "delete$resource"
            RestMethod.DELETE_CON -> "deleteWithCondition$resource"
            //RestMethod.PATCH -> "update$resource"
            RestMethod.PATCH_VALUE -> "update${resource}ByValues"
            RestMethod.PUT -> "createOrUpdate$resource"
        }
    }
}