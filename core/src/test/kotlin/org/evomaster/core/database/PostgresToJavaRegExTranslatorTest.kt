package org.evomaster.core.database

import org.junit.Assert
import org.junit.jupiter.api.Test

class PostgresToJavaRegExTranslatorTest {

    @Test
    fun testPostgresToJavaRegex() {
        val outcome = PostgresToJavaRegExTranslator().translatePostgresSimilarTo("hello world")
        Assert.assertEquals("hello world", outcome)
    }

    @Test
    fun testWildcard1() {
        val outcome = PostgresToJavaRegExTranslator().translatePostgresSimilarTo("%hello%")
        Assert.assertEquals(".*hello.*", outcome)
    }

    @Test
    fun testWildcard2() {
        val outcome = PostgresToJavaRegExTranslator().translatePostgresSimilarTo("_hello_")
        Assert.assertEquals(".hello.", outcome)
    }

    @Test
    fun testEscape1() {
        val outcome = PostgresToJavaRegExTranslator().translatePostgresSimilarTo("\\%hello\\%")
        Assert.assertEquals("%hello%", outcome)
    }

    @Test
    fun testEscape2() {
        val outcome = PostgresToJavaRegExTranslator().translatePostgresSimilarTo("\\_hello\\_")
        Assert.assertEquals("_hello_", outcome)
    }

    @Test
    fun testEscape3() {
        val outcome = PostgresToJavaRegExTranslator().translatePostgresSimilarTo("\\\\")
        Assert.assertEquals("\\", outcome)
    }
}