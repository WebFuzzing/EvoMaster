package org.evomaster.core.parser

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class RegexUtilsTest {

    @Test
    fun testIgnoreCaseRegex() {

        assertEquals("(a|A)(b|B)(c|C)123(d|D)(e|E)(f|F)", RegexUtils.ignoreCaseRegex("aBc123DEf"))
    }
}