package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
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
}