package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.*

internal class GeneUtilsTest {

    @Test
    fun testPadding() {

        val x = 9
        val res = GeneUtils.padded(x, 2)

        assertEquals("09", res)
    }


    @Test
    fun testPaddingNegative() {

        val x = -2
        val res = GeneUtils.padded(x, 3)

        assertEquals("-02", res)

    }

    @Test
    fun testRepairDefaultDateGene() {
        val gene = DateGene("date")
        GeneUtils.repairGenes(listOf(gene))
        GregorianCalendar(gene.year.value, gene.month.value, gene.day.value, 0, 0)
    }

    @Test
    fun testRepairBrokenDateGene() {
        val gene = DateGene("date", IntegerGene("year", 1998), IntegerGene("month", 4), IntegerGene("day", 31))
        GeneUtils.repairGenes(gene.flatView())
        GregorianCalendar(gene.year.value, gene.month.value, gene.day.value, 0, 0)
        assertEquals(1998, gene.year.value)
        assertEquals(4, gene.month.value)
        assertEquals(30, gene.day.value)
    }

    @Test
    fun testRepairBrokenSqlTimestampGene() {
        val dateGene = DateGene("date", IntegerGene("year", 1998), IntegerGene("month", 4), IntegerGene("day", 31))
        val timeGene = TimeGene("time", IntegerGene("hour", 23), IntegerGene("minute", 13), IntegerGene("second", 41), false)
        val sqlTimestampGene = SqlTimestampGene("timestamp", dateGene, timeGene)

        GeneUtils.repairGenes(sqlTimestampGene.flatView())
        sqlTimestampGene.apply {
            GregorianCalendar(this.date.year.value, this.date.month.value, this.date.day.value, this.time.hour.value, this.time.minute.value)
        }
        sqlTimestampGene.date.apply {
            assertEquals(1998, this.year.value)
            assertEquals(4, this.month.value)
            assertEquals(30, this.day.value)
        }
        sqlTimestampGene.time.apply {
            assertEquals(23, this.hour.value)
            assertEquals(13, this.minute.value)
            assertEquals(41, this.second.value)
        }
    }

    @Test
    fun testFlatViewWithExcludeDateGene(){
        val dateGene = DateGene("date", IntegerGene("year", 1998), IntegerGene("month", 4), IntegerGene("day", 31))
        val timeGene = TimeGene("time", IntegerGene("hour", 23), IntegerGene("minute", 13), IntegerGene("second", 41), false)
        val sqlTimestampGene = SqlTimestampGene("timestamp", dateGene, timeGene)

        val excludePredicate = {gene : Gene -> (gene is DateGene)}
        sqlTimestampGene.flatView(excludePredicate).apply {
            assertFalse(contains(dateGene.year))
            assertFalse(contains(dateGene.month))
            assertFalse(contains(dateGene.day))
            assert(contains(dateGene))
            assert(contains(timeGene))
            assert(contains(timeGene.hour))
            assert(contains(timeGene.minute))
            assert(contains(timeGene.second))
        }
    }


}