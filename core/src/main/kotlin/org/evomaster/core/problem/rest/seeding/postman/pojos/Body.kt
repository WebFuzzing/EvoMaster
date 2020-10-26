package org.evomaster.core.problem.rest.seeding.postman.pojos

data class Body (
        val mode: String,
        val raw: String?,
        val urlencoded: List<Urlencoded>?
)