package org.evomaster.core.search.gene

import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test

class FlatViewTest {


    @Test
    fun testExcludeBooleanGene() {
        val gene = BooleanGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }

    @Test
    fun testExcludeTimeStampGene() {
        val gene = SqlTimestampGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }

    @Test
    fun testExcludeIntegerGene() {
        val gene = IntegerGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }

    @Test
    fun testExcludeFloatGene() {
        val gene = FloatGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }

    @Test
    fun testExcludeDateGene() {
        val gene = DateGene("gene")
        assertTrue(gene.flatView { true }.isEmpty());
    }


    @Test
    fun testExcludeSqlJSONGene() {
        val gene = SqlJSONGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }

    @Test
    fun testExcludeSqlUUIDGene() {
        val gene = SqlUUIDGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }

    @Test
    fun testExcludeSqlXMLGene() {
        val gene = SqlXMLGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }


    @Test
    fun testExcludeStringGene() {
        val gene = StringGene("gene")
        assertTrue(gene.flatView { true }.isEmpty())
    }

}