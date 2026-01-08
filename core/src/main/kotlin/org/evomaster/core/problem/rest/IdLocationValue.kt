package org.evomaster.core.problem.rest

class IdLocationValue(
    /**
     * RFC6901 JSON Pointer for location in JSON document
     */
    val pointer: String,
    /**
     * Value in the node, read as text
     */
    val value: String
)