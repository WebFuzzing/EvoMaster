package org.evomaster.core.problem.rest.schema

data class SchemaLocation(
    val location: String,
    val type: SchemaLocationType
){

    companion object{
        val MEMORY = SchemaLocation("",SchemaLocationType.MEMORY)

        fun ofRemote(url: String) = SchemaLocation(url, SchemaLocationType.REMOTE)

        fun ofLocal(urlOrPath: String) = SchemaLocation(urlOrPath, SchemaLocationType.LOCAL)
    }

}