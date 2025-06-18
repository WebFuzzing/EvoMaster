package org.evomaster.core.output.naming.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.action.EvaluatedAction
import javax.ws.rs.core.MediaType

object RestNamingUtils {

    private const val EMPTY = "empty"
    private const val CONTENT = "content"
    private const val STRING = "string"
    private const val ROOT = "root"
    private const val LIST = "list"
    private const val OBJECT = "object"
    private const val ELEMENT = "element"

    fun getPath(nameQualifier: String): String {
        if (nameQualifier == "/") {
            return ROOT
        }
        return TestWriterUtils.safeVariableName(nameQualifier)
    }

    fun isGetCall(evaluatedAction: EvaluatedAction): Boolean {
        return (evaluatedAction.action as RestCallAction).verb == HttpVerb.GET
    }

    fun addBodyShape(result: RestCallResult): List<String> {
        val bodyString = result.getBody()
        if (bodyString.isNullOrBlank()) {
            return listOf(EMPTY)
        }
        val type = result.getBodyType()?: MediaType.TEXT_PLAIN_TYPE
        if (type.isCompatible(MediaType.APPLICATION_JSON_TYPE) || type.toString().toLowerCase().contains("+json")) {
            return handleJsonBody(bodyString)
        }
        return listOf(CONTENT)
    }

    private fun handleJsonBody(bodyString: String?): List<String> {
        val node = ObjectMapper().readTree(bodyString)
        return when {
            node.isArray -> getListBodyName(node)
            node.isObject -> getObjectBodyName(node)
            node.isTextual -> listOf(STRING)
            else -> listOf(CONTENT) // no particular knowledge about the content
        }
    }

    private fun getObjectBodyName(jsonNode: JsonNode): List<String> {
        return if (jsonNode.fields().hasNext()) listOf(OBJECT) else listOf(EMPTY, OBJECT)
    }

    private fun getListBodyName(jsonNode: JsonNode): List<String> {
        return when (val listSize = jsonNode.size()) {
            0 -> listOf(EMPTY, LIST)
            1 -> listOf("1", ELEMENT)
            else -> listOf(listSize.toString(), "${ELEMENT}s")
        }
    }

}
