package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.FlexibleMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.FlexibleGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MapGeneTest{

    @Test
    fun test(){
        val s1 = PairGene.createStringPairGene(StringGene("string_1"), true)
        val s2 = PairGene.createStringPairGene(StringGene("string_2"))
        val targetFormat = OutputFormat.KOTLIN_JUNIT_5

        val map = FixedMapGene("PrintableMap", PairGene.createStringPairGene(StringGene("map")), 7, null, mutableListOf(s1, s2))
        val mapstring = map.getValueAsPrintableString(targetFormat = targetFormat)

        assertTrue(mapstring.contains(s1.getValueAsPrintableString(targetFormat = targetFormat), ignoreCase = true))
        assertTrue(mapstring.contains(s2.getValueAsPrintableString(targetFormat = targetFormat), ignoreCase = true))
    }

    @Test
    fun testIntKeyAndContainKey(){
        val intKey1 = IntegerGene("key_1", 1)
        val strValue1 = StringGene("key_1", "foo")
        val intKey2 = IntegerGene("key_2", 2)
        val strValue2 = strValue1.copy() as StringGene

        val targetFormat = OutputFormat.KOTLIN_JUNIT_5

        val map = FixedMapGene("PrintableMap", intKey1.copy() as IntegerGene, strValue1.copy() as StringGene, 7)
        val s1 = PairGene("key_1",intKey1, strValue1)
        map.addElement(s1)
        val s2 = PairGene("key_2", intKey2, strValue2)
        map.addElement(s2)

        val mapstring = map.getValueAsPrintableString(targetFormat = targetFormat)
        assertEquals("1:\"foo\"", s1.getValueAsPrintableString(targetFormat = targetFormat))
        assertEquals("2:\"foo\"", s2.getValueAsPrintableString(targetFormat = targetFormat))

        assertTrue(mapstring.contains("\"1\":\"foo\"", ignoreCase = true))
        assertTrue(mapstring.contains("\"2\":\"foo\"", ignoreCase = true))

        val intKey3 = IntegerGene("key_3", 2)
        val strValue3 = strValue1.copy() as StringGene
        val s3 = PairGene("key_3", intKey3, strValue3)
        assertTrue(map.containsKey(s3))
        intKey3.value = 3
        assertFalse(map.containsKey(s3))

        val intKey4 = IntegerGene("key_4", 1)
        val strValue4 = strValue1.copy() as StringGene
        strValue4.value = "bar"
        val s4 = PairGene("key_4", intKey4, strValue4)
        assertTrue(map.containsKey(s4))
        map.addElement(s4)
        //replace the existing pair with s4
        assertTrue(map.getValueAsPrintableString(targetFormat = targetFormat).contains("\"1\":\"bar\"", ignoreCase = true))
    }

    @Test
    fun testEnumKeyStringValue(){
        val enumValues = listOf("ONE", "TWO", "THREE")

        val enumKey0 = EnumGene("key_1", enumValues)
        enumKey0.index = enumKey0.values.indexOf("ONE")
        val strValue0 = StringGene("key_1", "foo")

        val enumKey1 = EnumGene("key_2", enumValues)
        enumKey1.index = enumKey0.values.indexOf("TWO")
        val strValue1 = StringGene("key_2", "foo")


        val map = FixedMapGene("PrintableMap", enumKey0.copy() as EnumGene<*>, strValue0.copy() as StringGene, 7)
        val s1 = PairGene("key_1",enumKey0, strValue0)
        map.addElement(s1)
        val s2 = PairGene("key_2", enumKey1, strValue1)
        map.addElement(s2)

        val targetFormat = OutputFormat.KOTLIN_JUNIT_5

        val mapstring = map.getValueAsPrintableString(targetFormat = targetFormat)
        assertEquals("\"ONE\":\"foo\"", s1.getValueAsPrintableString(targetFormat = targetFormat))
        assertEquals("\"TWO\":\"foo\"", s2.getValueAsPrintableString(targetFormat = targetFormat))


        assertTrue(mapstring.contains("\"ONE\":\"foo\"", ignoreCase = true))
        assertTrue(mapstring.contains("\"TWO\":\"foo\"", ignoreCase = true))


        val enumKey1New = EnumGene("key_2", enumValues)
        enumKey1New.index = enumKey0.values.indexOf("TWO")
        val strValue1New = StringGene("key_2", "bar")
        val s2New = PairGene("key_2",enumKey1New as EnumGene<*>, strValue1New)
        assertTrue(map.containsKey(s2New))
        map.addElement(s2New)
        //replace the existing pair with s2New
        assertTrue(map.getValueAsPrintableString(targetFormat = targetFormat).contains("\"TWO\":\"bar\"", ignoreCase = true))
        assertEquals(2, map.getAllElements().size)
    }

    @Test
    fun testFlexibleGeneReplace(){
        val integerGene = IntegerGene("int", 1)
        val fMap = FlexibleMapGene("intMap", StringGene("key"), integerGene, null)

        val stringGene = StringGene("string", "bar")
        val element = fMap.template.copy() as PairGene<*, FlexibleGene>
        element.second.replaceGeneTo(stringGene)
        assertEquals(element.second.gene, stringGene)
        fMap.addChild(element)

        assertEquals(1, fMap.getAllElements().size)
        assertEquals(element, fMap.getViewOfChildren().first() as PairGene<*,*>)
    }
}