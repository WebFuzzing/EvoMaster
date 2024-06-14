package org.evomaster.core.problem.graphql

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult

class GraphQlCallResult : HttpWsCallResult {

    companion object{
        const val LAST_STATEMENT_WHEN_GQL_ERRORS = "LAST_STATEMENT_WHEN_GQL_ERRORS"
    }

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    @VisibleForTesting
    internal constructor(other: ActionResult) : super(other)


    override fun copy(): ActionResult {
        return GraphQlCallResult(this)
    }

    fun setLastStatementWhenGQLErrors(statement: String){
        addResultValue(LAST_STATEMENT_WHEN_GQL_ERRORS, statement)
    }

    fun getLastStatementWhenGQLErrors() : String? = getResultValue(LAST_STATEMENT_WHEN_GQL_ERRORS)

    fun hasLastStatementWhenGQLError() = ! getLastStatementWhenGQLErrors().isNullOrBlank()

    override fun matchedType(action: Action): Boolean {
        return action is GraphQLAction
    }
}
