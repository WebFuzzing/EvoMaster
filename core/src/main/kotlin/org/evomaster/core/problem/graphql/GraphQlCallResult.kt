package org.evomaster.core.problem.graphql

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.search.action.Action

class GraphQlCallResult : HttpWsCallResult {

    companion object{
        const val LAST_STATEMENT_WHEN_GQL_ERRORS = "LAST_STATEMENT_WHEN_GQL_ERRORS"
        private val mapper = ObjectMapper()
    }

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    @VisibleForTesting
    internal constructor(other: GraphQlCallResult) : super(other)


    override fun copy(): GraphQlCallResult {
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

    fun hasErrors(): Boolean {
        val errors = extractBodyInGraphQlResponse()?.findPath("errors") ?: return false

        return !errors.isEmpty || !errors.isMissingNode
    }

    fun hasNonEmptyData(): Boolean {
        val data = extractBodyInGraphQlResponse()?.findPath("data") ?: return false

        return !data.isEmpty || !data.isMissingNode
    }

    private fun extractBodyInGraphQlResponse(): JsonNode? {
        return try {
            getBody()?.run { mapper.readTree(getBody()) }
        } catch (e: JsonProcessingException) {
            null
        }
    }
}
