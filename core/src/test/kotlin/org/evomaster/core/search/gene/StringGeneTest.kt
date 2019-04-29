package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class StringGeneTest{

    @Test
    fun slashTest(){
        val testChar = '\\'
        val testGene = StringGene("SlashGene", "Test For the Slash ${testChar}escape")

        //println("Printable Slash =>  ${testGene.getValueAsPrintableString()}")
        //println("Raw Slash =>  ${testGene.getValueAsRawString()}")

        assertTrue(testGene.getValueAsPrintableString(targetFormat = null).contains("\\\\"))
        assertTrue(!testGene.getValueAsPrintableString(targetFormat = null).contains(" \\e"))
        assertTrue(!testGene.getValueAsRawString().contains("\\\\"))
        assertTrue(testGene.getValueAsRawString().contains(" \\e"))
    }

    @Test
    fun dollarTest(){
        val dollarTest = '$'
        val testGene = StringGene("DollarGene", "Test for the Dollar ${dollarTest}escape")

        println("Dollar => ${testGene.getValueAsPrintableString()}")
        println("Dollar => ${testGene.getValueAsPrintableString(targetFormat = OutputFormat.KOTLIN_JUNIT_5)}")

        // Check that it contains the character
        assertTrue(testGene.getValueAsPrintableString(targetFormat = OutputFormat.KOTLIN_JUNIT_5).contains('$'))
        // Check that the character is escaped
        assertTrue(testGene.getValueAsPrintableString(targetFormat = OutputFormat.KOTLIN_JUNIT_5).contains("\\$"))
        //Check that it does not contain unescaped character (of the relevant type)
        assertTrue(!testGene.getValueAsPrintableString(targetFormat = OutputFormat.KOTLIN_JUNIT_5).contains(" \$"))
        // Check that the non-kotlin version does not contain the escaped character
        assertTrue(!testGene.getValueAsPrintableString().contains("\\$"))
    }
}