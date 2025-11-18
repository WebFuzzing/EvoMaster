package org.evomaster.core.search.gene.xml

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.ObjectWithAttributesGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.evomaster.core.search.gene.utils.GeneUtils

class ObjectWithAttributesGeneTest {


    @Test
    fun testXmlPrintWithAttributesAndValue() {

        val person = ObjectWithAttributesGene(
            name = "parent",
            fixedFields = listOf(
                StringGene("attrib1", value = "true"),
                ObjectWithAttributesGene(
                    name = "child1",
                    fixedFields = listOf(
                        StringGene("attrib2", value = "-1"),
                        StringGene("attrib3", value = "bar"),
                        IntegerGene("#text", value = 42)
                    ),
                    isFixed = true,
                    attributeNames = setOf("attrib2","attrib3")
                ),
                StringGene("child2", value = "foo"),
            ),
            isFixed = true,
            attributeNames = setOf("attrib1")
        )
        val actual = person.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<parent attrib1=\"true\"><child1 attrib2=\"-1\" attrib3=\"bar\">42</child1><child2>foo</child2></parent>"
        assertEquals(expected, actual)
    }

    @Test
    fun testXmlEmptyObject() {

        val obj = ObjectWithAttributesGene(
            name = "empty",
            fixedFields = emptyList(),
            isFixed = true,
            attributeNames = emptySet()
        )

        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<empty></empty>"

        assertEquals(expected, actual)
    }

    @Test
    fun testXmlEmptyAttributeValue() {

        val obj = ObjectWithAttributesGene(
            name = "person",
            fixedFields = listOf(StringGene("id", value = "")),
            isFixed = true,
            attributeNames = setOf("id")
        )

        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<person id=\"\"></person>"

        assertEquals(expected, actual)
    }

    @Test
    fun testXmlNullAttributeValue() {

        val obj = ObjectWithAttributesGene(
            name = "item",
            fixedFields = listOf(IntegerGene("code", value = null)),
            isFixed = true,
            attributeNames = setOf("code")
        )

        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<item code=\"0\"></item>"

        assertEquals(expected, actual)
    }

    @Test
    fun testXmlEscaping() {

        val obj = ObjectWithAttributesGene(
            name = "x",
            fixedFields = listOf(
                StringGene("attr", "\"<>&'"),
                StringGene("#text", "\"<>&'")
            ),
            isFixed = true,
            attributeNames = setOf("attr")
        )

        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<x attr=\"&quot;&lt;&gt;&amp;&apos;\">&quot;&lt;&gt;&amp;&apos;</x>"

        assertEquals(expected, actual)
    }

    @Test
    fun testValueAsTextOnly() {

        val obj = ObjectWithAttributesGene(
            name = "item",
            fixedFields = listOf(
                IntegerGene("#text", value = 42)
            ),
            isFixed = true,
            attributeNames = emptySet()
        )

        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<item>42</item>"

        assertEquals(expected, actual)
    }

    @Test
    fun testValueBooleanAsText() {

        val obj = ObjectWithAttributesGene(
            name = "flag",
            fixedFields = listOf(
                BooleanGene("#text", false)
            ),
            isFixed = true
        )

        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<flag>false</flag>"

        assertEquals(expected, actual)
    }

    @Test
    fun testValueEmptyString() {

        val obj = ObjectWithAttributesGene(
            name = "node",
            fixedFields = listOf(
                StringGene("#text", "")
            ),
            isFixed = true
        )

        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<node></node>"

        assertEquals(expected, actual)
    }

    @Test
    fun testDeepMixedNesting() {

        val root = ObjectWithAttributesGene(
            name = "root",
            fixedFields = listOf(
                StringGene("id", "root1"),
                ObjectGene(
                    name = "device",
                    listOf(
                        StringGene("model", "XPhone"),
                        ObjectWithAttributesGene(
                            name = "location",
                            fixedFields = listOf(
                                StringGene("country", "AR"),
                                ObjectGene(
                                    name = "gps",
                                    listOf(
                                        IntegerGene("lat", 12),
                                        IntegerGene("lon", 34)
                                    )
                                )
                            ),
                            isFixed = true,
                            attributeNames = setOf("country")
                        )
                    )
                )
            ),
            isFixed = true,
            attributeNames = setOf("id")
        )
        val actual = root.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected =
            "<root id=\"root1\"><device><model>XPhone</model><location country=\"AR\"><gps><lat>12</lat><lon>34</lon></gps></location></device></root>"
        assertEquals(expected, actual)
    }

    @Test
    fun testDeepMixedNestingStartingOG() {

        val root = ObjectGene(
            name = "device",
            listOf(
                StringGene("model", "XPhone"),
                ObjectWithAttributesGene(
                    name = "location",
                    fixedFields = listOf(
                        StringGene("country", "AR"),
                        ObjectGene(
                            name = "gps",
                            listOf(
                                IntegerGene("lat", 12),
                                IntegerGene("lon", 34)
                            )
                        )
                    ),
                    isFixed = true,
                    attributeNames = setOf("country")
                )
            )
        )

        val actual = root.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected =
            "<device><model>XPhone</model><location country=\"AR\"><gps><lat>12</lat><lon>34</lon></gps></location></device>"
        assertEquals(expected, actual)
    }

    //tests from ObjectGene
    @Test
    fun testBooleanSelectionBase(){

        val foo = StringGene("foo")
        val bar = IntegerGene("bar")
        val gene = ObjectWithAttributesGene("parent", listOf(foo, bar))

        val selection = GeneUtils.getBooleanSelection(gene)

        val actual = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)

        assertEquals("{foo,bar}", actual)
    }

    @Test
    fun testBooleanSelectionNested(){

        val foo = StringGene("foo")
        val bar = IntegerGene("bar")
        val hello = StringGene("hello")
        val nested = ObjectWithAttributesGene("nested", listOf(hello))
        val gene = ObjectWithAttributesGene("parent", listOf(foo, bar, nested))

        val selection = GeneUtils.getBooleanSelection(gene)

        val actual = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)

        assertEquals("{foo,bar,nested{hello}}", actual)
    }

    @Test
    fun testTextCannotBeAttribute() {

        val ex = org.junit.jupiter.api.assertThrows<IllegalStateException> {

            ObjectWithAttributesGene(
                name = "node",
                fixedFields = listOf(
                    StringGene("#text", "value")
                ),
                isFixed = true,
                attributeNames = setOf("#text")  // ilegal
            ).getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        }

        assertEquals("#text cannot be used as an attribute in XML", ex.message)
    }

    @Test
    fun testDuplicateChildNameThrowsException() {

        val ex = org.junit.jupiter.api.assertThrows<IllegalStateException> {

            ObjectWithAttributesGene(
                name = "node",
                fixedFields = listOf(
                    StringGene("child", "a"),
                    IntegerGene("child", 123) // duplicado
                ),
                isFixed = true,
                attributeNames = emptySet()
            ).getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        }

        assertEquals(
            "Duplicate child elements not allowed in XML: [child]",
            ex.message
        )
    }
}