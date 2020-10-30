package org.evomaster.core.problem.rest.seeding.postman.pojos

data class Body (
        val mode: String, // Postman-encoding used for body, "raw" for e.g. text and JSON, "urlencoded" for form bodies
        val raw: String?, // Request body as a string, null if mode != "raw"
        val urlencoded: List<Urlencoded>? // Request body as key-value pairs, null if mode != "urlencoded"
)