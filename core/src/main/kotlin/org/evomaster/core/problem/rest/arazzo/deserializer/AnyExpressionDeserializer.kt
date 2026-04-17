package org.evomaster.core.problem.rest.arazzo.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.evomaster.core.problem.rest.arazzo.models.AnyExpression
import org.evomaster.core.problem.rest.arazzo.parser.ExpressionParser

class AnyExpressionDeserializer : JsonDeserializer<AnyExpression>() {
    override fun deserialize(
        p0: JsonParser,
        p1: DeserializationContext
    ): AnyExpression {
        val node: JsonNode = p0.codec.readTree(p0)
        if (node.isTextual && node.asText().startsWith("$")) {
            return try {
                val parsedExpression = ExpressionParser.parse(node.asText())
                AnyExpression.Expression(parsedExpression)
            } catch (e: Exception) {
                throw IllegalArgumentException("Arazzo Parsing Error: Invalid ${node.asText()}", e)
            }
        }

        return AnyExpression.Constant(node)
    }
}