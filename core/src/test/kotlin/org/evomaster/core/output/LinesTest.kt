package org.evomaster.core.output

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class LinesTest {

    @Test
    fun isCurrentACommentLine() {

        val lines = Lines()
        lines.add("foo")
        assertFalse(lines.isCurrentACommentLine())

        lines.add("bar // bar")
        assertFalse(lines.isCurrentACommentLine())

        lines.add("   // Hello There!!! ...  ")
        assertTrue(lines.isCurrentACommentLine())
    }
}