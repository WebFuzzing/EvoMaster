package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.formatter.OutputFormatter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

internal class GeneUtilsEscapeTest {

    @Test
    fun testEscapes(){
        /*val testJson = "{\n" +
                "\"name\" : \"contains\\\"now\"\n" +
                "\"values\" : \"various\u001dother\u0016unicode\"\n" +
                "\"kotlinTest\" : \"some\$kotlin\"\n" +
            "}"

         */
        val testJson2 = """{"name":"kotlin"}"""

        val testJson = StringGene("Quote\\Gene", "Test For the quotes${'$'}escape")

        val formatKotlin = OutputFormat.KOTLIN_JUNIT_5
        val formatJava5 = OutputFormat.JAVA_JUNIT_5
        println("TestJson Raw: $testJson")
        println("TestJson Kotlin: ${testJson.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = formatKotlin)}")
        println("TestJson Java: ${testJson.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = formatKotlin)}")

        //assertTrue(testJson.getValueAsPrintableString(mode = "json", targetFormat = formatKotlin).contains("\\"))
        //assertTrue(testJson.getValueAsPrintableString(mode = "json", targetFormat = formatKotlin).contains("\\"))

        assertTrue(testJson.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = formatKotlin).contains("\""))
        assertTrue(testJson.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = formatKotlin).contains("\$"))

        assertFalse(testJson.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = formatKotlin).contains("\\u"))

        //escaped JSON does not trigger formatter problems?


        OutputFormatter.JSON_FORMATTER.getFormatted(testJson.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = formatKotlin))
        OutputFormatter.JSON_FORMATTER.getFormatted(testJson.getValueAsPrintableString(mode = GeneUtils.EscapeMode.JSON, targetFormat = formatJava5))

    }
}