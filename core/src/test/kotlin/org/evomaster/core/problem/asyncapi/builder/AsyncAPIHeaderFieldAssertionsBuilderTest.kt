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
 * M11-PR6: when a message declares a `headers:` schema the builder also
 * extracts per-header facets (required / enum / pattern / format / length)
 * into [AsyncAPIAction.headerFieldAssertions]. The writer consumes these
 * to emit `replyHeader*` assertions against the captured ReplyEnvelope's
 * headers map.
 */
class AsyncAPIHeaderFieldAssertionsBuilderTest {

    @Test
    fun builderExtractsRequiredEnumAndFormatAssertionsFromHeadersSchema() {
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
                action: receive
                channel: { ${'$'}ref: '#/channels/req' }
                reply:
                  channel: { ${'$'}ref: '#/channels/rep' }
            components:
              messages:
                ReqMsg:
                  payload: { type: object }
                RepMsg:
                  headers:
                    type: object
                    required: [x-tenant-id, x-trace-id]
                    properties:
                      x-tenant-id:    { type: string }
                      x-trace-id:     { type: string, format: uuid }
                      x-tenant-plan:  { type: string, enum: [free, pro, enterprise] }
                  payload:
                    type: object
                    properties:
                      orderId: { type: string }
        """.trimIndent()

        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)

        val subscribe = built.operations["callOrder"]!!
            .first { it.kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY }
        val publish = built.operations["callOrder"]!!
            .first { it.kind == AsyncAPIAction.Kind.PUBLISH }

        val specs = subscribe.headerFieldAssertions
        assertTrue(
            specs.any { it.path == "x-tenant-id" && it.kind == ReplyFieldAssertion.Kind.REQUIRED },
            "expected REQUIRED on x-tenant-id; got $specs"
        )
        assertTrue(
            specs.any { it.path == "x-trace-id" && it.kind == ReplyFieldAssertion.Kind.REQUIRED },
            "expected REQUIRED on x-trace-id; got $specs"
        )
        val traceFormat = specs.firstOrNull {
            it.path == "x-trace-id" && it.kind == ReplyFieldAssertion.Kind.FORMAT
        }
        assertTrue(traceFormat != null, "expected FORMAT spec for x-trace-id; got $specs")
        assertEquals("uuid", traceFormat!!.format)
        val planEnum = specs.firstOrNull {
            it.path == "x-tenant-plan" && it.kind == ReplyFieldAssertion.Kind.ENUM
        }
        assertTrue(planEnum != null, "expected ENUM spec for x-tenant-plan; got $specs")
        assertEquals(listOf("free", "pro", "enterprise"), planEnum!!.expectedValues)

        // PUBLISH actions never carry header-field assertions — they emit,
        // not observe.
        assertTrue(
            publish.headerFieldAssertions.isEmpty(),
            "PUBLISH should have no header assertions"
        )
    }

    @Test
    fun messagesWithoutHeadersSchemaYieldEmptyHeaderAssertions() {
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
                action: receive
                channel: { ${'$'}ref: '#/channels/req' }
                reply:
                  channel: { ${'$'}ref: '#/channels/rep' }
            components:
              messages:
                ReqMsg: { payload: { type: object } }
                RepMsg:
                  payload:
                    type: object
                    properties:
                      orderId: { type: string }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            asyncapi,
            SchemaLocation("inline", SchemaLocationType.RESOURCE)
        )
        val built = AsyncAPIActionBuilder(EMConfig()).build(schema)
        val subscribe = built.operations["call"]!!
            .first { it.kind == AsyncAPIAction.Kind.SUBSCRIBE_REPLY }
        assertTrue(
            subscribe.headerFieldAssertions.isEmpty(),
            "no headers schema → no header specs"
        )
    }
}
