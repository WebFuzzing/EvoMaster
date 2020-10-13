package org.evomaster.core.problem.rest.seeding.postman.pojos

data class Item (
        val name: String,
        val request: Request,
        val response: List<Any>
)