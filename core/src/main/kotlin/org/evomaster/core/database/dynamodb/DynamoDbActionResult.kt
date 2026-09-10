package org.evomaster.core.database.dynamodb

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

/** Result of executing a [DynamoDbAction]. */
class DynamoDbActionResult : ActionResult {

    /** Creates a result for the action identified by [sourceLocalId]. */
    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    /** Creates a copy of another DynamoDB action result. */
    constructor(other: DynamoDbActionResult) : super(other)

    companion object {
        const val INSERT_DYNAMODB_EXECUTE_SUCCESSFULLY = "INSERT_DYNAMODB_EXECUTE_SUCCESSFULLY"
    }

    /** Creates an independent copy of this result. */
    override fun copy(): DynamoDbActionResult = DynamoDbActionResult(this)

    /** Records whether the insertion completed successfully. */
    fun setInsertExecutionResult(success: Boolean) =
        addResultValue(INSERT_DYNAMODB_EXECUTE_SUCCESSFULLY, success.toString())

    /** Returns whether the insertion completed successfully. */
    fun getInsertExecutionResult(): Boolean =
        getResultValue(INSERT_DYNAMODB_EXECUTE_SUCCESSFULLY)?.toBoolean() ?: false

    /** Returns whether [action] is a DynamoDB insertion action. */
    override fun matchedType(action: Action): Boolean = action is DynamoDbAction
}
