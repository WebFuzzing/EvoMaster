package org.evomaster.core.problem.rest

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