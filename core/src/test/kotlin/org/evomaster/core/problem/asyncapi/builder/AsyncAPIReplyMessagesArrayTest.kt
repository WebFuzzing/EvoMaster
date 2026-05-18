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
 * AsyncAPI 3.0 lets a `reply.messages: [...]` enumerate several variants
 * the SUT can answer with.  Confirm the SUBSCRIBE_REPLY action carries
 * every declared reply id so the fitness layer can match an incoming
 * payload against any of them.
 */
class AsyncAPIReplyMessagesArrayTest {

    private val asyncapiWithMultiReply = """
        asyncapi: 3.0.0
        info: { title: t, version: 1 }
        channels:
          inbound:
            address: requests
            messages:
              Request: { ${'$'}ref: '#/components/messages/Request' }
          outbound:
            address: responses
            messages:
              Accepted:  { ${'$'}ref: '#/components/messages/Accepted' }
              Rejected:  { ${'$'}ref: '#/components/messages/Rejected' }
              Queued:    { ${'$'}ref: '#/components/messages/Queued' }
        operations:
          submit:
            action: receive
            channel: { ${'$'}ref: '#/channels/inbound' }
            messages:
              - ${'$'}ref: '#/channels/inbound/messages/Request'
            reply:
              channel: { ${'$'}ref: '#/channels/outbound' }
              messages:
                - ${'$'}ref: '#/channels/outbound/messages/Accepted'
                - ${'$'}ref: '#/channels/outbound/messages/Rejected'
                - ${'$'}ref: '#/channels/outbound/messages/Queued'
        components:
          messages:
            Request:  { payload: { type: object } }
            Accepted: { payload: { type: object, required: [orderId], properties: { orderId: { type: string } } } }
            Rejected: { payload: { type: object, required: [reason],  properties: { reason:  { type: string } } } }
            Queued:   { payload: { type: object, required: [eta],     properties: { eta:     { type: integer } } } }
    """.trimIndent()

    @Test
    fun subscribeReplyCarriesEveryDeclaredReplyMessageId() {
        val schema = AsyncAPIAccess.parse(asyncapiWithMultiReply,
            SchemaLocation("inline", SchemaLocationType.RESOURCE))
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val actions = built.operations["submit"]!!
        assertEquals(2, actions.size)
        val subscribe = actions[1]
        assertEquals(AsyncAPIAction.Kind.SUBSCRIBE_REPLY, subscribe.kind)
        // Primary id is whichever the channel listed first; the other two
        // come along as additionalReplyMessageIds.
        assertEquals("Accepted", subscribe.messageId)
        assertEquals(listOf("Rejected", "Queued"), subscribe.additionalReplyMessageIds)
    }

    @Test
    fun singleReplyKeepsAdditionalsEmpty() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val subscribe = built.operations["sendNcsRequest"]!![1]
        assertEquals(AsyncAPIAction.Kind.SUBSCRIBE_REPLY, subscribe.kind)
        assertTrue(subscribe.additionalReplyMessageIds.isEmpty(),
            "single-reply schemas should keep additionals empty")
    }
}
