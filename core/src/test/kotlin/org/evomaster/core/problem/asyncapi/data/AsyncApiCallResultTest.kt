package org.evomaster.core.problem.asyncapi.data

import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsyncApiCallResultTest {

    companion object {
        private const val ADDRESS = "shop.orders.confirmed"

        private const val SOURCE_ID = "action-0"

        private const val FILLER = 'a'

        /**
         * A character outside the Basic Multilingual Plane, written as two chars.
         */
        private const val SURROGATE_PAIR = "😀"

        private const val MAX = AsyncApiCallResult.EMITTED_PAYLOAD_MAX_LENGTH
    }

    private fun message(payload: String?, truncated: Boolean? = false) = EmittedMessageDto().apply {
        address = ADDRESS
        this.payload = payload
        payloadTruncated = truncated
        payloadType = String::class.java.name
    }

    private fun keep(message: EmittedMessageDto): EmittedMessageDto {
        val result = AsyncApiCallResult(SOURCE_ID)
        result.setEmittedMessages(listOf(message))
        return result.getEmittedMessages().single()
    }

    @Test
    fun testAShortPayloadIsKeptAsItIs() {

        val payload = FILLER.toString().repeat(MAX)

        val kept = keep(message(payload))

        assertEquals(ADDRESS, kept.address)
        assertEquals(payload, kept.payload)
        assertEquals(false, kept.payloadTruncated)
        assertEquals(String::class.java.name, kept.payloadType)
    }

    @Test
    fun testALongPayloadIsKeptCutAndFlagged() {

        val kept = keep(message(FILLER.toString().repeat(MAX + 1)))

        assertEquals(FILLER.toString().repeat(MAX), kept.payload)
        assertEquals(true, kept.payloadTruncated, "a cut payload must say so, or a test would assert on it")
    }

    @Test
    fun testACutDoesNotSplitASurrogatePair() {

        //the pair takes the last char that fits and the first that does not
        val payload = FILLER.toString().repeat(MAX - 1) + SURROGATE_PAIR

        val kept = keep(message(payload))

        assertEquals(FILLER.toString().repeat(MAX - 1), kept.payload)
        assertEquals(true, kept.payloadTruncated)
    }

    @Test
    fun testWhatTheInstrumentationCutStaysFlagged() {

        val kept = keep(message(FILLER.toString(), truncated = true))

        assertEquals(true, kept.payloadTruncated)
    }

    @Test
    fun testAMessageWithoutTextPayloadKeepsItsType() {

        val kept = keep(message(null, truncated = null))

        assertNull(kept.payload)
        assertNull(kept.payloadTruncated)
        assertEquals(String::class.java.name, kept.payloadType)
    }
}
