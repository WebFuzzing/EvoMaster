package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateTimeGeneTest {

    @Test
    fun testDefaultFormat() {
        val gene = DateTimeGene("dateTime")
        gene.date.apply {
            year.value = 1978
            month.value = 7
            day.value = 31
        }
        gene.time.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        assertEquals(
                "1978-07-31T03:04:05",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testISOLocalDateTimeFormat() {
        val gene = DateTimeGene("dateTime",
                dateTimeGeneFormat = DateTimeGene.DateTimeGeneFormat.ISO_LOCAL_DATE_TIME_FORMAT
        )
        gene.date.apply {
            year.value = 1978
            month.value = 7
            day.value = 31
        }
        gene.time.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        assertEquals(
                "1978-07-31T03:04:05",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testDefaultDateTimeFormat() {
        val gene = DateTimeGene("dateTime",
                dateTimeGeneFormat = DateTimeGene.DateTimeGeneFormat.DEFAULT_DATE_TIME
        )
        gene.date.apply {
            year.value = 1978
            month.value = 7
            day.value = 31
        }
        gene.time.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        assertEquals(
                "1978-07-31 03:04:05",
                gene.getValueAsRawString()
        )
    }
}