package org.evomaster.resource.rest.generator.template

/**
 * created by manzh on 2019-08-15
 */
open class Tag (val content : String){

    open fun getText(args : Map<String, String> = mapOf()) : String {
        if (args.isEmpty()) return content
        return "$content(${args.map { if (validateParams(it.key)){if (withoutQuotation(it.key)) "${it.key} = ${it.value}" else "${it.key} = \"${it.value}\""} else throw IllegalArgumentException("invalid args") }.joinToString(",")})"
    }

    open fun validateParams(param : String) : Boolean = true

    open fun withoutQuotation(param: String) : Boolean = false
}