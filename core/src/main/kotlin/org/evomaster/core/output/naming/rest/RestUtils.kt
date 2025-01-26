package org.evomaster.core.output.naming.rest

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.search.action.EvaluatedAction
import javax.ws.rs.core.MediaType

object RestUtils {

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
        return when (bodyString?.trim()?.first()) {
            '[' -> getListBodyName(bodyString)
            '{' -> getObjectBodyName(bodyString)
            '"' -> listOf(STRING)
            else -> listOf(CONTENT) // no particular knowledge about the content
        }
    }

    private fun getObjectBodyName(bodyString: String?): List<String> {
        try {
            val objectSize = Gson().fromJson(bodyString, Map::class.java).size
            if (objectSize == 0) {
                return listOf(EMPTY, OBJECT)
            }
        } catch (_: JsonSyntaxException) {
            // If there's any exception, just default to the object name. Test assertion will be handled when writing the actual test
        }
        return listOf(OBJECT)
    }

    private fun getListBodyName(bodyString: String?): List<String> {
        return try {
            when (val listSize = Gson().fromJson(bodyString, List::class.java).size) {
                0 -> listOf(EMPTY, LIST)
                1 -> listOf("1", ELEMENT)
                else -> listOf(listSize.toString(), "${ELEMENT}s")
            }
        } catch (e: JsonSyntaxException) {
            // If there's any exception, just default to the list name. Test assertion will be handled when writing the actual test
            listOf(LIST)
        }
    }

}
