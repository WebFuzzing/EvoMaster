package org.evomaster.resource.rest.generator.template

import org.evomaster.resource.rest.generator.model.CommonTypes

/**
 * created by manzh on 2019-08-20
 */
interface ConstantTypeScript {

    fun getCommonType(type : CommonTypes) : String

    fun getAllCommonTypes() : Map<CommonTypes, String>

    fun getTypes() : Map<String, String>

    fun getGenericTypes(list: List<String>) : Map<String, String> = mutableMapOf()
}