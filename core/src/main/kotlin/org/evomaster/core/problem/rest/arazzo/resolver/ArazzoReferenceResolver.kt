package org.evomaster.core.problem.rest.arazzo.resolver

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.rest.arazzo.models.*

class ArazzoReferenceResolver(
    val components: Components?,
    val arazzoJsonNode: JsonNode?,
    val openApi: OpenAPI?
) {

    fun resolveSuccessReusable(items: List<SuccessReusable>?): List<SuccessAction>? {
        if (items == null) return null

        return items.map { item ->
            when (item) {
                is SuccessReusable.Success -> item.action
                is SuccessReusable.ReusableObj -> resolveReusableWithPrefix(item.reusable, "successActions") as SuccessAction
            }
        }
    }

    fun resolveFailureReusable(items: List<FailureReusable>?): List<FailureAction>? {
        if (items == null) return null

        return items.map { item ->
            when (item) {
                is FailureReusable.Failure -> item.action
                is FailureReusable.ReusableObj -> resolveReusableWithPrefix(item.reusable, "failureActions") as FailureAction
            }
        }
    }

    fun resolveParametersReusable(items: List<ParameterReusable>?): List<Parameter>? {
        if (items == null) return null

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

        val actionName = reference.name.removePrefix(prefixExpected + ".")
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