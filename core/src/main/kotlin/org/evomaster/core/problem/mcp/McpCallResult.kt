package org.evomaster.core.problem.mcp

import org.evomaster.core.search.action.ActionResult

class McpCallResult : ActionResult {

    companion object {
        const val IS_ERROR = "IS_ERROR"
    }

    constructor(sourceLocalId: String) : super(sourceLocalId)

    private constructor(other: McpCallResult) : super(other)

    override fun copy(): McpCallResult = McpCallResult(this)

    fun setIsError(isError: Boolean) {
        addResultValue(IS_ERROR, isError.toString())
    }

    fun getIsError(): Boolean = getResultValue(IS_ERROR)?.toBoolean() ?: false
}
