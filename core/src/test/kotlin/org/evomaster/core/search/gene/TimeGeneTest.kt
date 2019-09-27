package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimeGeneTest {

    @Test
    fun testDefaultTimeGeneFormat() {
        val gene = TimeGene("time")

        gene.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        assertEquals(
                "03:04:05.000Z",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testTimeWithMillisFormat() {
        val gene = TimeGene("time",
                timeGeneFormat = TimeGene.TimeGeneFormat.TIME_WITH_MILLISECONDS
        )
        gene.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        assertEquals(
                "03:04:05.000Z",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testDefaultDateTimeFormat() {
        val gene = TimeGene("time",
                timeGeneFormat = TimeGene.TimeGeneFormat.ISO_LOCAL_DATE_FORMAT
        )
        gene.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        assertEquals(
                "03:04:05",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testCopyOfISOLocalTimeFormat() {
        val gene = TimeGene("time",
                timeGeneFormat = TimeGene.TimeGeneFormat.ISO_LOCAL_DATE_FORMAT
        )
        gene.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        val copy = gene.copy()
        assertTrue(copy is TimeGene)
        copy as TimeGene

        copy.apply {
            assertEquals(3, hour.value)
            assertEquals(4, minute.value)
            assertEquals(5, second.value)
            assertEquals(TimeGene.TimeGeneFormat.ISO_LOCAL_DATE_FORMAT, timeGeneFormat)
        }
    }

    @Test
    fun testCopyOfTimeWithMillisFormat() {
        val gene = TimeGene("time",
                timeGeneFormat = TimeGene.TimeGeneFormat.TIME_WITH_MILLISECONDS
        )
        gene.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        val copy = gene.copy()
        assertTrue(copy is TimeGene)
        copy as TimeGene

        copy.apply {
            assertEquals(3, hour.value)
            assertEquals(4, minute.value)
            assertEquals(5, second.value)
            assertEquals(TimeGene.TimeGeneFormat.TIME_WITH_MILLISECONDS, timeGeneFormat)
        }
    }
    @Test
    fun testCopyValueFrom() {
        val gene0 = TimeGene("time",
                timeGeneFormat = TimeGene.TimeGeneFormat.TIME_WITH_MILLISECONDS
        )
        gene0.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        val gene1 = TimeGene("time",
                timeGeneFormat = TimeGene.TimeGeneFormat.ISO_LOCAL_DATE_FORMAT
        )
        gene1.copyValueFrom(gene0)

        gene1.apply {
            assertEquals(3, hour.value)
            assertEquals(4, minute.value)
            assertEquals(5, second.value)
            assertEquals(TimeGene.TimeGeneFormat.ISO_LOCAL_DATE_FORMAT, timeGeneFormat)
        }
    }



}