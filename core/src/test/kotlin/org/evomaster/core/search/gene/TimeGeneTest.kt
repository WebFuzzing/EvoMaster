package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.datetime.FormatForDatesAndTimes
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimeGeneTest {

    @Test
    fun testDefaultTimeGeneFormat() {
        val gene = TimeGene("time", format = FormatForDatesAndTimes.RFC3339)

        gene.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
            selectZ()
            useMilliseconds(false)
        }
        assertEquals(
                "03:04:05Z",
                gene.getValueAsRawString()
        )
        assertEquals(
            true,
            gene.isValidTime()
        )
    }

    @Test
    fun testTimeWithMillisFormat() {
        val gene = TimeGene("time",
                format = FormatForDatesAndTimes.RFC3339
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
        assertEquals(
            true,
            gene.isValidTime()
        )
    }

    @Test
    fun testDefaultDateTimeFormat() {
        val gene = TimeGene("time", format = FormatForDatesAndTimes.ISO_LOCAL)
        gene.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        assertEquals(
                "03:04:05",
                gene.getValueAsRawString()
        )
        assertEquals(
            true,
            gene.isValidTime()
        )
    }

    @Test
    fun testCopyOfISOLocalTimeFormat() {
        val gene = TimeGene("time",
                format = FormatForDatesAndTimes.ISO_LOCAL
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
            assertEquals(FormatForDatesAndTimes.ISO_LOCAL, format)
            assertEquals(false, onlyValidTimes)
        }
    }

    @Test
    fun testCopyOfTimeWithMillisFormat() {
        val gene = TimeGene("time",
                format = FormatForDatesAndTimes.RFC3339
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
            assertEquals(FormatForDatesAndTimes.RFC3339, format)
            assertEquals(false, onlyValidTimes)
        }
    }
    @Test
    fun testCopyValueFrom() {
        val gene0 = TimeGene("time",
                format = FormatForDatesAndTimes.RFC3339
        )
        gene0.apply {
            hour.value = 3
            minute.value = 4
            second.value = 5
        }
        val gene1 = TimeGene("time",
                format = FormatForDatesAndTimes.ISO_LOCAL
        )
        gene1.copyValueFrom(gene0)

        gene1.apply {
            assertEquals(3, hour.value)
            assertEquals(4, minute.value)
            assertEquals(5, second.value)
            assertEquals(FormatForDatesAndTimes.ISO_LOCAL, format)
            assertEquals(false, onlyValidTimes)
        }
    }

    @Test
    fun testInvalidTime() {
        val gene = TimeGene("time")

        gene.apply {
            hour.value = -1
            minute.value = 4
            second.value = 5
        }
        assertEquals(
    false,
            gene.isValidTime()
        )
    }

    @Test
    fun testValidTime() {
        val gene = TimeGene("time", onlyValidTimes = true)
        val rand = Randomness()
        repeat (1000) {
            gene.randomize(rand, tryToForceNewValue = true)
            assertEquals(true, gene.isValidTime())
        }
    }


}