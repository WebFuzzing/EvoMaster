package org.evomaster.core.database.neo4j

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

/** Result of executing a [Neo4jDbAction]. */
class Neo4jDbActionResult : ActionResult {

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    constructor(other: Neo4jDbActionResult) : super(other)

    companion object {
        const val INSERT_NEO4J_EXECUTE_SUCCESSFULLY = "INSERT_NEO4J_EXECUTE_SUCCESSFULLY"
    }

    override fun copy(): Neo4jDbActionResult = Neo4jDbActionResult(this)

    /**
     * @param success whether every node and relationship of the action was inserted
     */
    fun setInsertExecutionResult(success: Boolean) =
        addResultValue(INSERT_NEO4J_EXECUTE_SUCCESSFULLY, success.toString())

    fun getInsertExecutionResult(): Boolean =
        getResultValue(INSERT_NEO4J_EXECUTE_SUCCESSFULLY)?.toBoolean() ?: false

    override fun matchedType(action: Action): Boolean = action is Neo4jDbAction
}
