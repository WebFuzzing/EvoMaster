package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.BigIntegerGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CassandraColumnGeneBuilderTest {

    private fun buildFor(cqlType: String): Gene =
        CassandraColumnGeneBuilder.buildGene(CassandraColumn("aColumn", cqlType))

    @Test
    fun testTextTypes() {
        listOf("ascii", "text", "varchar").forEach {
            assertTrue(buildFor(it) is StringGene, "unexpected gene for $it")
        }
    }

    @Test
    fun testIntegerTypes() {
        assertTrue(buildFor("tinyint") is IntegerGene)
        assertTrue(buildFor("smallint") is IntegerGene)
        assertTrue(buildFor("int") is IntegerGene)
    }

    @Test
    fun testBoundsOfNarrowIntegerTypes() {
        val tinyint = buildFor("tinyint") as IntegerGene
        assertEquals(Byte.MIN_VALUE.toInt(), tinyint.min)
        assertEquals(Byte.MAX_VALUE.toInt(), tinyint.max)

        val smallint = buildFor("smallint") as IntegerGene
        assertEquals(Short.MIN_VALUE.toInt(), smallint.min)
        assertEquals(Short.MAX_VALUE.toInt(), smallint.max)
    }

    @Test
    fun testOtherNumericTypes() {
        assertTrue(buildFor("bigint") is LongGene)
        assertTrue(buildFor("varint") is BigIntegerGene)
        assertTrue(buildFor("decimal") is BigDecimalGene)
        assertTrue(buildFor("float") is FloatGene)
        assertTrue(buildFor("double") is DoubleGene)
    }

    @Test
    fun testBooleanAndUuidTypes() {
        assertTrue(buildFor("boolean") is BooleanGene)
        assertTrue(buildFor("uuid") is UUIDGene)
    }

    @Test
    fun testTemporalTypes() {
        assertTrue(buildFor("timestamp") is DateTimeGene)
        assertTrue(buildFor("date") is DateGene)
        assertTrue(buildFor("time") is TimeGene)
    }

    @Test
    fun testTypeNameIsNormalized() {
        assertTrue(buildFor(" TEXT ") is StringGene)
    }

    @Test
    fun testGeneKeepsTheNameOfTheColumn() {
        val gene = CassandraColumnGeneBuilder.buildGene(CassandraColumn("firstName", "text"))
        assertEquals("firstName", gene.name)
    }

    /**
     * A counter is only writable with an UPDATE, and a timeuuid needs a value that a plain uuid
     * gene would not produce, so neither can be given an arbitrary value in an insertion.
     */
    @Test
    fun testUnsupportedTypes() {
        listOf("counter", "timeuuid", "blob", "inet", "duration", "list<int>", "frozen<myType>").forEach {
            assertFalse(CassandraColumnGeneBuilder.isSupported(CassandraColumn("aColumn", it)), "$it should not be supported")
            assertThrows<IllegalArgumentException>("no exception for $it") { buildFor(it) }
        }
    }

    @Test
    fun testSupportedTypesAreReportedAsSuch() {
        listOf("text", "int", "uuid", "timestamp", "boolean").forEach {
            assertTrue(CassandraColumnGeneBuilder.isSupported(CassandraColumn("aColumn", it)), "$it should be supported")
        }
    }
}
