package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ObjectGeneTest {


    @Test
    fun testXMLEmpty() {
        val parentElement = ObjectGene("parentElement", listOf())
        val xmlString = parentElement.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<parentElement></parentElement>", xmlString)
    }

    @Test
    fun testNestedXMLPrintable() {
        val childElement = ObjectGene("childElement", listOf())
        val parentElement = ObjectGene("parentElement", listOf(childElement))
        val xmlString = parentElement.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<parentElement><childElement></childElement></parentElement>", xmlString)
    }

    @Test
    fun testManyNestedXMLPrintable() {
        val level2 = ObjectGene("level2", listOf())
        val level1 = ObjectGene("level1", listOf(level2))
        val level0 = ObjectGene("level0", listOf(level1))
        val xmlString = level0.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<level0><level1><level2></level2></level1></level0>", xmlString)
    }

    @Test
    fun testIntegerGene() {
        val gene = ObjectGene("anElement", listOf(IntegerGene("integerValue", value = 0)))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<anElement>0</anElement>", actual)
    }

    @Test
    fun testBooleanGene() {
        val gene = ObjectGene("anElement", listOf(BooleanGene("booleanValue", value = false)))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<anElement>false</anElement>", actual)
    }

    @Test
    fun testStringGene() {
        val gene = ObjectGene("anElement", listOf(StringGene("stringValue", value = "Hello World")))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<anElement>Hello World</anElement>", actual)
    }

    @Test
    fun testEscapedStringGene() {
        val gene = ObjectGene("anElement", listOf(StringGene("stringValue", value = "<xml>This should be escaped</xml>")))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<anElement>&lt;xml&gt;This should be escaped&lt;/xml&gt;</anElement>", actual)
    }

    @Test
    fun testManyFields() {
        val child0 = ObjectGene("child", listOf())
        val child1 = ObjectGene("child", listOf())
        val gene = ObjectGene("parent", listOf(child0, child1))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<parent><child></child><child></child></parent>", actual)
    }


    @Test
    fun testBooleanSelectionBase(){

        val foo = StringGene("foo")
        val bar = IntegerGene("bar")
        val gene = ObjectGene("parent", listOf(foo, bar))

        val selection = GeneUtils.getBooleanSelection(gene)

        val actual = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)

        assertEquals("{foo,bar}", actual)
    }

    @Test
    fun testBooleanSelectionNested(){

        val foo = StringGene("foo")
        val bar = IntegerGene("bar")
        val hello = StringGene("hello")
        val nested = ObjectGene("nested", listOf(hello))
        val gene = ObjectGene("parent", listOf(foo, bar, nested))

        val selection = GeneUtils.getBooleanSelection(gene)

        val actual = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)

        assertEquals("{foo,bar,nested{hello}}", actual)
    }
}