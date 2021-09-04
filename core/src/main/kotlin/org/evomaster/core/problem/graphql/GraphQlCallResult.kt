package org.evomaster.core.problem.graphql

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult

class GraphQlCallResult : HttpWsCallResult {

    companion object{
        const val LAST_STATEMENT_WHEN_GQL_ERRORS = "LAST_STATEMENT_WHEN_GQL_ERRORS"
    }

    constructor(stopping: Boolean = false) : super(stopping)

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
