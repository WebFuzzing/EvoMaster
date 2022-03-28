package org.evomaster.core.problem.rest.seeding.postman.pojos

data class Url (
        val raw: String,
        val protocol: String,
        val host: List<String>,
        val path: List<String>,
        val query: List<Query>?
)