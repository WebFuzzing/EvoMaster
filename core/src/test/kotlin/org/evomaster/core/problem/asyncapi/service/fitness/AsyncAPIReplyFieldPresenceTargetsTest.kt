package org.evomaster.core.problem.asyncapi.service.fitness

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-function tests for [AbstractAsyncAPIFitness.replyFieldPresenceTargets].
 * Drives the helper directly off a hand-rolled (variant schema, parsed
 * reply) pair so we can assert exactly which targets fire for which
 * present/absent permutations — no broker, no full evaluation.
 */
class AsyncAPIReplyFieldPresenceTargetsTest {

    private val mapper = ObjectMapper()

    @Test
    fun emitsPresentForFieldsThatAppearAndAbsentForOnesThatDont() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "required": ["status"],
              "properties": {
                "status":   { "type": "string" },
                "progress": { "type": "number" },
                "details":  { "type": "object" }
              }
            }
        """.trimIndent())
        val reply = mapper.readTree("""{ "status": "ok", "progress": 50 }""")

        val targets = AbstractAsyncAPIFitness.replyFieldPresenceTargets(
            subscribeAction(), "Success", reply, variantSchema
        )

        assertTrue(targets.contains("REPLY_FIELD_PRESENCE:Success:responses.ncs:sendNcsRequest:progress=present"))
        assertTrue(targets.contains("REPLY_FIELD_PRESENCE:Success:responses.ncs:sendNcsRequest:details=absent"))
        // Required `status` does not get a presence target — its absence
        // would already trip REPLY_SCHEMA_INVALID, so the signal is
        // redundant.
        assertTrue(targets.none { it.contains(":status=") }, "required fields must not appear: $targets")
    }

    @Test
    fun nonObjectReplyEmitsNothing() {
        val variantSchema = mapper.readTree("""
            { "type": "object", "properties": { "x": { "type": "string" } } }
        """.trimIndent())
        val reply = mapper.readTree(""""just a string"""")

        assertEquals(emptyList<String>(),
            AbstractAsyncAPIFitness.replyFieldPresenceTargets(subscribeAction(), null, reply, variantSchema))
    }

    @Test
    fun schemaWithoutPropertiesEmitsNothing() {
        val variantSchema = mapper.readTree("""{ "type": "object" }""")
        val reply = mapper.readTree("""{ "anything": 1 }""")

        assertEquals(emptyList<String>(),
            AbstractAsyncAPIFitness.replyFieldPresenceTargets(subscribeAction(), null, reply, variantSchema))
    }

    private fun subscribeAction(): AsyncAPIAction = AsyncAPIAction(
        operationName = "sendNcsRequest",
        channelAddress = "responses.ncs",
        channelName = "responsesNcs",
        kind = AsyncAPIAction.Kind.SUBSCRIBE_REPLY,
        pairId = "test-pair",
        messageId = "ReplyMessage",
        parameters = mutableListOf(),
        correlationHeaderName = "evm-correlation-id"
    )
}
