package org.evomaster.core.problem.rest.arazzo.models

sealed class Source {
    data class Header(val token: String) : Source()
    data class Query(val name: String) : Source()
    data class Path(val name: String) : Source()
    data class Body(val jsonPointer: String? = null) : Source()
}