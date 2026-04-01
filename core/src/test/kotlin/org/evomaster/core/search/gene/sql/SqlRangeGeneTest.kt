package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlRangeGeneTest {

    companion object {
        private fun createRandomnessWithSeed(seed: Long): Randomness {
            return Randomness().apply { updateSeed(seed) }
        }
    }
    @Test
    fun testRepairOnInitialization() {
        val left = IntegerGene("left", value = 10)
        val right = IntegerGene("right", value = 5)

        // SqlRangeGene should swap them in init { repairGeneIfNeeded() }
        val range = SqlRangeGene("range", template = IntegerGene("int"), left = left, right = right)

        assertEquals(5, (range.flatView().find { it.name == "left" } as IntegerGene).value)
        assertEquals(10, (range.flatView().find { it.name == "right" } as IntegerGene).value)
        assertTrue(range.isLocallyValid())
    }

    @Test
    fun testPrintableString() {
        val left = IntegerGene("left", value = 5)
        val right = IntegerGene("right", value = 10)
        val isLeftClosed = BooleanGene("isLeftClosed", value = true)
        val isRightClosed = BooleanGene("isRightClosed", value = false)

        val range = SqlRangeGene("range", template = IntegerGene("int"),
            isLeftClosed = isLeftClosed, left = left, right = right, isRightClosed = isRightClosed)

        assertEquals("\"[ 5 , 10 )\"", range.getValueAsPrintableString())

        isLeftClosed.value = false
        assertEquals("\"( 5 , 10 )\"", range.getValueAsPrintableString())

        isRightClosed.value = true
        assertEquals("\"( 5 , 10 ]\"", range.getValueAsPrintableString())

        isLeftClosed.value = true
        assertEquals("\"[ 5 , 10 ]\"", range.getValueAsPrintableString())
    }

    @Test
    fun testEmptyRangePrintableString() {
        val left = IntegerGene("left", value = 5)
        val right = IntegerGene("right", value = 5)

        // [5, 5] is NOT empty in Postgres range types, it contains {5}
        val range1 = SqlRangeGene("range", template = IntegerGene("int"),
            isLeftClosed = BooleanGene("L", true), left = left, right = right, isRightClosed = BooleanGene("R", true))
        assertEquals("\"[ 5 , 5 ]\"", range1.getValueAsPrintableString())

        // (5, 5], [5, 5), (5, 5) ARE empty
        val range2 = SqlRangeGene("range", template = IntegerGene("int"),
            isLeftClosed = BooleanGene("L", false), left = left, right = right, isRightClosed = BooleanGene("R", true))
        assertEquals("\"empty\"", range2.getValueAsPrintableString())

        val range3 = SqlRangeGene("range", template = IntegerGene("int"),
            isLeftClosed = BooleanGene("L", true), left = left, right = right, isRightClosed = BooleanGene("R", false))
        assertEquals("\"empty\"", range3.getValueAsPrintableString())

        val range4 = SqlRangeGene("range", template = IntegerGene("int"),
            isLeftClosed = BooleanGene("L", false), left = left, right = right, isRightClosed = BooleanGene("R", false))
        assertEquals("\"empty\"", range4.getValueAsPrintableString())
    }

    @Test
    fun testCopyAndEquality() {
        val range = SqlRangeGene("range", template = IntegerGene("int"))
        val rand = createRandomnessWithSeed(42)
        range.randomize(rand, false)

        val copy = range.copy() as SqlRangeGene<*>

        assertTrue(range.containsSameValueAs(copy))
        assertEquals(range.getValueAsPrintableString(), copy.getValueAsPrintableString())

        // Mutate copy
        val leftGene = copy.flatView().find { it.name == "left" } as IntegerGene
        leftGene.value = (leftGene.value ?: 0) + 1
        copy.repair()

        assertFalse(range.containsSameValueAs(copy))
    }



    @Test
    fun testUnsafeCopyValueFrom() {
        val range1 = SqlRangeGene("range1", template = IntegerGene("int"))
        range1.randomize(createRandomnessWithSeed(100), false)

        val range2 = SqlRangeGene("range2", template = IntegerGene("int"))
        range2.randomize(createRandomnessWithSeed(42), false)

        assertFalse(range1.containsSameValueAs(range2))

        val success = range1.unsafeCopyValueFrom(range2)
        assertTrue(success)
        assertTrue(range1.containsSameValueAs(range2))
    }

}
