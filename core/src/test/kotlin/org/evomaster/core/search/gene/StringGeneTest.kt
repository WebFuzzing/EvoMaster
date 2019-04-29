package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class StringGeneTest{

    @Test
    fun slashTest(){
        val testChar = '\\'
        val testGene = StringGene("SlashGene", "Test For the Slash ${testChar}escape")

        //println("Printable Slash =>  ${testGene.getValueAsPrintableString()}")
        //println("Raw Slash =>  ${testGene.getValueAsRawString()}")

        assertTrue(testGene.getValueAsPrintableString().contains("\\\\"))
        assertTrue(!testGene.getValueAsPrintableString().contains(" \\e"))
        assertTrue(!testGene.getValueAsRawString().contains("\\\\"))
        assertTrue(testGene.getValueAsRawString().contains(" \\e"))
    }

    @Test
    fun dollarTest(){
        val dollarTest = '$'
        val testGene = StringGene("DollarGene", "Test for the Dollar ${dollarTest}escape")

        //println("Dollar => ${testGene.getValueAsPrintableString()}")
        //println("Dollar => ${testGene.getValueAsRawString()}")

        // Check that it contains the character
        assertTrue(testGene.getValueAsPrintableString().contains('$'))
        // Check that the character is escaped
        assertTrue(testGene.getValueAsPrintableString().contains("\\$"))
        //Check that it does not contain unescaped character (of the relevant type)
        assertTrue(!testGene.getValueAsPrintableString().contains(" \$"))
        // Check that the raw version does not contain the escaped character
        assertTrue(!testGene.getValueAsRawString().contains("\\$"))
    }
}