package org.evomaster.core.problem.rest.arazzo.resolver

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.rest.arazzo.models.Components
import org.evomaster.core.problem.rest.arazzo.models.FailureAction
import org.evomaster.core.problem.rest.arazzo.models.FailureReusable
import org.evomaster.core.problem.rest.arazzo.models.Parameter
import org.evomaster.core.problem.rest.arazzo.models.ParameterReusable
import org.evomaster.core.problem.rest.arazzo.models.Reusable
import org.evomaster.core.problem.rest.arazzo.models.RuntimeExpression
import org.evomaster.core.problem.rest.arazzo.models.SuccessAction
import org.evomaster.core.problem.rest.arazzo.models.SuccessReusable

class ArazzoReferenceResolver(
    val components: Components?,
    val arazzoJsonNode: JsonNode?,
    val openApi: OpenAPI?
) {

    fun resolveSuccessActions(items: List<SuccessReusable>?): List<SuccessAction> {
        if (items == null) return emptyList()

        return items.map { item ->
            when (item) {
                is SuccessReusable.Success -> item.action
                is SuccessReusable.ReusableObj -> resolveReusableWithPrefix(item.reusable, "successActions") as SuccessAction
            }
        }
    }

    fun resolveFailureActions(items: List<FailureReusable>?): List<FailureAction> {
        if (items == null) return emptyList()

        return items.map { item ->
            when (item) {
                is FailureReusable.Failure -> item.action
                is FailureReusable.ReusableObj -> resolveReusableWithPrefix(item.reusable, "failureActions") as FailureAction
            }
        }
    }

    fun resolveParameters(items: List<ParameterReusable>?): List<Parameter> {
        if (items == null) return emptyList()

        return items.map { item ->
            when (item) {
                is ParameterReusable.Param -> item.parameter
                is ParameterReusable.ReusableObj -> resolveReusableWithPrefix(item.reusable, "parameters") as Parameter
            }
        }
    }

    private fun resolveReusableWithPrefix(reusable: Reusable, prefixExpected: String) : Any {
        if (components == null) {
            throw IllegalArgumentException("Arazzo Parsing Error: Can't reference with no Components")
        }

        val reference = reusable.reference

        if (reference !is RuntimeExpression.Components) {
            throw IllegalArgumentException(
                "Arazzo Parsing Error: A reference to Components was expected."
            )
        }

        if (!reference.name.startsWith(prefixExpected)) {
            throw IllegalArgumentException(
                "Arazzo Parsing Error: Invalid reference (${reference.name}). Expected to point to '${prefixExpected}'"
            )
        }

        val actionName = reference.name.removePrefix(prefixExpected)
        val resolve = when(prefixExpected) {
            "successActions" -> components.successActions?.get(actionName)
            "failureActions" -> components.failureActions?.get(actionName)
            "parameters" -> components.parameters?.get(actionName)
            else -> null
        }

        if (resolve == null) {
            throw IllegalArgumentException(
                "Arazzo Parsing Error: The ${prefixExpected}: '$actionName' is not in the components."
            )
        }

        return resolve
    }

    private fun resolveJsonPointer(reference: String) : Schema<*>? {
        if (reference.startsWith("#/")) {
            return resolveJsonPointerLocal(reference)
        }
        return resolveJsonPointerExternal(reference)
    }

    private fun resolveJsonPointerLocal(reference: String) : Schema<*>? {
        if (arazzoJsonNode == null) {
            throw IllegalArgumentException("Arazzo Parsing Error: Can't reference with no Arazzo Document")
        }

        val jsonPointer = reference.substring(1)

        val result = arazzoJsonNode.at(jsonPointer)

        if (result.isMissingNode) {
            throw IllegalArgumentException("Arazzo Parsing Error: Can't reference '${reference}'")
        }

        return Json.mapper().convertValue(result, Schema::class.java)
    }

    private fun resolveJsonPointerExternal(reference: String) : Schema<*> {
        if (openApi == null) {
            throw IllegalArgumentException("Arazzo Parsing Error: Can't reference with no OpenApi Document")
        }

        val tokens = reference.split("#")
        if (tokens.size < 2) {
            throw IllegalArgumentException("Arazzo Parsing Error: Error reference (${reference}). '#' Is mandatory")
        }

        val jsonPointer = tokens[1]
        val expectedPrefix = "/components/schemas/"
        if (!jsonPointer.startsWith(expectedPrefix)) {
            throw IllegalArgumentException("Arazzo Parsing Error: Error reference (${reference}). \"/components/schemas/\" Is mandatory for references to OpenApi")
        }

        val schemaName = jsonPointer.removePrefix(expectedPrefix)
        val result = openApi.components?.schemas?.get(schemaName)
            ?: throw IllegalArgumentException("Arazzo Parsing Error: (${reference}) reference does not exist in the OpenApi document")

        return result
    }

}