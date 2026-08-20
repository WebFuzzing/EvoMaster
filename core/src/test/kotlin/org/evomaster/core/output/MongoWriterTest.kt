package org.evomaster.core.output

import org.apache.commons.lang3.StringEscapeUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MongoWriterTest {

    @Test
    fun `should escape literal backslash u without changing its runtime value`() {
        val cases = mapOf(
            """{"field":"El24e\uJTQGh"}""" to """{\"field\":\"El24e\134uJTQGh\"}""",
            """{"field":"El24e\\uJTQGh"}""" to """{\"field\":\"El24e\134\134uJTQGh\"}""",
            """{"field":"\u0041"}""" to """{\"field\":\"\134u0041\"}"""
        )

        cases.forEach { (ejson, expected) ->
            val escaped = MongoWriter.escapeEjsonForJavaLiteral(ejson)

            assertEquals(expected, escaped)
            assertEquals(ejson, StringEscapeUtils.unescapeJava(escaped))
        }
    }

    @Test
    fun `should not rewrite unicode escapes created by escapeJava`() {
        listOf("\\é", "\\😀", "é", "😀").forEach { ejson ->
            val escaped = MongoWriter.escapeEjsonForJavaLiteral(ejson)

            assertEquals(StringEscapeUtils.escapeJava(ejson), escaped)
            assertEquals(ejson, StringEscapeUtils.unescapeJava(escaped))
        }
    }
}
