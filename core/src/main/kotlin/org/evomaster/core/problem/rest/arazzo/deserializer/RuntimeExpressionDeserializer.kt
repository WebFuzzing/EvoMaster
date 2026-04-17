package org.evomaster.core.problem.rest.arazzo.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.evomaster.core.problem.rest.arazzo.models.RuntimeExpression
import org.evomaster.core.problem.rest.arazzo.parser.ExpressionParser

class RuntimeExpressionDeserializer : JsonDeserializer<RuntimeExpression>() {
    override fun deserialize(
        p0: JsonParser,
        p1: DeserializationContext
    ): RuntimeExpression {
        val expressionString = p0.text

        return try {
            ExpressionParser.parse(expressionString)
        } catch (e: Exception) {
            throw IllegalArgumentException("Arazzo Parsing Error: Invalid $expressionString", e)
        }
    }
}