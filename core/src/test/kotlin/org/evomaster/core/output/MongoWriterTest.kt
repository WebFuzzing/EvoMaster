package org.evomaster.core.output

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MongoWriterTest {

    @Test
    fun `should escape literal backslash u in EJSON without changing its runtime value`() {
        val ejson = """{"field":"El24e\\uJTQGh"}"""

        val escaped = MongoWriter.escapeEjsonForJavaLiteral(ejson)

        assertEquals("""{\"field\":\"El24e\134\134uJTQGh\"}""", escaped)
        assertFalse(Regex("""(\\\\)+u""").containsMatchIn(escaped))
    }
}
