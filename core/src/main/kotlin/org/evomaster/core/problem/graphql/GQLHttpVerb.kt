package org.evomaster.core.problem.graphql

import io.swagger.models.HttpMethod

enum class GQLHttpVerb {


    GET,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    PATCH,
    TRACE,
    HEAD;

    companion object {
        //todo not sure
        fun from(method: HttpMethod): GQLHttpVerb {
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