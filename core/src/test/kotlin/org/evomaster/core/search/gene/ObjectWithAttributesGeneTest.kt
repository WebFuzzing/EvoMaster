package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        val expected =
            "<parent attrib1=\"true\">" +
                "<child1 attrib2=\"-1\" attrib3=\"bar\">42</child1>" +
                "<child2>foo</child2>" +
            "</parent>"
        Assertions.assertEquals(expected, actual)
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

        Assertions.assertEquals(expected, actual)
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

        Assertions.assertEquals(expected, actual)
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

        Assertions.assertEquals(expected, actual)
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
        // Both attributes and content are escaped once via getValueAsPrintableString (XML mode)
        val expected = "<x attr=\"&quot;&lt;&gt;&amp;&apos;\">&quot;&lt;&gt;&amp;&apos;</x>"

        Assertions.assertEquals(expected, actual)
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

        Assertions.assertEquals(expected, actual)
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

        Assertions.assertEquals(expected, actual)
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

        Assertions.assertEquals(expected, actual)
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
            "<root id=\"root1\">" +
                "<device>" +
                    "<model>XPhone</model>" +
                    "<location country=\"AR\">" +
                        "<gps>" +
                            "<lat>12</lat>" +
                            "<lon>34</lon>" +
                        "</gps>" +
                    "</location>" +
                "</device>" +
            "</root>"
        Assertions.assertEquals(expected, actual)
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
            "<device>" +
                    "<model>XPhone</model>" +
                    "<location country=\"AR\">" +
                        "<gps>" +
                            "<lat>12</lat>" +
                            "<lon>34</lon>" +
                        "</gps>" +
                    "</location>" +
            "</device>"
        Assertions.assertEquals(expected, actual)
    }

    //tests from ObjectGene
    @Test
    fun testBooleanSelectionBase(){

        val foo = StringGene("foo")
        val bar = IntegerGene("bar")
        val gene = ObjectWithAttributesGene("parent", listOf(foo, bar))

        val selection = GeneUtils.getBooleanSelection(gene)

        val actual = selection.getValueAsPrintableString(mode = GeneUtils.EscapeMode.BOOLEAN_SELECTION_MODE)

        Assertions.assertEquals("{foo,bar}", actual)
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

        Assertions.assertEquals("{foo,bar,nested{hello}}", actual)
    }

    @Test
    fun testTextAsAttributeLogsWarning() {
        // When #text is incorrectly specified as an attribute in the schema,
        // the class logs a warning but continues processing (does not throw an exception)
        // This tests that the object is created successfully despite the invalid schema
        val obj = ObjectWithAttributesGene(
            name = "node",
            fixedFields = listOf(
                StringGene("#text", "value")
            ),
            isFixed = true,
            attributeNames = setOf("#text")
        )

        // Object should be created successfully - no exception thrown
        Assertions.assertNotNull(obj)

        // The XML output will treat #text as an attribute (even though it's invalid schema)
        val actual = obj.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected = "<node #text=\"value\"></node>"

        Assertions.assertEquals(expected, actual)
    }


    @Test
    fun testArrayGeneWithAttributesInsideObjectGene() {

        val root = ObjectGene(
            name = "project",
            listOf(
                StringGene("code", "PRJ-001"),
                ArrayGene(
                    name = "members",
                    template = ObjectWithAttributesGene(
                        name = "member",
                        fixedFields = listOf(
                            StringGene("id", "M001"),
                            StringGene("name", "John"),
                            IntegerGene("age", 30)
                        ),
                        isFixed = false,
                        attributeNames = setOf("id"),
                        additionalFields = null
                    ),
                    elements = mutableListOf(
                        ObjectWithAttributesGene(
                            name = "member",
                            fixedFields = listOf(
                                StringGene("id", "M001"),
                                StringGene("name", "Alice"),
                                IntegerGene("age", 25)
                            ),
                            isFixed = false,
                            attributeNames = setOf("id"),
                            additionalFields = null
                        ),
                        ObjectWithAttributesGene(
                            name = "member",
                            fixedFields = listOf(
                                StringGene("id", "M002"),
                                StringGene("name", "Bob"),
                                IntegerGene("age", 35)
                            ),
                            isFixed = false,
                            attributeNames = setOf("id"),
                            additionalFields = null
                        )
                    )
                )
            )
        )

        val actual = root.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        val expected =
            "<project>" +
                    "<code>PRJ-001</code>" +
                    "<members>" +
                        "<member id=\"M001\">" +
                            "<name>Alice</name>" +
                            "<age>25</age>" +
                        "</member>" +
                        "<member id=\"M002\">" +
                            "<name>Bob</name>" +
                            "<age>35</age>" +
                        "</member>" +
                    "</members>" +
            "</project>"
        Assertions.assertEquals(expected, actual)
    }
}