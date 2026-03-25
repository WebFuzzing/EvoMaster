package org.evomaster.core.redis

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

/**
 * sql insert action execution result
 */
class RedisDbActionResult : ActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)
    constructor(other: RedisDbActionResult) : super(other)

    companion object {
        const val INSERT_REDIS_EXECUTE_SUCCESSFULLY = "INSERT_REDIS_EXECUTE_SUCCESSFULLY"
    }

    override fun copy(): RedisDbActionResult {
        return RedisDbActionResult(this)
    }

    /**
     * @param success specifies whether the INSERT REDIS executed successfully
     */
    fun setInsertExecutionResult(success: Boolean) =
        addResultValue(INSERT_REDIS_EXECUTE_SUCCESSFULLY, success.toString())

    /**
     * @return whether the REDIS action executed successfully
     */
    fun getInsertExecutionResult() = getResultValue(INSERT_REDIS_EXECUTE_SUCCESSFULLY)?.toBoolean() ?: false

    override fun matchedType(action: Action): Boolean {
        return action is RedisDbAction
    }
}