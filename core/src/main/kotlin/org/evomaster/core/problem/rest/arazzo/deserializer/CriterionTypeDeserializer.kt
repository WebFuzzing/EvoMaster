package org.evomaster.core.problem.rest.arazzo.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.evomaster.core.problem.rest.arazzo.models.CriterionExpression
import org.evomaster.core.problem.rest.arazzo.models.CriterionType

class CriterionTypeDeserializer : JsonDeserializer<CriterionType>() {
    override fun deserialize(
        p0: JsonParser,
        p1: DeserializationContext
    ): CriterionType {
        val node: JsonNode = p0.codec.readTree(p0)
        return when {
            node.isTextual ->  CriterionType.Simple(node.asText())
            node.isObject -> {
                val complexObj = p0.codec.treeToValue(node, CriterionExpression::class.java)
                CriterionType.Complex(complexObj)
            }
            else -> throw IllegalArgumentException("Arazzo Parsing Error: Invalid ${node.nodeType}. Expected string or Criterion Expression")
        }
    }
}