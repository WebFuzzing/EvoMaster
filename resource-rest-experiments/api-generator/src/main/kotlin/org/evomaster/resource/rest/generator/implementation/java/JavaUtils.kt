package org.evomaster.resource.rest.generator.implementation.java

/**
 * created by manzh on 2019-12-20
 */
object JavaUtils {

    fun getSingleComment(content : String) : String = "//$content"

    fun getMultipleComment(list: List<String>, isDoc :Boolean = false) =
            """
                /**
                ${list.joinToString(System.lineSeparator()) { if (isDoc) "*$it" else it }}
                */ 
            """.trimIndent()
}