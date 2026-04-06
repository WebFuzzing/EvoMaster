package org.evomaster.core.problem.rest.arazzo.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.evomaster.core.problem.rest.arazzo.models.FailureAction
import org.evomaster.core.problem.rest.arazzo.models.FailureReusable
import org.evomaster.core.problem.rest.arazzo.models.Reusable

class FailureReusableDeserializer : JsonDeserializer<FailureReusable>() {
    override fun deserialize(
        p0: JsonParser,
        p1: DeserializationContext
    ): FailureReusable? {
        val node: JsonNode = p0.codec.readTree(p0)

        return if (node.has("reference")) {
            val reusable = p0.codec.treeToValue(node, Reusable::class.java)
            FailureReusable.ReusableObj(reusable)
        } else {
            val action = p0.codec.treeToValue(node, FailureAction::class.java)
            FailureReusable.Inline(action)
        }
    }
}