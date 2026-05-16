package org.evomaster.core.problem.asyncapi.service.fitness

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-function coverage of [AbstractAsyncAPIFitness.outputFieldPresenceTargets]
 * — the per-field presence ladder applied to messages captured on a
 * SUT-produced channel (M9-PR4 output-observation oracle).
 */
class AsyncAPIOutputFieldPresenceTargetsTest {

    private val mapper = ObjectMapper()

    @Test
    fun emitsPresenceTargetForEveryDeclaredProperty() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "properties": {
                "orderId":   { "type": "string" },
                "status":    { "type": "string" },
                "cancelReason": { "type": "string" }
              }
            }
        """.trimIndent())
        val payload = mapper.readTree("""{ "orderId": "o-1", "status": "OK" }""")

        val targets = AbstractAsyncAPIFitness.outputFieldPresenceTargets(
            action = subscribeOutputAction(),
            messageId = "OrderCreated",
            payload = payload,
            variantSchema = variantSchema
        )

        assertTrue(targets.contains("OUTPUT_FIELD_PRESENCE:orders.events:OrderCreated:orderId=present"))
        assertTrue(targets.contains("OUTPUT_FIELD_PRESENCE:orders.events:OrderCreated:status=present"))
        assertTrue(targets.contains("OUTPUT_FIELD_PRESENCE:orders.events:OrderCreated:cancelReason=absent"))
    }

    @Test
    fun nonObjectPayloadEmitsNothing() {
        val variantSchema = mapper.readTree("""
            { "type": "object", "properties": { "x": { "type": "string" } } }
        """.trimIndent())
        val payload = mapper.readTree(""""bare-string"""")

        assertEquals(
            emptyList<String>(),
            AbstractAsyncAPIFitness.outputFieldPresenceTargets(
                subscribeOutputAction(), "X", payload, variantSchema
            )
        )
    }

    @Test
    fun schemaWithoutPropertiesEmitsNothing() {
        val variantSchema = mapper.readTree("""{ "type": "object" }""")
        val payload = mapper.readTree("""{ "anything": 1 }""")

        assertEquals(
            emptyList<String>(),
            AbstractAsyncAPIFitness.outputFieldPresenceTargets(
                subscribeOutputAction(), "X", payload, variantSchema
            )
        )
    }

    private fun subscribeOutputAction(): AsyncAPIAction = AsyncAPIAction(
        operationName = "emitOrderEvents",
        channelAddress = "orders.events",
        channelName = "orders.events",
        kind = AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT,
        pairId = "out",
        messageId = "OrderCreated",
        additionalReplyMessageIds = emptyList(),
        parameters = mutableListOf(),
        correlationHeaderName = null
    )
}
