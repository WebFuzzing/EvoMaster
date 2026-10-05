package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.enterprise.EnterpriseActionResult

/**
 * Stores the outcome of executing a single [McpAction] during fitness evaluation.
 *
 * A result is considered an error when the MCP server sets `isError: true` in the tool-call
 * response, or when the fitness function catches an exception from the server.
 */
class McpCallResult : EnterpriseActionResult {

    companion object {
        const val IS_ERROR = "IS_ERROR"
    }

    constructor(sourceLocalId: String) : super(sourceLocalId)

    /**
     * Copy constructor. Delegates to the [EnterpriseActionResult] copy constructor
     * which propagates [stopping], [deathSentence], the results map, and detected faults.
     */
    private constructor(other: McpCallResult) : super(other)

    override fun copy(): McpCallResult = McpCallResult(this)

    fun setIsError(isError: Boolean) {
        addResultValue(IS_ERROR, isError.toString())
    }
}
