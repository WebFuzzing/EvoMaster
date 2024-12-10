package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.FormatForDatesAndTimes
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class DateTimeGeneTest {

    @Test
    fun testRandomizeRFC3339(){

        val random = Randomness()

        val gene = DateTimeGene("dateTime", format = FormatForDatesAndTimes.RFC3339, onlyValid = true)

        repeat(100){
            gene.randomize(random, tryToForceNewValue = true)
            gene.time.selectZ() // Instant is not RFC3339 compliant
            val s = gene.getValueAsRawString()
            Instant.parse(s) // must not throw exception
        }
    }


    @Test
    fun validateInstant(){

        val s = "1906-02-17T23:06:07"
        val z = "${s}Z"

        assertThrows<Exception> { Instant.parse(s) }
        Instant.parse(z)

        val gene = DateTimeGene("dateTime", format = FormatForDatesAndTimes.RFC3339)
        gene.date.apply {
            year.value = 1906
            month.value = 2
            day.value = 17
        }
        gene.time.apply {
            hour.value = 23
            minute.value = 6
            second.value = 7
            useMilliseconds(false)
            selectZ()
        }
        val x = gene.getValueAsRawString()

        Instant.parse(x)
        assertEquals(z,x)
    }


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
                format = FormatForDatesAndTimes.ISO_LOCAL
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
                format = FormatForDatesAndTimes.DATETIME
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
                format = FormatForDatesAndTimes.DATETIME
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
                FormatForDatesAndTimes.DATETIME,
                copyOfGene0.format
        )
    }

    @Test
    fun testCopyOfGeneISOLocalTimeFormat() {
        val gene0 = DateTimeGene("dateTime",
                format = FormatForDatesAndTimes.ISO_LOCAL
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
                FormatForDatesAndTimes.ISO_LOCAL,
                copyOfGene0.format
        )
    }


    @Test
    fun testCopyValueFrom() {
        val gene0 = DateTimeGene("dateTime",
                format = FormatForDatesAndTimes.ISO_LOCAL
        )
        val gene1 = DateTimeGene("dateTime",
                format = FormatForDatesAndTimes.DATETIME
        )
        gene1.copyValueFrom(gene0)

        assertEquals(
            FormatForDatesAndTimes.ISO_LOCAL,
                gene0.format)
        assertEquals(
            FormatForDatesAndTimes.DATETIME,
                gene1.format)
    }
}