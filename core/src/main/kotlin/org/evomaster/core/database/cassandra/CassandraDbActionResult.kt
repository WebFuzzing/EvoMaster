package org.evomaster.core.database.cassandra

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

/**
 * Cassandra insert action execution result
 */
class CassandraDbActionResult : ActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)
    constructor(other: CassandraDbActionResult) : super(other)

    companion object {
        const val INSERT_CASSANDRA_EXECUTE_SUCCESSFULLY = "INSERT_CASSANDRA_EXECUTE_SUCCESSFULLY"
    }

    override fun copy(): CassandraDbActionResult {
        return CassandraDbActionResult(this)
    }

    /**
     * @param success specifies whether the INSERT CASSANDRA executed successfully
     */
    fun setInsertExecutionResult(success: Boolean) =
        addResultValue(INSERT_CASSANDRA_EXECUTE_SUCCESSFULLY, success.toString())

    /**
     * @return whether the Cassandra action executed successfully
     */
    fun getInsertExecutionResult() = getResultValue(INSERT_CASSANDRA_EXECUTE_SUCCESSFULLY)?.toBoolean() ?: false

    override fun matchedType(action: Action): Boolean {
        return action is CassandraDbAction
    }
}
