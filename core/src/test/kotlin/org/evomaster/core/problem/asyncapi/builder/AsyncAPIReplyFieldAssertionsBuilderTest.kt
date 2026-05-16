package org.evomaster.core.problem.asyncapi.builder

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.problem.asyncapi.data.ReplyFieldAssertion
import org.evomaster.core.problem.asyncapi.schema.AsyncAPIAccess
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Confirms M9-PR5 builder additions populate
 * [AsyncAPIAction.replyFieldAssertions] for SUBSCRIBE_REPLY actions and
 * leave them empty for other action kinds.
 */
class AsyncAPIReplyFieldAssertionsBuilderTest {

    @Test
    fun builderExtractsRequiredAndEnumAssertionsForSubscribeReply() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              req:
                address: orders.requests
                messages:
                  ReqMsg: { ${'$'}ref: '#/components/messages/ReqMsg' }
              rep:
                address: orders.responses
                messages:
                  RepMsg: { ${'$'}ref: '#/components/messages/RepMsg' }
            operations:
              callOrder:
                action: send
                channel: { ${'$'}ref: '#/channels/req' }
                reply:
                  channel: { ${'$'}ref: '#/channels/rep' }
            components:
              messages:
                ReqMsg:
                  payload: { type: object }
                RepMsg:
                  payload:
                    type: object
                    required: [orderId, status]
                    properties:
                      orderId: { type: string }
                      status:  { type: string, enum: [ACCEPTED, REJECTED] }
                      note:    { type: string }
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val pair = built.operations["callOrder"]!!
        assertEquals(2, pair.size)
        val subscribe = pair.first { it.kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY }
        val publish = pair.first { it.kind == AsyncAPIAction.Kind.PUBLISH }

        // SUBSCRIBE_REPLY carries the pre-computed assertion specs.
        val specs = subscribe.replyFieldAssertions
        assertTrue(specs.any { it.path == "orderId" && it.kind == ReplyFieldAssertion.Kind.REQUIRED })
        assertTrue(specs.any { it.path == "status" && it.kind == ReplyFieldAssertion.Kind.REQUIRED })
        val statusEnum = specs.firstOrNull { it.path == "status" && it.kind == ReplyFieldAssertion.Kind.ENUM }
        assertTrue(statusEnum != null, "expected ENUM spec for 'status'; got $specs")
        assertEquals(listOf("ACCEPTED", "REJECTED"), statusEnum!!.expectedValues)
        // `note` is optional, no enum → no assertion spec.
        assertTrue(specs.none { it.path == "note" }, "optional `note` should not yield a spec")

        // PUBLISH actions never carry reply-field assertions.
        assertTrue(publish.replyFieldAssertions.isEmpty(), "PUBLISH should have no reply assertions")
    }

    @Test
    fun replyWithoutPropertiesYieldsNoSpecs() {
        val asyncapi = """
            asyncapi: 3.0.0
            info: { title: t, version: '1' }
            channels:
              req:
                address: t
                messages:
                  ReqMsg: { ${'$'}ref: '#/components/messages/ReqMsg' }
              rep:
                address: t2
                messages:
                  RepMsg: { ${'$'}ref: '#/components/messages/RepMsg' }
            operations:
              call:
                action: send
                channel: { ${'$'}ref: '#/channels/req' }
                reply:
                  channel: { ${'$'}ref: '#/channels/rep' }
            components:
              messages:
                ReqMsg: { payload: { type: object } }
                RepMsg: { payload: { type: object } }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val subscribe = built.operations["call"]!!.first { it.kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY }
        assertTrue(subscribe.replyFieldAssertions.isEmpty(), "no properties → no specs")
    }
}
