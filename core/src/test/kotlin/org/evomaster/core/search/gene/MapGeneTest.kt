package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MapGeneTest{

    @Test
    fun test(){
        val s1 = PairGene.createStringPairGene(StringGene("string_1"), true)
        val s2 = PairGene.createStringPairGene(StringGene("string_2"))
        val targetFormat = OutputFormat.KOTLIN_JUNIT_5

        val map = MapGene("PrintableMap", PairGene.createStringPairGene(StringGene("map")), 7, mutableListOf(s1, s2))
        val mapstring = map.getValueAsPrintableString(targetFormat = targetFormat)

        assertTrue(mapstring.contains(s1.getValueAsPrintableString(targetFormat = targetFormat), ignoreCase = true))
        assertTrue(mapstring.contains(s2.getValueAsPrintableString(targetFormat = targetFormat), ignoreCase = true))
    }

    @Test
    fun testIntKeyAndContainKey(){
        val intKey1 = IntegerGene("key_1", 1)
        val strValue1 = StringGene("key_1", "foo")
        val intKey2 = IntegerGene("key_2", 2)
        val strValue2 = strValue1.copyContent() as StringGene

        val targetFormat = OutputFormat.KOTLIN_JUNIT_5

        val map = MapGene("PrintableMap", intKey1.copy() as IntegerGene, strValue1.copy() as StringGene, 7)
        val s1 = PairGene("key_1",intKey1, strValue1)
        map.addElements(s1)
        val s2 = PairGene("key_2", intKey2, strValue2)
        map.addElements(s2)

        val mapstring = map.getValueAsPrintableString(targetFormat = targetFormat)
        assertEquals("1:\"foo\"", s1.getValueAsPrintableString(targetFormat = targetFormat))
        assertEquals("2:\"foo\"", s2.getValueAsPrintableString(targetFormat = targetFormat))

        assertTrue(mapstring.contains("\"1\":\"foo\"", ignoreCase = true))
        assertTrue(mapstring.contains("\"2\":\"foo\"", ignoreCase = true))

        val intKey3 = IntegerGene("key_3", 2)
        val strValue3 = strValue1.copyContent() as StringGene
        val s3 = PairGene("key_3", intKey3, strValue3)
        assertTrue(map.containsKey(s3))
        intKey3.value = 3
        assertFalse(map.containsKey(s3))
    }
}