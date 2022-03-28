package org.evomaster.core.problem.rest.seeding.postman.pojos

data class Request (
        val method: String,
        val header: List<Header>?,
        val url: Url,
        val body: Body?
)