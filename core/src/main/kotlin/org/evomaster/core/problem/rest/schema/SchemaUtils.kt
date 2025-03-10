package org.evomaster.core.problem.rest.schema

object SchemaUtils {

    fun isLocalRef(sref: String) = sref.startsWith("#")

    fun extractLocation(sref: String) : String{
        if(!sref.contains("#")){
            throw IllegalArgumentException("Not a valid \$ref, as it contains no #: $sref")
        }
        return sref.substring(0, sref.indexOf("#"))
    }
}