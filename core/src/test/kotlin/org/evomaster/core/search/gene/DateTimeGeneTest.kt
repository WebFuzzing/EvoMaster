package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun testCopyOfGeneDefaultTimeFormat() {
        val gene0 = DateTimeGene("dateTime",
                dateTimeGeneFormat = DateTimeGene.DateTimeGeneFormat.DEFAULT_DATE_TIME
        )
        val copyOfGene0 = gene0.copy()
        assertEquals(copyOfGene0.name, gene0.name)
        assertTrue(copyOfGene0 is DateTimeGene)
        copyOfGene0 as DateTimeGene
        copyOfGene0.date.apply {
            assertEquals(year.toInt(),
                    gene0.date.year.toInt())
            assertEquals(month.toInt(),
                    gene0.date.month.toInt())
            assertEquals(day.toInt(),
                    gene0.date.day.toInt())
        }
        copyOfGene0.time.apply {
            assertEquals(hour.toInt(),
                    gene0.time.hour.toInt())
            assertEquals(minute.toInt(),
                    gene0.time.minute.toInt())
            assertEquals(second.toInt(),
                    gene0.time.second.toInt())

        }
        assertEquals(
                DateTimeGene.DateTimeGeneFormat.DEFAULT_DATE_TIME,
                copyOfGene0.dateTimeGeneFormat
        )
    }

    @Test
    fun testCopyOfGeneISOLocalTimeFormat() {
        val gene0 = DateTimeGene("dateTime",
                dateTimeGeneFormat = DateTimeGene.DateTimeGeneFormat.ISO_LOCAL_DATE_TIME_FORMAT
        )
        val copyOfGene0 = gene0.copy()
        assertEquals(copyOfGene0.name, gene0.name)
        assertTrue(copyOfGene0 is DateTimeGene)
        copyOfGene0 as DateTimeGene
        copyOfGene0.date.apply {
            assertEquals(year.toInt(),
                    gene0.date.year.toInt())
            assertEquals(month.toInt(),
                    gene0.date.month.toInt())
            assertEquals(day.toInt(),
                    gene0.date.day.toInt())
        }
        copyOfGene0.time.apply {
            assertEquals(hour.toInt(),
                    gene0.time.hour.toInt())
            assertEquals(minute.toInt(),
                    gene0.time.minute.toInt())
            assertEquals(second.toInt(),
                    gene0.time.second.toInt())

        }
        assertEquals(
                DateTimeGene.DateTimeGeneFormat.ISO_LOCAL_DATE_TIME_FORMAT,
                copyOfGene0.dateTimeGeneFormat
        )
    }


    @Test
    fun testCopyValueFrom() {
        val gene0 = DateTimeGene("dateTime",
                dateTimeGeneFormat = DateTimeGene.DateTimeGeneFormat.ISO_LOCAL_DATE_TIME_FORMAT
        )
        val gene1 = DateTimeGene("dateTime",
                dateTimeGeneFormat = DateTimeGene.DateTimeGeneFormat.DEFAULT_DATE_TIME
        )
        gene1.copyValueFrom(gene0)

        assertEquals(
            DateTimeGene.DateTimeGeneFormat.ISO_LOCAL_DATE_TIME_FORMAT,
                gene0.dateTimeGeneFormat)
        assertEquals(
            DateTimeGene.DateTimeGeneFormat.DEFAULT_DATE_TIME,
                gene1.dateTimeGeneFormat)
    }
}