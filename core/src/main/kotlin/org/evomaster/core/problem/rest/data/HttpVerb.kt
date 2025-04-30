package org.evomaster.core.problem.rest.data

import io.swagger.models.HttpMethod


enum class HttpVerb {


    GET,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    PATCH,
    TRACE,
    HEAD;

    fun isWriteOperation() : Boolean{
        return this == POST || this == PUT || this == DELETE || this == PATCH
    }

    fun isReadOperation() = !isWriteOperation()

    companion object {

        /**
         * Out of set {PUT,DELETE,PATCH}, give other 2 values different from input.
         *
         * Note: this is ignoring POST
         */
         fun otherWriteOperationsOnSameResourcePath(verb: HttpVerb) : List<HttpVerb>{
            val write = listOf(PUT, DELETE, PATCH)
            if(!write.contains(verb)){
                throw IllegalArgumentException("Not valid verb: $verb")
            }
            return write.filter { it != verb }
        }

        fun from(method: HttpMethod): HttpVerb {
            return when (method) {
                HttpMethod.GET -> GET
                HttpMethod.POST -> POST
                HttpMethod.PUT -> PUT
                HttpMethod.DELETE -> DELETE
                HttpMethod.OPTIONS -> OPTIONS
                HttpMethod.PATCH -> PATCH
                HttpMethod.HEAD -> HEAD
                else -> throw IllegalArgumentException("Cannot handle method $method")
            }
        }
    }
}