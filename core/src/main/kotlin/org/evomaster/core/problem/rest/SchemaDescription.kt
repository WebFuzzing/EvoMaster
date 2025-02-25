package org.evomaster.core.problem.rest

class SchemaDescription {

    private val headers: MutableMap<String, String> = mutableMapOf()

    private val body: MutableMap<String, String> = mutableMapOf()

    fun addHeader(name: String, description: String) {
        headers[name] = description
    }

    fun addBody(name: String, description: String) {
        body[name] = description
    }

    fun getHeaderDescriptions() : MutableMap<String, String> {
        return headers
    }

    fun getBodyDescriptions() : MutableMap<String, String> {
        return body
    }
}
