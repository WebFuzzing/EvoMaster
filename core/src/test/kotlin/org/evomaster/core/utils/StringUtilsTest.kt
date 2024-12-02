package org.evomaster.core.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StringUtilsTest{


    @Test
    fun testLinesWithMaxLength(){

        val tokens = listOf("Foo","A","B","Hello","C","D")
        val separator = ", "

        val lines = StringUtils.linesWithMaxLength(tokens,separator, 6)
        assertEquals(4, lines.size)
        assertEquals("Foo, A",lines[0])
        assertEquals(", B",lines[1])
        assertEquals(", Hello",lines[2])
        assertEquals(", C, D",lines[3])
    }
}