package org.evomaster.core.search.gene.cassandra

import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CqlDurationGeneTest {

    private fun duration(months: Int, days: Int, nanos: Long, negative: Boolean = false) =
        CqlDurationGene(
            "elapsed",
            months = IntegerGene("months", months),
            days = IntegerGene("days", days),
            nanos = LongGene("nanos", nanos)
        ).apply { this.negative.value = negative }

    @Test
    fun testValueIsRenderedWithTheThreeUnits() {
        assertEquals("1mo2d3ns", duration(1, 2, 3L).getValueAsRawString())
    }

    /**
     * All the amounts are written even when zero, so that the literal is never empty.
     */
    @Test
    fun testZeroDuration() {
        assertEquals("0mo0d0ns", duration(0, 0, 0L).getValueAsRawString())
    }

    /**
     * A duration literal carries at most one sign, applying to the whole value, as a duration
     * mixing signs cannot be written in CQL.
     */
    @Test
    fun testNegativeDurationHasASingleLeadingSign() {
        assertEquals("-1mo2d3ns", duration(1, 2, 3L, negative = true).getValueAsRawString())
    }

    @Test
    fun testDurationIsPositiveByDefault() {
        assertFalse(CqlDurationGene("elapsed").negative.value)
    }

    @Test
    fun testCopyKeepsAllTheComponents() {
        val gene = duration(1, 2, 3L, negative = true)
        val copy = gene.copy() as CqlDurationGene

        assertEquals(gene.getValueAsRawString(), copy.getValueAsRawString())
        assertTrue(gene.containsSameValueAs(copy))
    }

    @Test
    fun testDurationsDifferingInOneComponentAreNotTheSame() {
        val gene = duration(1, 2, 3L)

        assertFalse(gene.containsSameValueAs(duration(9, 2, 3L)))
        assertFalse(gene.containsSameValueAs(duration(1, 9, 3L)))
        assertFalse(gene.containsSameValueAs(duration(1, 2, 9L)))
        assertFalse(gene.containsSameValueAs(duration(1, 2, 3L, negative = true)))
    }

    @Test
    fun testCopyValueFrom() {
        val gene = CqlDurationGene("elapsed")

        assertTrue(gene.copyValueFrom(duration(1, 2, 3L, negative = true)))
        assertEquals("-1mo2d3ns", gene.getValueAsRawString())
    }
}