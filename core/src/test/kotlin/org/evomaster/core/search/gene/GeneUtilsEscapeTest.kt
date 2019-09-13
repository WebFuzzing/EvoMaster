package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.formatter.OutputFormatter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

internal class GeneUtilsEscapeTest {

    @Test
    fun testEscapes(){
        val testJson = "{\n" +
            "\"name\" : \"contains\\\"now\"\n" +
            "}"

        val formatKotlin = OutputFormat.KOTLIN_JUNIT_5
        val formatJava5 = OutputFormat.JAVA_JUNIT_5
        println("TestJson: ${GeneUtils.applyEscapes(testJson, "json")}")

        assertTrue(GeneUtils.applyEscapes(testJson, "json", formatKotlin).contains("\\"))
        assertTrue(GeneUtils.applyEscapes(testJson, "json", formatJava5).contains("\\"))

        //escaped JSON does not trigger formatter problems?
        //OutputFormatter.JSON_FORMATTER.getFormatted(GeneUtils.applyEscapes(testJson, "json", formatKotlin))
        //OutputFormatter.JSON_FORMATTER.getFormatted(GeneUtils.applyEscapes(testJson, "json", formatJava5))
    }
}