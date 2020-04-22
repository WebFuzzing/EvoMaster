package org.evomaster.resource.rest.generator.template

import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippet

/**
 * created by manzh on 2019-08-13
 */
interface MethodScript : ScriptTemplate{

    fun generateHeading(types : RegisterType) : String

    fun generateEnding(types : RegisterType) : String

    fun getParams() : Map<String, String>

    fun getBody() : List<String>

    fun getTags() : List<String> = listOf()

    fun getParamTag() : Map<String, String> = mutableMapOf()

    fun getReturn() : String? = null

    fun getInvocation(obj : String?, paramVars : List<String>) : String

    fun getComments() : List<String> = listOf()

    override fun generate(types: RegisterType) : String{
        getParams().values.forEach{types.validType(it)}
        getReturn()?.apply {
            types.validType(this)
        }

        val content = StringBuilder()

        getComments().forEach {
            if (!it.isNullOrBlank()){
                content.append(it)
                content.append(System.lineSeparator())
            }
        }

        getTags().forEach {
            if (!it.isNullOrBlank()){
                content.append(it)
                content.append(System.lineSeparator())
            }
        }
        content.append(generateHeading(types))
        content.append(System.lineSeparator())
        getBody().forEach {
            if (!it.isNullOrBlank()){
                content.append(it)
                content.append(System.lineSeparator())
            }
        }
        content.append(System.lineSeparator())
        content.append(generateEnding(types))
        return content.toString()
    }

    fun getIfSnippets() : List<IfSnippet>
}