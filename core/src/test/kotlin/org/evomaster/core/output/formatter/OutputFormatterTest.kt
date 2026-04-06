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

        val string = """{"id":"9d8UV_=e1T0eWTlc", "value":"93${'$'}v98g"}"""
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes3(){

        val string = """
            {"id":"19r\"l_", "value":""}
        """
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes4(){
        val testGene = StringGene("QuoteGene", "Test For the quotes${'"'}escape")

        OutputFormatter.JSON_FORMATTER.getFormatted(testGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = OutputFormat.KOTLIN_JUNIT_5))
    }

    @Test
    fun testEscapes6(){

        val string = """
            {"id":"19r\\l_"}
        """
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes7(){

        val string = """
           {"id":"Ot${'$'}Ag", "value":"Q"}
        """
        OutputFormatter.JSON_FORMATTER.getFormatted(string)
    }

    @Test
    fun testEscapes8(){
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
              "id" : 4821943963580588583,
              "name" : "n4QtYI",
              "rdId" : 937,
              "value" : 859
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

    @Test
    fun testInvalidJsonWithAsciiControlCharacter() {
        val json = " {\"s0\":\"This is a long string\", \"s1\":\"This isa long string\", \"s2\":\"_EM_3149_XYZ_\"} "
        val isValid = OutputFormatter.JSON_FORMATTER.isValid(json)
        assertFalse(isValid)
    }

    @Test
    fun testValidJsonWithEscapedAsciiControlCharacter() {
        val json = " {\"s0\":\"This is a long string\", \"s1\":\"This is\\u001Fa long string\", \"s2\":\"_EM_3149_XYZ_\"} "
        val isValid = OutputFormatter.JSON_FORMATTER.isValid(json)
        assertTrue(isValid)
    }

    @Test
    fun testXml(){
        assertTrue(OutputFormatter.getFormatters()?.size == 2)
        val body = """
        <root>
            <authorId>VZyJz8z_Eu2</authorId>
            <creationTime>1921-3-13T10:18:56.000Z</creationTime>
            <newsId>L</newsId>
        </root>
    """.trimIndent()

        // should throw no exception
        OutputFormatter.XML_FORMATTER.getFormatted(body)
    }

    @Test
    fun testXmlMismatched(){
        assertTrue(OutputFormatter.getFormatters()?.size == 2)
        val body = """
        <root>
            <authorId>VZyJz8z_Eu2</authorId>
            <creationTime>1921-3-13T10:18:56.000Z</creationTime>
            <newsId>L</newsId>
    """.trimIndent()

        assertThrows<Exception> {
            OutputFormatter.XML_FORMATTER.getFormatted(body)
        }
    }

    @Test
    fun testValidXml() {
        val xml = "<root><name>Hello World</name></root>"
        val isValid = OutputFormatter.XML_FORMATTER.isValid(xml)
        assertTrue(isValid)
    }

    @Test
    fun testInvalidXml() {
        val xml = "<root><name>Hello World</root>"
        val isValid = OutputFormatter.XML_FORMATTER.isValid(xml)
        assertFalse(isValid)
    }

    @Test
    fun testXmlScientificNotationLikeValues() {
        val body = """
        <root>
            <value>1e10</value>
            <small>2.5e-3</small>
        </root>
    """.trimIndent()

        val formatted = OutputFormatter.XML_FORMATTER.getFormatted(body)
        assertNotNull(formatted)
    }

    @Test
    fun testXmlWithAttributes() {
        val body = """
        <root>
            <item id="123" type="example">Content</item>
        </root>
    """.trimIndent()

        val formatted = OutputFormatter.XML_FORMATTER.getFormatted(body)
        assertNotNull(formatted)
    }

    @Test
    fun testXmlWithSpecialCharacters() {
        val body = """
        <root>
            <text>&lt;test&gt; &amp; &quot;quote&quot;</text>
        </root>
    """.trimIndent()

        val formatted = OutputFormatter.XML_FORMATTER.getFormatted(body)
        assertNotNull(formatted)
    }

    @Test
    fun testXmlNestedElements() {
        val body = """
        <root>
            <parent>
                <child>
                    <subchild>value</subchild>
                </child>
            </parent>
        </root>
    """.trimIndent()

        val formatted = OutputFormatter.XML_FORMATTER.getFormatted(body)
        assertNotNull(formatted)
    }

    @Test
    fun testXmlSelfClosingTag() {
        val body = """
        <root>
            <empty />
        </root>
    """.trimIndent()

        val formatted = OutputFormatter.XML_FORMATTER.getFormatted(body)
        assertNotNull(formatted)
    }

    @Test
    fun testXmlInvalidEscape() {
        val body = """
        <root>
            <text>&invalid;</text>
        </root>
    """.trimIndent()

        assertThrows<Exception> {
            OutputFormatter.XML_FORMATTER.getFormatted(body)
        }
    }

    @Test
    fun testXmlUnclosedTag() {
        val body = """
        <root>
            <child>value
        </root>
    """.trimIndent()

        assertThrows<Exception> {
            OutputFormatter.XML_FORMATTER.getFormatted(body)
        }
    }

    @Test
    fun testJsonReadFields() {
        val body = """
        {
          "authorId": "VZyJz8z_Eu2",
          "creationTime": "1921-3-13T10:18:56.000Z",
          "newsId": "L",
          "title": "Hello"
        }
    """.trimIndent()

        val result = OutputFormatter.JSON_FORMATTER.readFields(
            body,
            setOf("authorId", "newsId")
        )

        assertNotNull(result)
        assertEquals("VZyJz8z_Eu2", result?.get("authorId"))
        assertEquals("L", result?.get("newsId"))
        assertEquals(2, result?.size)
    }

    @Test
    fun testJsonReadFieldsMissingAndInvalid() {
        val body = """
        {
          "authorId": "VZyJz8z_Eu2",
          "title": "Hello"
        }
    """.trimIndent()

        val result = OutputFormatter.JSON_FORMATTER.readFields(
            body,
            setOf("authorId", "newsId")
        )

        assertNotNull(result)
        assertEquals("VZyJz8z_Eu2", result?.get("authorId"))
        assertFalse(result?.containsKey("newsId") ?: true)

        val invalidBody = """
        {
          "authorId": "VZyJz8z_Eu2",
          "title": "Hello"
    """.trimIndent()

        val invalidResult = OutputFormatter.JSON_FORMATTER.readFields(
            invalidBody,
            setOf("authorId", "title")
        )

        assertNull(invalidResult)
    }

    @Test
    fun testXmlReadFields() {
        val body = """
        <root>
            <authorId>VZyJz8z_Eu2</authorId>
            <creationTime>1921-3-13T10:18:56.000Z</creationTime>
            <newsId>L</newsId>
            <title>Hello</title>
        </root>
    """.trimIndent()

        val result = OutputFormatter.XML_FORMATTER.readFields(
            body,
            setOf("authorId", "newsId")
        )

        assertNotNull(result)
        assertEquals("VZyJz8z_Eu2", result?.get("authorId"))
        assertEquals("L", result?.get("newsId"))
        assertEquals(2, result?.size)
    }

    @Test
    fun testXmlReadFieldsMissingAndInvalid() {
        val body = """
        <root>
            <authorId>VZyJz8z_Eu2</authorId>
            <title>Hello</title>
        </root>
    """.trimIndent()

        val result = OutputFormatter.XML_FORMATTER.readFields(
            body,
            setOf("authorId", "newsId")
        )

        assertNotNull(result)
        assertEquals("VZyJz8z_Eu2", result?.get("authorId"))
        assertFalse(result?.containsKey("newsId") ?: true)

        val invalidBody = """
        <root>
            <authorId>VZyJz8z_Eu2</authorId>
            <title>Hello</title>
    """.trimIndent()

        val invalidResult = OutputFormatter.XML_FORMATTER.readFields(
            invalidBody,
            setOf("authorId", "title")
        )

        assertNull(invalidResult)
    }
}
