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

    @Test
    fun testConvertToAsciiPlainAsciiUnchanged() {
        assertEquals("hello_world", StringUtils.convertToAscii("hello_world"))
        assertEquals("FooBar123", StringUtils.convertToAscii("FooBar123"))
    }

    @Test
    fun testConvertToAsciiNorwegianDanish() {
        // Ø/ø and Æ/æ do not decompose under NFD — handled by explicit map
        assertEquals("O", StringUtils.convertToAscii("Ø"))
        assertEquals("o", StringUtils.convertToAscii("ø"))
        assertEquals("AE", StringUtils.convertToAscii("Æ"))
        assertEquals("ae", StringUtils.convertToAscii("æ"))
        // Å/å decomposes under NFD
        assertEquals("A", StringUtils.convertToAscii("Å"))
        assertEquals("a", StringUtils.convertToAscii("å"))
    }

    @Test
    fun testConvertToAsciiSwedishGerman() {
        // These all decompose under NFD (base letter + combining diacritic)
        assertEquals("A", StringUtils.convertToAscii("Ä"))
        assertEquals("a", StringUtils.convertToAscii("ä"))
        assertEquals("O", StringUtils.convertToAscii("Ö"))
        assertEquals("o", StringUtils.convertToAscii("ö"))
        assertEquals("U", StringUtils.convertToAscii("Ü"))
        assertEquals("u", StringUtils.convertToAscii("ü"))
        // ß does not decompose under NFD — handled by explicit map
        assertEquals("ss", StringUtils.convertToAscii("ß"))
    }

    @Test
    fun testConvertToAsciiIcelandic() {
        assertEquals("D",  StringUtils.convertToAscii("Ð"))
        assertEquals("d",  StringUtils.convertToAscii("ð"))
        assertEquals("TH", StringUtils.convertToAscii("Þ"))
        assertEquals("th", StringUtils.convertToAscii("þ"))
    }

    @Test
    fun testConvertToAsciiPolishFrench() {
        assertEquals("L",  StringUtils.convertToAscii("Ł"))
        assertEquals("l",  StringUtils.convertToAscii("ł"))
        assertEquals("OE", StringUtils.convertToAscii("Œ"))
        assertEquals("oe", StringUtils.convertToAscii("œ"))
    }

    @Test
    fun testConvertToAsciiOtherAccented() {
        // Common accented characters that decompose under NFD
        assertEquals("e", StringUtils.convertToAscii("é"))
        assertEquals("e", StringUtils.convertToAscii("è"))
        assertEquals("n", StringUtils.convertToAscii("ñ"))
        assertEquals("c", StringUtils.convertToAscii("ç"))
    }

    @Test
    fun testConvertToAsciiMixedString() {
        assertEquals("StromsAElv", StringUtils.convertToAscii("StrømsÆlv"))
        assertEquals("Malostranke_namesti", StringUtils.convertToAscii("Malostranké_náměstí"))
    }
}