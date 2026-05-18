package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Confirms M9-PR4: every `action: RECEIVE` operation produces one
 * SUBSCRIBE_OUTPUT action (previously SEND ops were silently dropped).
 */
class AsyncAPISubscribeOutputBuilderTest {

    @Test
    fun receiveOperationProducesSubscribeOutputAction() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              inbound:
                address: orders.requests
                messages:
                  Req: { ${'$'}ref: '#/components/messages/Req' }
              outbound:
                address: orders.events
                messages:
                  Created: { ${'$'}ref: '#/components/messages/Created' }
            operations:
              consume:
                action: receive
                channel: { ${'$'}ref: '#/channels/inbound' }
              emit:
                action: send
                channel: { ${'$'}ref: '#/channels/outbound' }
                messages:
                  - { ${'$'}ref: '#/channels/outbound/messages/Created' }
            components:
              messages:
                Req: { payload: { type: object } }
                Created:
                  payload:
                    type: object
                    properties:
                      orderId: { type: string }
                    required: [orderId]
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val publish = built.operations["consume"]!!.first()
        assertEquals(AsyncAPIAction.Kind.PUBLISH, publish.kind)

        val subscribeOutputs = built.operations["emit"]
        assertNotNull(subscribeOutputs, "SEND operation should produce at least one action")
        assertEquals(1, subscribeOutputs!!.size, "SEND op should produce exactly one SUBSCRIBE_OUTPUT")
        val subOut = subscribeOutputs.first()
        assertEquals(AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT, subOut.kind)
        assertEquals("orders.events", subOut.channelAddress)
        assertEquals("Created", subOut.messageId)
        assertTrue(
            subOut.additionalReplyMessageIds.isEmpty(),
            "single-message channel should have empty additionalReplyMessageIds"
        )
    }

    @Test
    fun receiveOperationWithMultipleMessageVariantsCapturesAllIds() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              inbound:
                address: i
                messages:
                  Req: { ${'$'}ref: '#/components/messages/Req' }
              events:
                address: orders.events
                messages:
                  Created: { ${'$'}ref: '#/components/messages/Created' }
                  Cancelled: { ${'$'}ref: '#/components/messages/Cancelled' }
                  Updated: { ${'$'}ref: '#/components/messages/Updated' }
            operations:
              feed:
                action: receive
                channel: { ${'$'}ref: '#/channels/inbound' }
              emit:
                action: send
                channel: { ${'$'}ref: '#/channels/events' }
            components:
              messages:
                Req: { payload: { type: object } }
                Created: { payload: { type: object, properties: { orderId: { type: string } } } }
                Cancelled: { payload: { type: object, properties: { reason: { type: string } } } }
                Updated: { payload: { type: object, properties: { v: { type: integer } } } }
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val subOut = built.operations["emit"]!!.single()
        assertEquals(AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT, subOut.kind)
        // Primary + 2 alternates = 3 declared variants.
        val variants = listOf(subOut.messageId) + subOut.additionalReplyMessageIds
        assertEquals(setOf("Created", "Cancelled", "Updated"), variants.toSet())
    }

    @Test
    fun sendOnlySchemaProducesNoPublishOnlyOutput() {
        // SEND-only schemas (no RECEIVE op = SUT consumes nothing) are still
        // parseable; the sampler refuses to start a search (no triggerable
        // operations) but the builder shouldn't blow up.
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              events:
                address: events
                messages:
                  M: { ${'$'}ref: '#/components/messages/M' }
            operations:
              emit:
                action: send
                channel: { ${'$'}ref: '#/channels/events' }
            components:
              messages:
                M: { payload: { type: object } }
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val subOut = built.operations["emit"]!!.single()
        assertEquals(AsyncAPIAction.Kind.SUBSCRIBE_OUTPUT, subOut.kind)
    }
}
