package org.evomaster.core.output.formatter

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class OutputFormatterTest {

    @Test
    fun test(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val body = """
                {
                   "authorId": "VZyJz8z_Eu2",
                   "creationTime": "1921-3-13T10:18:56.000Z",
                   "newsId": "L"
                }
                """
        //should throw no exception
        OutputFormatter.JSON_FORMATTER.getFormatted(body)
    }


    @Test
    fun testMismatched(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val body = """

                   "authorId": "VZyJz8z_Eu2",
                   "creationTime": "1921-3-13T10:18:56.000Z",
                   "newsId": "L"
                }
                """

        assertThrows<Exception>{
            OutputFormatter.JSON_FORMATTER.getFormatted(body)
        }
    }
    @Test
    fun testEscapes(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val body = """
            {
            "name":"T\""
            }
        """

        val stringGene = StringGene("name", body)
        OutputFormatter.JSON_FORMATTER.getFormatted(body)
    }

    @Test
    fun testEscapes2(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)

        val string = """{"id":"9d8UV_=e1T0eWTlc", "value":"93${'$'}v98g"}"""
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes3(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)

        val string = """
            {"id":"19r\"l_", "value":""}
        """
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes4(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val testGene = StringGene("QuoteGene", "Test For the quotes${'"'}escape")

        OutputFormatter.JSON_FORMATTER.getFormatted(testGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = OutputFormat.KOTLIN_JUNIT_5))
    }

    @Test
    fun testEscapes6(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)

        val string = """
            {"id":"19r\\l_"}
        """
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes7(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)

        val string = """
           {"id":"Ot${'$'}Ag", "value":"Q"}
        """
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes8(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val testGene = StringGene("DollarGene", "Test For the dollar${'$'}escape")

        OutputFormatter.JSON_FORMATTER.getFormatted(testGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = OutputFormat.KOTLIN_JUNIT_5))
        OutputFormatter.JSON_FORMATTER.getFormatted(testGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = OutputFormat.JAVA_JUNIT_5))

    }

    @Test
    fun testJsonScientificNotation(){
        val json = "{\"id\":4821943963580588583, \"name\":\"n4QtYI\", \"rdId\":937, \"value\":859}"
        val formatted = OutputFormatter.JSON_FORMATTER.getFormatted(json)
        val expected = """
            {
              "id": 4821943963580588583,
              "name": "n4QtYI",
              "rdId": 937,
              "value": 859
            }
        """.trimIndent()
        assertEquals(expected, formatted)
    }

    @Test
    fun testValidQuotedString() {
        val json = "\"Hello World\""
        val isValid = OutputFormatter.JSON_FORMATTER.isValid(json)
        assertTrue(isValid)
    }
    @Test
    fun testInvalidUnquotedString() {
        val json = "Hello World"
        val isValid = OutputFormatter.JSON_FORMATTER.isValid(json)
        assertFalse(isValid)
    }

    /*
     Unquoted strings are not valid JSON. But
     the GSON parser does not implement the standard.
     This should be eventually fixed.
     Fixed by switching to Jackson for validation
     */
    @Test
    fun testInvalidUnquotedStringWithoutBlanks() {
        val json = "HelloWorld"
        val isValid = OutputFormatter.JSON_FORMATTER.isValid(json)
        assertFalse(isValid)
    }


}
