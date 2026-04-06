package org.evomaster.core.problem.rest.arazzo.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.evomaster.core.problem.rest.arazzo.models.Reusable
import org.evomaster.core.problem.rest.arazzo.models.SuccessAction
import org.evomaster.core.problem.rest.arazzo.models.SuccessReusable

class SuccessReusableDeserializer : JsonDeserializer<SuccessReusable>() {
    override fun deserialize(
        p0: JsonParser,
        p1: DeserializationContext
    ): SuccessReusable? {
        val node: JsonNode = p0.codec.readTree(p0)

        return if (node.has("reference")) {
            val reusable = p0.codec.treeToValue(node, Reusable::class.java)
            SuccessReusable.ReusableObj(reusable)
        } else {
            val action = p0.codec.treeToValue(node, SuccessAction::class.java)
            SuccessReusable.Inline(action)
        }
    }
}