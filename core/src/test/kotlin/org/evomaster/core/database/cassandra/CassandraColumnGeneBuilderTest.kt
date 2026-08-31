package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.cassandra.CqlCollectionGene
import org.evomaster.core.search.gene.cassandra.CqlCollectionKind
import org.evomaster.core.search.gene.cassandra.CqlDurationGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.network.InetGene
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

    @Test
    fun testDurationType() {
        assertTrue(buildFor("duration") is CqlDurationGene)
    }

    @Test
    fun testInetType() {
        assertTrue(buildFor("inet") is InetGene)
    }

    @Test
    fun testListType() {
        val gene = buildFor("list<int>") as CqlCollectionGene

        assertEquals(CqlCollectionKind.LIST, gene.kind)
        val content = gene.content as ArrayGene<*>
        assertFalse(content.uniqueElements)
        assertTrue(content.template is IntegerGene)
    }

    /**
     * Cassandra collapses the repeated elements of a set, so generating them is wasted effort.
     * Note that the gene only asks for unique elements, without guaranteeing them: the check is
     * skipped altogether for the element types [ArrayGene] cannot compare, and nothing keeps an
     * element from being mutated into the value of another one afterwards.
     */
    @Test
    fun testSetTypeAsksForUniqueElements() {
        val gene = buildFor("set<text>") as CqlCollectionGene

        assertEquals(CqlCollectionKind.SET, gene.kind)
        val content = gene.content as ArrayGene<*>
        assertTrue(content.uniqueElements)
        assertTrue(content.template is StringGene)
    }

    @Test
    fun testMapType() {
        val gene = buildFor("map<text, int>") as CqlCollectionGene

        assertEquals(CqlCollectionKind.MAP, gene.kind)
        val content = gene.content as FixedMapGene<*, *>
        assertTrue(content.template.first is StringGene)
        assertTrue(content.template.second is IntegerGene)
    }

    @Test
    fun testNestedCollectionType() {
        val gene = buildFor("list<set<int>>") as CqlCollectionGene

        assertEquals(CqlCollectionKind.LIST, gene.kind)
        val element = (gene.content as ArrayGene<*>).template as CqlCollectionGene
        assertEquals(CqlCollectionKind.SET, element.kind)
        assertTrue((element.content as ArrayGene<*>).template is IntegerGene)
    }

    /**
     * Whether a collection is frozen does not change how a value of it is written in an insertion.
     */
    @Test
    fun testFrozenCollectionIsHandledAsAPlainOne() {
        val gene = buildFor("frozen<list<int>>") as CqlCollectionGene

        assertEquals(CqlCollectionKind.LIST, gene.kind)
        assertTrue((gene.content as ArrayGene<*>).template is IntegerGene)
    }

    /**
     * No value can be generated for a collection when none can be generated for what it holds.
     */
    @Test
    fun testCollectionOfAnUnsupportedTypeIsNotSupported() {
        listOf("list<blob>", "map<text, counter>", "set<frozen<myType>>", "list<set<blob>>").forEach {
            assertFalse(CassandraColumnGeneBuilder.isSupported(CassandraColumn("aColumn", it)), "$it should not be supported")
            assertThrows<IllegalArgumentException>("no exception for $it") { buildFor(it) }
        }
    }

    /**
     * A counter is only writable with an UPDATE, and a timeuuid needs a value that a plain uuid
     * gene would not produce, so neither can be given an arbitrary value in an insertion. For the
     * other types, it is just that no gene generating a value for them has been written yet.
     * The tuples and the vectors are written with type parameters without being collections, so
     * they are the ones the handling of the collection types has to avoid mistaking for one.
     */
    @Test
    fun testUnsupportedTypes() {
        listOf("counter", "timeuuid", "blob", "frozen<myType>", "tuple<int, text>", "vector<float, 3>").forEach {
            assertFalse(CassandraColumnGeneBuilder.isSupported(CassandraColumn("aColumn", it)), "$it should not be supported")
            assertThrows<IllegalArgumentException>("no exception for $it") { buildFor(it) }
        }
    }

    @Test
    fun testSupportedTypesAreReportedAsSuch() {
        listOf("text", "int", "uuid", "timestamp", "boolean", "inet", "list<int>", "map<text, int>").forEach {
            assertTrue(CassandraColumnGeneBuilder.isSupported(CassandraColumn("aColumn", it)), "$it should be supported")
        }
    }
}
