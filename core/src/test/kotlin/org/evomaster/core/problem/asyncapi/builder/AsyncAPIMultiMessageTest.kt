package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AsyncAPI 3.0 lets one operation dispatch over several message types on
 * the same channel (`messages: [Created, Updated, Cancelled]`). The
 * builder must produce one runnable action variant per message id so the
 * sampler can pick any of them — picking only the first leaves the other
 * variants unreachable to the EA.
 */
class AsyncAPIMultiMessageTest {

    private val asyncapiWithMultipleMessages = """
        asyncapi: 3.0.0
        info: { title: t, version: 1 }
        channels:
          inbound:
            address: orders.inbound
            messages:
              OrderCreated:   { ${'$'}ref: '#/components/messages/OrderCreated' }
              OrderUpdated:   { ${'$'}ref: '#/components/messages/OrderUpdated' }
              OrderCancelled: { ${'$'}ref: '#/components/messages/OrderCancelled' }
        operations:
          publishOrderEvent:
            action: send
            channel: { ${'$'}ref: '#/channels/inbound' }
            messages:
              - ${'$'}ref: '#/channels/inbound/messages/OrderCreated'
              - ${'$'}ref: '#/channels/inbound/messages/OrderUpdated'
              - ${'$'}ref: '#/channels/inbound/messages/OrderCancelled'
        components:
          messages:
            OrderCreated:
              payload: { type: object, properties: { id: { type: string } } }
            OrderUpdated:
              payload: { type: object, properties: { id: { type: string }, version: { type: integer } } }
            OrderCancelled:
              payload: { type: object, properties: { id: { type: string }, reason: { type: string } } }
    """.trimIndent()

    @Test
    fun builderProducesOneActionVariantPerDeclaredMessage() {
        val schema = AsyncAPIAccess.parse(asyncapiWithMultipleMessages,
            SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        // With multiple messages, the operation key becomes opName#messageId
        // so each variant is independently samplable.
        val keys = built.operations.keys.filter { it.startsWith("publishOrderEvent") }
        assertEquals(setOf(
            "publishOrderEvent#OrderCreated",
            "publishOrderEvent#OrderUpdated",
            "publishOrderEvent#OrderCancelled"
        ), keys.toSet())

        keys.forEach { key ->
            val actions = built.operations[key]!!
            assertEquals(1, actions.size, "$key should be a single PUBLISH (no reply declared)")
            val publish = actions.single()
            assertEquals(AsyncAPIAction.Kind.PUBLISH, publish.kind)
            // The action carries the specific message id it was built for, so
            // the fitness can emit PUBLISH_MESSAGE_TYPE per variant.
            assertEquals(key.substringAfter("#"), publish.messageId)
        }
    }

    @Test
    fun singleMessageStaysKeyedByOperationName() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        // Backward-compat: rest-kafka-ncs has one message type per operation,
        // so the key stays as the bare operation name (no `#` suffix).
        assertTrue(built.operations.containsKey("sendNcsRequest"))
        assertTrue(built.operations.keys.none { it.contains("#") })
    }
}
