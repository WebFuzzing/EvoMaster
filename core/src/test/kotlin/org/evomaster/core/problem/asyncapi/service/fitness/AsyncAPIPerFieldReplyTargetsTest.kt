package org.evomaster.core.problem.asyncapi.service.fitness

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-function coverage of [AbstractAsyncAPIFitness.replyFieldFacetTargets]
 * — the per-property facet ladder applied to a reply payload (M9-PR5).
 *
 * Each test drives a hand-rolled (variant schema, observed payload) pair
 * directly so the produced target set can be asserted without running a full
 * black-box evaluation.
 */
class AsyncAPIPerFieldReplyTargetsTest {

    private val mapper = ObjectMapper()

    @Test
    fun emitsRequiredPresentForFieldsThatAppear() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "required": ["orderId", "status"],
              "properties": {
                "orderId": { "type": "string" },
                "status":  { "type": "string" }
              }
            }
        """.trimIndent())
        val reply = mapper.readTree("""{ "orderId": "o-1", "status": "OK" }""")

        val targets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", reply, variantSchema
        )
        assertTrue(targets.contains("REPLY_FIELD:REQUIRED_PRESENT:Success:responses.ncs:sendNcsRequest:orderId"))
        assertTrue(targets.contains("REPLY_FIELD:REQUIRED_PRESENT:Success:responses.ncs:sendNcsRequest:status"))
        assertTrue(targets.none { it.contains("REQUIRED_ABSENT") }, "all required fields are present; no ABSENT targets")
    }

    @Test
    fun emitsRequiredAbsentWhenFieldMissing() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "required": ["orderId", "status"],
              "properties": {
                "orderId": { "type": "string" },
                "status":  { "type": "string" }
              }
            }
        """.trimIndent())
        val reply = mapper.readTree("""{ "orderId": "o-1" }""")

        val targets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", reply, variantSchema
        )
        assertTrue(targets.contains("REPLY_FIELD:REQUIRED_PRESENT:Success:responses.ncs:sendNcsRequest:orderId"))
        assertTrue(targets.contains("REPLY_FIELD:REQUIRED_ABSENT:Success:responses.ncs:sendNcsRequest:status"))
    }

    @Test
    fun emitsEnumInRangeAndOutOfRange() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "properties": {
                "status": { "type": "string", "enum": ["ACCEPTED", "REJECTED"] }
              }
            }
        """.trimIndent())

        val okReply = mapper.readTree("""{ "status": "ACCEPTED" }""")
        val okTargets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", okReply, variantSchema
        )
        assertTrue(okTargets.contains("REPLY_FIELD:ENUM_IN_RANGE:Success:responses.ncs:sendNcsRequest:status"))

        val badReply = mapper.readTree("""{ "status": "PENDING" }""")
        val badTargets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", badReply, variantSchema
        )
        assertTrue(badTargets.contains("REPLY_FIELD:ENUM_OUT_OF_RANGE:Success:responses.ncs:sendNcsRequest:status"))
    }

    @Test
    fun emitsBoundaryOkAndViolated() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "properties": {
                "amountCents": { "type": "integer", "minimum": 0, "maximum": 1000 }
              }
            }
        """.trimIndent())

        val okReply = mapper.readTree("""{ "amountCents": 250 }""")
        val okTargets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", okReply, variantSchema
        )
        assertTrue(okTargets.contains("REPLY_FIELD:BOUNDARY_OK:Success:responses.ncs:sendNcsRequest:amountCents"))

        val tooLow = mapper.readTree("""{ "amountCents": -5 }""")
        val tooLowTargets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", tooLow, variantSchema
        )
        assertTrue(tooLowTargets.contains("REPLY_FIELD:BOUNDARY_VIOLATED:Success:responses.ncs:sendNcsRequest:amountCents"))

        val tooHigh = mapper.readTree("""{ "amountCents": 9001 }""")
        val tooHighTargets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", tooHigh, variantSchema
        )
        assertTrue(tooHighTargets.contains("REPLY_FIELD:BOUNDARY_VIOLATED:Success:responses.ncs:sendNcsRequest:amountCents"))
    }

    @Test
    fun emitsLengthOkAndViolated() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "properties": {
                "orderId": { "type": "string", "minLength": 3, "maxLength": 8 }
              }
            }
        """.trimIndent())

        val okTargets = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", mapper.readTree("""{ "orderId": "o-12345" }"""), variantSchema
        )
        assertTrue(okTargets.contains("REPLY_FIELD:LENGTH_OK:Success:responses.ncs:sendNcsRequest:orderId"))

        val violation = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), "Success", mapper.readTree("""{ "orderId": "o" }"""), variantSchema
        )
        assertTrue(violation.contains("REPLY_FIELD:LENGTH_VIOLATED:Success:responses.ncs:sendNcsRequest:orderId"))
    }

    @Test
    fun emitsFormatOkAndViolatedForEmailUuidAndDateTime() {
        val variantSchema = mapper.readTree("""
            {
              "type": "object",
              "properties": {
                "email":      { "type": "string", "format": "email" },
                "id":         { "type": "string", "format": "uuid" },
                "occurredAt": { "type": "string", "format": "date-time" }
              }
            }
        """.trimIndent())

        val ok = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), null,
            mapper.readTree("""
                {
                  "email": "alice@example.com",
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "occurredAt": "2026-05-14T20:00:00Z"
                }
            """.trimIndent()),
            variantSchema
        )
        assertTrue(ok.any { it.contains("FORMAT_OK") && it.contains("email") && it.contains("=email") })
        assertTrue(ok.any { it.contains("FORMAT_OK") && it.contains(":id") && it.contains("=uuid") })
        assertTrue(ok.any { it.contains("FORMAT_OK") && it.contains("occurredAt") && it.contains("=date-time") })

        val bad = AbstractAsyncAPIFitness.replyFieldFacetTargets(
            subscribeReplyAction(), null,
            mapper.readTree("""
                {
                  "email": "not-an-email",
                  "id": "not-a-uuid",
                  "occurredAt": "tomorrow morning"
                }
            """.trimIndent()),
            variantSchema
        )
        assertTrue(bad.any { it.contains("FORMAT_VIOLATED") && it.contains("email") && it.contains("=email") })
        assertTrue(bad.any { it.contains("FORMAT_VIOLATED") && it.contains(":id") && it.contains("=uuid") })
        assertTrue(bad.any { it.contains("FORMAT_VIOLATED") && it.contains("occurredAt") && it.contains("=date-time") })
    }

    @Test
    fun nonObjectPayloadEmitsNothing() {
        val variantSchema = mapper.readTree("""
            { "type": "object", "properties": { "x": { "type": "string" } } }
        """.trimIndent())
        val reply = mapper.readTree(""""bare-string"""")
        assertEquals(
            emptyList<String>(),
            AbstractAsyncAPIFitness.replyFieldFacetTargets(subscribeReplyAction(), null, reply, variantSchema)
        )
    }

    private fun subscribeReplyAction(): AsyncAPIAction = AsyncAPIAction(
        operationName = "sendNcsRequest",
        channelAddress = "responses.ncs",
        channelName = "responsesNcs",
        kind = AsyncAPIAction.Kind.SUBSCRIBE_REPLY,
        pairId = "pair-x",
        messageId = "ReplyMessage",
        additionalReplyMessageIds = emptyList(),
        parameters = mutableListOf(),
        correlationHeaderName = "evm-correlation-id"
    )
}
