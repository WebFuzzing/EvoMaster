package org.evomaster.core.database

import org.junit.Assert
import org.junit.jupiter.api.Test

class PostgresToJavaRegExTranslatorTest {

    @Test
    fun testPostgresToJavaRegex() {
        val outcome = PostgresToJavaRegExTranslator().translate("hello world")
        Assert.assertEquals("hello world", outcome)
    }

    @Test
    fun testWildcard1() {
        val outcome = PostgresToJavaRegExTranslator().translate("%hello%")
        Assert.assertEquals(".*hello.*", outcome)
    }

    @Test
    fun testWildcard2() {
        val outcome = PostgresToJavaRegExTranslator().translate("_hello_")
        Assert.assertEquals(".hello.", outcome)
    }

    @Test
    fun testEscape1() {
        val outcome = PostgresToJavaRegExTranslator().translate("\\%hello\\%")
        Assert.assertEquals("%hello%", outcome)
    }

    @Test
    fun testEscape2() {
        val outcome = PostgresToJavaRegExTranslator().translate("\\_hello\\_")
        Assert.assertEquals("_hello_", outcome)
    }

    @Test
    fun testEscape3() {
        val outcome = PostgresToJavaRegExTranslator().translate("\\\\")
        Assert.assertEquals("\\", outcome)
    }
}