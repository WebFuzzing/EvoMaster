package org.evomaster.resource.rest.generator.implementation.java.service

/**
 * created by manzh on 2020-01-07
 */
data class IfSnippet(val snippet : String, val type: IfSnippetType, var line : Int = -1, var clazz : String = "", var methodName: String = ""){

    companion object{
        fun getHeader() : String = arrayOf("codeSnippet","branchType","className","methodName","lineAtOf").joinToString(",")
    }

    fun toCSV() : String = arrayOf("\"$snippet\"", type.toString(),clazz, methodName, line).joinToString(",")
}