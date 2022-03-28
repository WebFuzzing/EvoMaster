package org.evomaster.core.database
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult

/**
 * sql insert action execution result
 */
class DbActionResult : ActionResult {

    constructor(stopping: Boolean = false) : super(stopping)
    constructor(other: DbActionResult): super(other)

    companion object{
        const val INSERT_SQL_EXECUTE_SUCCESSFULLY = "INSERT_SQL_EXECUTE_SUCCESSFULLY"
    }

    override fun copy(): DbActionResult {
        return DbActionResult(this)
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
        return action is DbAction
    }
}