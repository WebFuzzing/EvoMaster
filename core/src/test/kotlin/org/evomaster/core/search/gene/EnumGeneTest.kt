package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnumGeneTest {

    @Test
    fun testEmptyDataList(){
        val data = listOf<Int>()
        val enumGene = EnumGene("value", data)
        assertEquals(0, enumGene.values.size)
    }

    // --- String enum: JSON vs XML ---

    @Test
    fun testStringEnum_jsonMode_wrapsInQuotes() {
        val gene = EnumGene("status", listOf("ACTIVE", "INACTIVE"))
        gene.index = 0
        val result = gene.getValueAsPrintableString(mode = null)
        assertEquals("\"ACTIVE\"", result)
    }

    @Test
    fun testStringEnum_xmlMode_noQuotes() {
        val gene = EnumGene("status", listOf("ACTIVE", "INACTIVE"))
        gene.index = 0
        val result = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("ACTIVE", result)
    }

    @Test
    fun testStringEnum_xmlMode_secondValue_noQuotes() {
        val gene = EnumGene("status", listOf("ACTIVE", "INACTIVE"))
        gene.index = 1
        val result = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("INACTIVE", result)
    }

    // --- Non-string enum: XML mode should work as before ---

    @Test
    fun testIntEnum_xmlMode_noQuotes() {
        val gene = EnumGene("priority", listOf(1, 2, 3))
        gene.index = 0
        val result = gene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)
        assertEquals("1", result)
    }

    @Test
    fun testIntEnum_nullMode_noQuotes() {
        val gene = EnumGene("priority", listOf(1, 2, 3))
        gene.index = 2
        val result = gene.getValueAsPrintableString(mode = null)
        assertEquals("3", result)
    }

}