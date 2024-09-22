package org.evomaster.core.output

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestWriterUtilsTest{


    @Test
    fun testSafeName(){

        //no change
        assertEquals("foo", TestWriterUtils.safeVariableName("foo"))
        assertEquals("foo42", TestWriterUtils.safeVariableName("foo42"))
        assertEquals("foo_bar", TestWriterUtils.safeVariableName("foo_bar"))
        assertEquals("__foo", TestWriterUtils.safeVariableName("__foo"))
        assertEquals("A", TestWriterUtils.safeVariableName("A"))

        //changed
        assertEquals("_x_", TestWriterUtils.safeVariableName(" x "))
        assertEquals("_42", TestWriterUtils.safeVariableName("42"))
        assertEquals("foo_bar", TestWriterUtils.safeVariableName("foo-bar"))
        assertEquals("_foo_", TestWriterUtils.safeVariableName("{foo}"))
        assertEquals("_", TestWriterUtils.safeVariableName("%"))

    }

    @Test
    fun testRemoveStartingAndEndingQuotesInJsonBody() {
        // no change
        assertEquals(listOf("Hello World"), TestWriterUtils.removeStartingAndEndingQuotesInJsonBody(listOf("Hello World")))
        assertEquals(listOf("Hello","World"), TestWriterUtils.removeStartingAndEndingQuotesInJsonBody(listOf("Hello","World")))
        assertEquals(listOf<String>(), TestWriterUtils.removeStartingAndEndingQuotesInJsonBody(listOf()))

        // changed
        assertEquals(listOf("\"Hello World\""), TestWriterUtils.removeStartingAndEndingQuotesInJsonBody(listOf("\" \\\"Hello World\\\" \"")))
        assertEquals(listOf("\"Hello New World\""), TestWriterUtils.removeStartingAndEndingQuotesInJsonBody(listOf("\" \\\"Hello New World\\\" \"")))
        assertEquals(listOf("\"Hello", "New", "World\""), TestWriterUtils.removeStartingAndEndingQuotesInJsonBody(listOf("\" \\\"Hello" ,"New", "World\\\" \"")))
    }



}
