package org.evomaster.core.problem.rest

import io.swagger.models.HttpMethod


enum class HttpVerb {


    GET,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    PATCH,
    HEAD;

    companion object {

        fun from(method: HttpMethod): HttpVerb {
            when (method) {
                HttpMethod.GET -> return GET
                HttpMethod.POST -> return POST
                HttpMethod.PUT -> return PUT
                HttpMethod.DELETE -> return DELETE
                HttpMethod.OPTIONS -> return OPTIONS
                HttpMethod.PATCH -> return PATCH
                HttpMethod.HEAD -> return HEAD
                else -> throw IllegalArgumentException("Cannot handle method $method")
            }
        }
    }
}