package org.evomaster.core.search.gene.mongo

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class EJSONOutputModeTest {
    @Test
    fun testStringGene() {
        val gene = StringGene("someField", value = "someValue")
        assertEquals("\"someValue\"", gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON))
    }

    @Test
    fun testIntegerGene() {
        val gene = IntegerGene("someField", value = 1)
        assertEquals("{\"\$numberInt\":\"1\"}", gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON))
    }

    @Test
    fun testDoubleGene() {
        val gene = DoubleGene("someField", value = 1.0)
        assertEquals("{\"\$numberDouble\":\"1.0\"}", gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON))
    }

    @Test
    fun testLongGene() {
        val gene = LongGene("someField", value = 1L)
        assertEquals("{\"\$numberLong\":\"1\"}", gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON))
    }

    @Test
    fun testBooleanGene() {
        val gene = BooleanGene("Boolean", value = false)
        assertEquals(
            "false",
            gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON)
        )
    }

    @Test
    fun testDateGene() {
        val gene = DateGene("someField")
        val millis = LocalDate.of(2016, 3, 12).atStartOfDay().atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        assertEquals("{\"\$date\":{\"\$numberLong\":\"$millis\"}}", gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON))
    }

    @Test
    fun testObjectGene() {
        val gene = ObjectGene("Object", listOf(LongGene("someField", value = 1L)))
        assertEquals(
            "{\"someField\":{\"\$numberLong\":\"1\"}}",
            gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON)
        )
    }

    @Test
    fun testArrayGene() {
        val gene = ArrayGene("Array", template = LongGene("Long", 1L), elements = mutableListOf(LongGene("Long", 1L)))
        assertEquals(
            "[{\"\$numberLong\":\"1\"}]",
            gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON)
        )
    }

    @Test
    fun testEscapedString() {
        val gene = ObjectGene("Object", listOf(StringGene("someStringField", value = "\\")))
        val ejsonString = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON)
        assertEquals(
            "{\"someStringField\":\"\\\\\"}", ejsonString
        )
    }

    @Test
    fun testAnotherEscapedString() {
        val gene = ObjectGene("Object", listOf(StringGene("someStringField", value = "\"")))
        val ejsonString = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.EJSON)
        assertEquals(
            "{\"someStringField\":\"\\\"\"}", ejsonString
        )
    }
}

