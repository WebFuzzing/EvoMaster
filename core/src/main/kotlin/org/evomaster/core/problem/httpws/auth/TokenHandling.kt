package org.evomaster.core.problem.httpws.auth

class TokenHandling (
    /**
     * How to extract the token from a JSON response, as such
     * JSON could have few fields, possibly nested.
     * It is expressed as a JSON Pointer
     */
    val extractFromField: String,


    /**
     * When sending a token in an HTTP header, specify to which header to add it (e.g., "Authorization")
     */
    val httpHeaderName: String,

    /**
     * When sending out the obtained token in an HTTP header,
     * specify if there should be any prefix (e.g., "Bearer " or "JWT ").
     * If needed, make sure it has trailing space(s).
     */
    val headerPrefix: String
)