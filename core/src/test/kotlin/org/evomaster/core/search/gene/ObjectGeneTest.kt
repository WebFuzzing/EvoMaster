package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.ArrayGene
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
        Assertions.assertEquals("<anElement><integerValue>0</integerValue></anElement>", actual)
    }

    @Test
    fun testBooleanGene() {
        val gene = ObjectGene("anElement", listOf(BooleanGene("booleanValue", value = false)))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<anElement><booleanValue>false</booleanValue></anElement>", actual)
    }

    @Test
    fun testStringGene() {
        val gene = ObjectGene("anElement", listOf(StringGene("stringValue", value = "Hello World")))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<anElement><stringValue>Hello World</stringValue></anElement>", actual)
    }

    @Test
    fun testEscapedStringGene() {
        val gene = ObjectGene("anElement", listOf(StringGene("stringValue", value = "<xml>This should be escaped</xml>")))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        Assertions.assertEquals("<anElement><stringValue>&lt;xml&gt;This should be escaped&lt;/xml&gt;</stringValue></anElement>", actual)
    }

    @Test
    fun testSingleFieldObjectIsNested() {
        // Regression: ObjectGene with one named field must produce a child element, not inline value.
        // e.g. @XmlRootElement class DepositRequest(var amount: Int) should serialize as
        // <depositRequest><amount>5</amount></depositRequest>, not <depositRequest>5</depositRequest>.
        val gene = ObjectGene("depositRequest", listOf(IntegerGene("amount", value = 5)))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<depositRequest><amount>5</amount></depositRequest>", actual)
    }

    @Test
    fun testHashTextFieldIsInline() {
        // A field named "#text" represents direct text content.
        val gene = ObjectGene("element", listOf(StringGene("#text", value = "hello")))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<element>hello</element>", actual)
    }

    @Test
    fun testManyFields() {
        val child0 = ObjectGene("child0", listOf())
        val child1 = ObjectGene("child1", listOf())
        val gene = ObjectGene("parent", listOf(child0, child1))
        val actual = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<parent><child0></child0><child1></child1></parent>", actual)
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

    @Test
    fun testValueAsContent() {

        val root = ObjectGene(
            name = "device",
            listOf(
                StringGene("#text", "XPhone"),
                ObjectGene(
                    name = "location",
                    listOf(
                        StringGene("country", "AR"),
                        ObjectGene(
                            name = "gps",
                            listOf(
                                IntegerGene("#text", 12),
                                IntegerGene("lon", 34)
                            )
                        )
                    )
                )
            )
        )

        val actual = root.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected =
            "<device>XPhone" +
                    "<location>" +
                        "<country>AR</country>" +
                        "<gps>12" +
                            "<lon>34</lon>" +
                        "</gps>" +
                    "</location>" +
            "</device>"
        assertEquals(expected, actual)
    }

    @Test
    fun testXmlArrayPrinting() {

        val item1 = StringGene("sarasa1", "yC")
        val item2 = StringGene("lala2", "2ctkEeIof")

        val array = ArrayGene("photoUrls", StringGene("item"), elements = mutableListOf(item1, item2))

        val root = ObjectGene(name = "root", fields = listOf(array))

        val xml = root.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("<root><photoUrls><sarasa1>yC</sarasa1><lala2>2ctkEeIof</lala2></photoUrls></root>", xml)
    }
}