package org.evomaster.core.parser

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class RegexUtilsTest {

    @Test
    fun testIgnoreCaseRegex() {

        assertEquals("(a|A)(b|B)(c|C)\\Q123\\E(d|D)(e|E)(f|F)", RegexUtils.ignoreCaseRegex("aBc123DEf"))
    }

    @Test
    fun testIgnoreCaseControlChars() {

        assertEquals("(a|A)\\Q[](){}\\\"^\$.\\E(b|B)", RegexUtils.ignoreCaseRegex("a[](){}\\\"^\$.b"))
    }
}