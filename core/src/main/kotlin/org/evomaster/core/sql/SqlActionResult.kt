package org.evomaster.core.sql
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

/**
 * sql insert action execution result
 */
class SqlActionResult : ActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)
    constructor(other: SqlActionResult): super(other)

    companion object{
        const val INSERT_SQL_EXECUTE_SUCCESSFULLY = "INSERT_SQL_EXECUTE_SUCCESSFULLY"
    }

    override fun copy(): SqlActionResult {
        return SqlActionResult(this)
    }

    /**
     * @param success specifies whether the INSERT SQL executed successfully
     *
     * NOTE THAT here for SELECT, the execution result is false by default.
     */
    fun setInsertExecutionResult(success: Boolean) = addResultValue(INSERT_SQL_EXECUTE_SUCCESSFULLY, success.toString())

    /**
     * @return whether the db action executed successfully
     */
    fun getInsertExecutionResult() = getResultValue(INSERT_SQL_EXECUTE_SUCCESSFULLY)?.toBoolean()?:false

    override fun matchedType(action: Action): Boolean {
        return action is SqlAction
    }
}