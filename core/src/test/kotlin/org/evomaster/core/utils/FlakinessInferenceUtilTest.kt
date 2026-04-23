package org.evomaster.core.utils

import org.evomaster.core.utils.FlakinessInferenceUtil.HASH_MARKER
import org.evomaster.core.utils.FlakinessInferenceUtil.OBJ_MARKER
import org.evomaster.core.utils.FlakinessInferenceUtil.RAND_MARKER
import org.evomaster.core.utils.FlakinessInferenceUtil.RUNMSG_MARKER
import org.evomaster.core.utils.FlakinessInferenceUtil.TIME_MARKER
import org.evomaster.core.utils.FlakinessInferenceUtil.UUID_MARKER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlakinessInferenceUtilTest {

    @Test
    fun testDeriveReplacesTimeTokens() {
        val input = """{"ts":"2023-08-09T10:11:12.123Z","ts2":"2023-08-09 10:11:12","date":"2023-08-09","epoch":1691575872123}"""
        val expected = """{"ts":"$TIME_MARKER","ts2":"$TIME_MARKER","date":"$TIME_MARKER","epoch":$TIME_MARKER}"""

        assertEquals(expected, FlakinessInferenceUtil.derive(input))
    }

    @Test
    fun testDeriveReplacesTimeTokensWithTSeparator() {
        val input = "\"2023-08-09T10:11:12\""
        val expected = "\"$TIME_MARKER\""

        assertEquals(expected, FlakinessInferenceUtil.derive(input))
    }

    @Test
    fun testDeriveReplacesRandomTokens() {
        val input = "uuid 123e4567-e89b-12d3-a456-426614174000 hex deadbeefdeadbeef base64 YWJjZGVmZ2hpamtsbW5vcA"
        val expected = "uuid $UUID_MARKER hex $RAND_MARKER base64 $RAND_MARKER"

        assertEquals(expected, FlakinessInferenceUtil.derive(input))
    }

    @Test
    fun testDeriveReplacesBase64WithPadding() {
        val input = "token YWJjZGVmZ2hpamtsbW5vcA=="
        val expected = "token $RAND_MARKER=="

        assertEquals(expected, FlakinessInferenceUtil.derive(input))
    }

    @Test
    fun testDeriveReplacesHashTokens() {
        val input = "hash d41d8cd98f00b204e9800998ecf8427e"
        val expected = "hash $HASH_MARKER"

        assertEquals(expected, FlakinessInferenceUtil.derive(input))
    }

    @Test
    fun testDeriveIgnoresNonMatchingTokens() {
        val input = "date 2023-08-09 epoch 169157587212 notEpoch 1691575872123x base64short YWJj"
        val expected = "date 2023-08-09 epoch 169157587212 notEpoch 1691575872123x base64short YWJj"

        assertEquals(expected, FlakinessInferenceUtil.derive(input))
    }

    @Test
    fun testDeriveReplacesRuntimeTokens() {
        val input = "obj com.foo.Bar@1a2b3c ptr 0x1a2b3c line Foo.java:123"
        val expected = "obj $OBJ_MARKER ptr $RUNMSG_MARKER line Foo$RUNMSG_MARKER"

        assertEquals(expected, FlakinessInferenceUtil.derive(input))
    }
}
