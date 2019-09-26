package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DateGeneTest {

    @Test
    fun testDefaultFormat() {
        val gene = DateGene("date")
        gene.apply {
            year.value = 1978
            month.value = 7
            day.value = 31
        }
        assertEquals(
                "1978-07-31",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testISOLocalDateFormat() {
        val gene = DateGene("date",
                dateGeneFormat = DateGene.DateGeneFormat.ISO_LOCAL_DATE_FORMAT)
        gene.apply {
            year.value = 1978
            month.value = 7
            day.value = 31
        }
        assertEquals(
                "1978-07-31",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testCopy() {
        val gene = DateGene("date",
                onlyValidDates = false,
                dateGeneFormat = DateGene.DateGeneFormat.ISO_LOCAL_DATE_FORMAT)
        val copy = gene.copy()
        assertTrue(copy is DateGene)
        copy as DateGene
        assertEquals(false, copy.onlyValidDates)
        assertEquals(DateGene.DateGeneFormat.ISO_LOCAL_DATE_FORMAT, copy.dateGeneFormat)
        assertEquals(gene.year.toInt(),copy.year.toInt())
        assertEquals(gene.month.toInt(),copy.month.toInt())
        assertEquals(gene.day.toInt(),copy.day.toInt())
    }
}