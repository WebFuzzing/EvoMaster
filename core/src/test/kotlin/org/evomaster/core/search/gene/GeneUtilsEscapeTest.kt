package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.formatter.OutputFormatter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

internal class GeneUtilsEscapeTest {

    @Test
    fun testEscapes(){
        val testJson = "{\n" +
                "\"name\" : \"contains\\\"now\"\n" +
                "\"values\" : \"various\u001dother\u0016unicode\"\n" +
                "\"kotlinTest\" : \"some\$kotlin\"\n" +
            "}"

        val formatKotlin = OutputFormat.KOTLIN_JUNIT_5
        val formatJava5 = OutputFormat.JAVA_JUNIT_5
        println("TestJson Raw: $testJson")
        println("TestJson Kotlin: ${GeneUtils.applyEscapes(testJson, "json", formatKotlin)}")
        println("TestJson Java: ${GeneUtils.applyEscapes(testJson, "json", formatJava5)}")

        assertTrue(GeneUtils.applyEscapes(testJson, "json", formatKotlin).contains("\\"))
        assertTrue(GeneUtils.applyEscapes(testJson, "json", formatJava5).contains("\\"))

        assertTrue(GeneUtils.applyEscapes(testJson, "json", formatKotlin).contains("\""))
        assertTrue(GeneUtils.applyEscapes(testJson, "json", formatKotlin).contains("\$"))

        assertFalse(GeneUtils.applyEscapes(testJson, "json", formatKotlin).contains("\\u"))

        //escaped JSON does not trigger formatter problems?
        //OutputFormatter.JSON_FORMATTER.getFormatted(GeneUtils.applyEscapes(testJson, "json", formatKotlin))
        //OutputFormatter.JSON_FORMATTER.getFormatted(GeneUtils.applyEscapes(testJson, "json", formatJava5))
    }
}