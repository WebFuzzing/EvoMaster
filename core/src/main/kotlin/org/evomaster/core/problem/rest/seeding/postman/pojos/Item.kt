package org.evomaster.core.problem.rest.seeding.postman.pojos

/**
 * Container for a Postman request (which in turn includes HTTP verb, path, param values, etc.)
 */
data class Item (
        val name: String,
        val request: Request
)