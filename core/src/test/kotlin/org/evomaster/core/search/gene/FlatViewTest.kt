package org.evomaster.core.search.gene

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class FlatViewTest {


    @Test
    fun testExcludeBooleanGene() {
        val gene = BooleanGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }

    @Test
    fun testExcludeTimeStampGene() {
        val gene = SqlTimestampGene("gene")
        assertEquals(1, gene.flatView { true }.size)

    }

    @Test
    fun testExcludeIntegerGene() {
        val gene = IntegerGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }

    @Test
    fun testExcludeFloatGene() {
        val gene = FloatGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }

    @Test
    fun testExcludeDateGene() {
        val gene = DateGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }


    @Test
    fun testExcludeSqlJSONGene() {
        val gene = SqlJSONGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }

    @Test
    fun testExcludeSqlUUIDGene() {
        val gene = SqlUUIDGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }

    @Test
    fun testExcludeSqlXMLGene() {
        val gene = SqlXMLGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }


    @Test
    fun testExcludeStringGene() {
        val gene = StringGene("gene")
        assertEquals(1, gene.flatView { true }.size)
    }

}