package org.evomaster.core.problem.rest.arazzo.parser

import org.evomaster.core.problem.rest.arazzo.models.RuntimeExpression
import org.evomaster.core.problem.rest.arazzo.models.Source

object ExpressionParser {
    private val tokenRegex = Regex("^[a-zA-Z0-9!#\\\$%&'*+\\-.^_`|~]+$")
    private val nameRegex = Regex("^[\\x21-\\x7E]+$")
    private val jsonPointerRegex = Regex("^(?:/(?:[^/~]|~0|~1)*)*$")

    private fun validateToken(token: String): String {
        if (!tokenRegex.matches(token)) {
            throw IllegalArgumentException("Arazzo Parsing Error: The 'token' [$token] must be a 'tchar' of ABNF.")
        }
        return token
    }

    private fun validateName(name: String): String {
        if (!nameRegex.matches(name)) {
            throw IllegalArgumentException("Arazzo Parsing Error: The 'name' [$name] must be a valid ABNF's character.")
        }
        return name
    }

    private fun validateJsonPointer(pointer: String): String {
        if (!jsonPointerRegex.matches(pointer)) {
            throw IllegalArgumentException("Arazzo Parsing Error: The 'json-pointer' [$pointer] must be a valid RFC 6901.")
        }
        return pointer
    }

    fun parse(input: String): RuntimeExpression {
        return when {
            input == "\$url" -> RuntimeExpression.Url
            input == "\$method" -> RuntimeExpression.Method
            input == "\$statusCode" -> RuntimeExpression.StatusCode

            input.startsWith("\$request.") -> RuntimeExpression.Request(parseSource(input.removePrefix("\$request.")))
            input.startsWith("\$response.") -> RuntimeExpression.Response(parseSource(input.removePrefix("\$response.")))

            input.startsWith("\$inputs.") -> RuntimeExpression.Inputs(validateName(input.removePrefix("\$inputs.")))
            input.startsWith("\$outputs.") -> RuntimeExpression.Outputs(validateName(input.removePrefix("\$outputs.")))
            input.startsWith("\$steps.") -> RuntimeExpression.Steps(validateName(input.removePrefix("\$steps.")))
            input.startsWith("\$workflows.") -> RuntimeExpression.Workflows(validateName(input.removePrefix("\$workflows.")))
            input.startsWith("\$sourceDescriptions.") -> RuntimeExpression.SourceDescriptions(validateName(input.removePrefix("\$sourceDescriptions.")))

            input.startsWith("\$components.parameters.") -> RuntimeExpression.ComponentParameters(validateName(input.removePrefix("\$components.parameters.")))
            input.startsWith("\$components.") -> RuntimeExpression.Components(validateName(input.removePrefix("\$components.")))

            else -> throw IllegalArgumentException("Arazzo Parsing Error: Expression '$input' is not recognize for Arazzo.")
        }
    }

    private fun parseSource(source: String): Source {
        return when {
            source.startsWith("header.") -> {
                val token = source.removePrefix("header.")
                Source.Header(validateToken(token))
            }
            source.startsWith("query.") -> {
                val name = source.removePrefix("query.")
                Source.Query(validateName(name))
            }
            source.startsWith("path.") -> {
                val name = source.removePrefix("path.")
                Source.Path(validateName(name))
            }
            source.startsWith("body") -> {
                if (source == "body") {
                    Source.Body(null)
                } else if (source.startsWith("body#")) {
                    val pointer = source.removePrefix("body#")
                    Source.Body(validateJsonPointer(pointer))
                } else {
                    throw IllegalArgumentException("Arazzo Parsing Error: Bad 'body' reference: $source")
                }
            }
            else -> throw IllegalArgumentException("Arazzo Parsing Error: Invalid source data: $source")
        }
    }
}