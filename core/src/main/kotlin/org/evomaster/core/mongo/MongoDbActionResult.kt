package org.evomaster.core.mongo

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

/**
 * sql insert action execution result
 */
class MongoDbActionResult : ActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)
    constructor(other: MongoDbActionResult) : super(other)

    companion object {
        const val INSERT_MONGO_EXECUTE_SUCCESSFULLY = "INSERT_MONGO_EXECUTE_SUCCESSFULLY"
    }

    override fun copy(): MongoDbActionResult {
        return MongoDbActionResult(this)
    }

    /**
     * @param success specifies whether the INSERT MONGO executed successfully
     */
    fun setInsertExecutionResult(success: Boolean) =
        addResultValue(INSERT_MONGO_EXECUTE_SUCCESSFULLY, success.toString())

    /**
     * @return whether the MongoDB action executed successfully
     */
    fun getInsertExecutionResult() = getResultValue(INSERT_MONGO_EXECUTE_SUCCESSFULLY)?.toBoolean() ?: false

    override fun matchedType(action: Action): Boolean {
        return action is MongoDbAction
    }
}