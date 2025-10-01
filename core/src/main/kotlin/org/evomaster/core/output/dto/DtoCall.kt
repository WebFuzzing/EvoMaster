package org.evomaster.core.output.dto

/**
 * Holds the representation of the DTO being called. Providing a wrapper for the lines that are involved
 * in setting the payload and the variable name that will be set in the generated code.
 */
class DtoCall(
    /**
     * The variable name that will be used to set as payload
     */
    val varName: String,
    /**
     * List of all the statements that make up the payload being instantiated.
     */
    val objectCalls: List<String>
)
