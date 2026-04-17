package org.evomaster.core.problem.rest.arazzo.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.evomaster.core.problem.rest.arazzo.models.Parameter
import org.evomaster.core.problem.rest.arazzo.models.ParameterReusable
import org.evomaster.core.problem.rest.arazzo.models.Reusable

class ParameterReusableDeserializer : JsonDeserializer<ParameterReusable>() {
    override fun deserialize(
        p0: JsonParser,
        p1: DeserializationContext
    ): ParameterReusable? {
        val node: JsonNode = p0.codec.readTree(p0)

        return if (node.has("reference")) {
            val reusable = p0.codec.treeToValue(node, Reusable::class.java)
            ParameterReusable.ReusableObj(reusable)
        } else {
            val parameter = p0.codec.treeToValue(node, Parameter::class.java)
            ParameterReusable.Param(parameter)
        }
    }
}