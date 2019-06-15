package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ObjectGeneTest {


    @Test
    fun testXMLPrintable() {
        val parentElement = ObjectGene("parentElement", listOf())
        val xmlString = parentElement.getValueAsPrintableString(mode = "xml")
        Assertions.assertEquals("<parentElement></parentElement>", xmlString)
    }

    @Test
    fun testNestedXMLPrintable() {
        val childElement = ObjectGene("childElement", listOf())
        val parentElement = ObjectGene("parentElement", listOf(childElement))
        val xmlString = parentElement.getValueAsPrintableString(mode = "xml")
        Assertions.assertEquals("<parentElement><childElement></childElement></parentElement>", xmlString)
    }

    @Test
    fun testManyNestedXMLPrintable() {
        val level2 = ObjectGene("level2", listOf())
        val level1 = ObjectGene("level1", listOf(level2))
        val level0 = ObjectGene("level0", listOf(level1))
        val xmlString = level0.getValueAsPrintableString(mode = "xml")
        Assertions.assertEquals("<level0><level1><level2></level2></level1></level0>", xmlString)
    }
}