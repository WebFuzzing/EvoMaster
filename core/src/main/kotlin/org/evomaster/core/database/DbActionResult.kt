package org.evomaster.core.database

import org.evomaster.core.search.ActionResult

/**
 *
 */
class DbActionResult(stopping: Boolean = false) : ActionResult(stopping) {

    companion object{
        const val INSERT_SQL_EXECUTE_SUCCESSFULLY = "INSERT_SQL_EXECUTE_SUCCESSFULLY"
    }

    fun setInsertExecutionResult(success: Boolean) = addResultValue(INSERT_SQL_EXECUTE_SUCCESSFULLY, success.toString())

    /**
     * @return whether the db action executed successfully
     */
    fun getInsertExecutionResult() = getResultValue(INSERT_SQL_EXECUTE_SUCCESSFULLY)?.toBoolean()?:false
}