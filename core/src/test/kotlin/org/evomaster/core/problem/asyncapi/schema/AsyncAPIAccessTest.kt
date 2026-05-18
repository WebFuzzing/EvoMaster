package org.evomaster.core.problem.asyncapi.schema

import org.evomaster.core.remote.SutProblemException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AsyncAPIAccessTest {

    @Test
    fun parseRestKafkaNcsFixture() {
        val schema = AsyncAPIAccess.getAsyncAPIFromResource("/asyncapi/fixtures/rest-kafka-ncs.yaml")

        assertEquals("3.0.0", schema.version)
        assertEquals("application/json", schema.defaultContentType)

        // servers
        val kafkaServer = schema.servers["kafka"]
        assertNotNull(kafkaServer)
        assertEquals("kafka", kafkaServer!!.protocol)

        // channels
        val channels = schema.channels
        assertEquals(setOf("requestsNcs", "responsesNcs"), channels.keys)
        assertEquals("requests.ncs", channels["requestsNcs"]!!.address)
        assertEquals(listOf("RequestMessage"), channels["requestsNcs"]!!.messageIds)

        // operations: receive (SUT consumes from the request channel) + reply binding
        val send = schema.operations["sendNcsRequest"]
        assertNotNull(send)
        assertEquals(AsyncAPIOperation.Action.RECEIVE, send!!.action)
        assertEquals("requestsNcs", send.channelName)
        assertNotNull(send.reply)
        assertEquals(listOf("responsesNcs"), send.reply!!.channelNames)

        // component messages
        val request = schema.messages["RequestMessage"]
        assertNotNull(request)
        assertEquals("\$message.header#/evm-correlation-id", request!!.correlationLocation)
        assertEquals("RequestPayload", request.payloadSchemaRef)
        assertNull(request.payloadInline)

        val reply = schema.messages["ReplyMessage"]
        assertNotNull(reply)
        assertEquals("ReplyPayload", reply!!.payloadSchemaRef)

        // component schemas — the oneOf reply variants exist
        assertTrue(schema.componentSchemas.containsKey("ReplyPayload"))
        assertTrue(schema.componentSchemas.containsKey("SuccessReply"))
        assertTrue(schema.componentSchemas.containsKey("ErrorReply"))
    }

    @Test
    fun rejectAsyncApi2xWithClearMessage() {
        val ex = assertThrows(SutProblemException::class.java) {
            AsyncAPIAccess.parse(
                schemaText = """
                    asyncapi: 2.6.0
                    info:
                      title: legacy
                      version: 1.0.0
                    channels: {}
                """.trimIndent(),
                location = org.evomaster.core.problem.rest.schema.SchemaLocation(
                    "test://", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
                )
            )
        }
        assertTrue(ex.message!!.contains("not supported"), "message was: ${ex.message}")
    }

    @Test
    fun rejectMissingAsyncApiVersionField() {
        val ex = assertThrows(SutProblemException::class.java) {
            AsyncAPIAccess.parse(
                schemaText = """
                    info:
                      title: oops
                      version: 0.0.1
                """.trimIndent(),
                location = org.evomaster.core.problem.rest.schema.SchemaLocation(
                    "test://", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
                )
            )
        }
        assertTrue(ex.message!!.contains("'asyncapi' version"), "message was: ${ex.message}")
    }

    @Test
    fun acceptsJsonSchemaTextThroughSameEntryPoint() {
        val json = """
            {
              "asyncapi": "3.0.0",
              "info": {"title": "x", "version": "1.0"},
              "channels": {
                "c1": {"address": "topic.a", "messages": {"M": {"${'$'}ref": "#/components/messages/M"}}}
              },
              "operations": {
                "op1": {
                  "action": "send",
                  "channel": {"${'$'}ref": "#/channels/c1"}
                }
              },
              "components": {
                "messages": {"M": {"name": "M", "payload": {"type": "object"}}}
              }
            }
        """.trimIndent()
        val schema = AsyncAPIAccess.parse(
            json,
            org.evomaster.core.problem.rest.schema.SchemaLocation(
                "inline", org.evomaster.core.problem.rest.schema.SchemaLocationType.RESOURCE
            )
        )
        assertEquals("3.0.0", schema.version)
        assertEquals(AsyncAPIOperation.Action.SEND, schema.operations["op1"]!!.action)
        assertEquals(listOf("M"), schema.channels["c1"]!!.messageIds)
        assertNotNull(schema.messages["M"]!!.payloadInline)
        assertNull(schema.messages["M"]!!.payloadSchemaRef)
    }
}
