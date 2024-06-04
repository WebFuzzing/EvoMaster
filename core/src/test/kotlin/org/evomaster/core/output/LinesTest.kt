package org.evomaster.core.output

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class LinesTest {

    @Test
    fun isCurrentACommentLine() {

        val lines = Lines(OutputFormat.JAVA_JUNIT_4)
        lines.add("foo")
        assertFalse(lines.isCurrentACommentLine())

        lines.add("bar // bar")
        assertFalse(lines.isCurrentACommentLine())

        lines.add("   // Hello There!!! ...  ")
        assertTrue(lines.isCurrentACommentLine())
    }

    @Test
    fun testPythonCommentLineUsesHashSymbol() {
        val lines = Lines(OutputFormat.PYTHON_UNITTEST)
        lines.addBlockCommentLine("this is a python comment")
        assertTrue(lines.toString().startsWith("# this"))
        assertTrue(lines.isCurrentACommentLine())
    }
}
