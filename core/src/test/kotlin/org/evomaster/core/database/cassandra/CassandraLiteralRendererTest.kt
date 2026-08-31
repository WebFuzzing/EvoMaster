package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.cassandra.CqlCollectionGene
import org.evomaster.core.search.gene.cassandra.CqlCollectionKind
import org.evomaster.core.search.gene.cassandra.CqlDurationGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.network.InetGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CassandraLiteralRendererTest {

    @Test
    fun testTextIsQuoted() {
        assertEquals("'Alice'", CassandraLiteralRenderer.toCqlLiteral(StringGene("name", "Alice")))
    }

    @Test
    fun testEmptyTextIsQuoted() {
        assertEquals("''", CassandraLiteralRenderer.toCqlLiteral(StringGene("name", "")))
    }

    /**
     * In CQL, a single quote inside a text literal is escaped by doubling it.
     */
    @Test
    fun testSingleQuoteInsideTextIsDoubled() {
        assertEquals("'l''Alice'", CassandraLiteralRenderer.toCqlLiteral(StringGene("name", "l'Alice")))
    }

    @Test
    fun testSeveralSingleQuotesInsideTextAreDoubled() {
        assertEquals("'''a'''", CassandraLiteralRenderer.toCqlLiteral(StringGene("name", "'a'")))
    }

    @Test
    fun testNumbersAreNotQuoted() {
        assertEquals("42", CassandraLiteralRenderer.toCqlLiteral(IntegerGene("age", 42)))
        assertEquals("-7", CassandraLiteralRenderer.toCqlLiteral(IntegerGene("delta", -7)))
        assertEquals("123", CassandraLiteralRenderer.toCqlLiteral(LongGene("amount", 123L)))
    }

    @Test
    fun testDoubleIsNotQuoted() {
        val gene = DoubleGene("ratio", 1.5)
        assertEquals(gene.getValueAsRawString(), CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    @Test
    fun testBooleanIsNotQuoted() {
        assertEquals("true", CassandraLiteralRenderer.toCqlLiteral(BooleanGene("flag", true)))
        assertEquals("false", CassandraLiteralRenderer.toCqlLiteral(BooleanGene("flag", false)))
    }

    /**
     * A uuid literal is written without quotes in CQL.
     */
    @Test
    fun testUuidIsNotQuoted() {
        val gene = UUIDGene("id")
        assertEquals(gene.getValueAsRawString(), CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    @Test
    fun testTemporalValuesAreQuoted() {
        listOf(DateTimeGene("created"), DateGene("day"), TimeGene("moment")).forEach {
            assertEquals("'${it.getValueAsRawString()}'", CassandraLiteralRenderer.toCqlLiteral(it))
        }
    }

    /**
     * A duration literal is written without quotes in CQL, sign included.
     */
    @Test
    fun testDurationIsNotQuoted() {
        val gene = CqlDurationGene(
            "elapsed",
            months = IntegerGene("months", 1),
            days = IntegerGene("days", 2),
            nanos = LongGene("nanos", 3L)
        )

        assertEquals("1mo2d3ns", CassandraLiteralRenderer.toCqlLiteral(gene))

        gene.negative.value = true
        assertEquals("-1mo2d3ns", CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    /**
     * An IP address is written as a quoted literal in CQL, ie an unquoted one is a syntax error.
     */
    @Test
    fun testInetIsQuoted() {
        val gene = InetGene("ip")

        assertEquals("'${gene.getValueAsRawString()}'", CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    private fun arrayGeneOf(vararg values: Int): ArrayGene<IntegerGene> {

        val gene = ArrayGene("elements", template = IntegerGene("element"))
        values.forEach { gene.addElement(IntegerGene("element", it)) }

        return gene
    }

    @Test
    fun testListIsWrittenBetweenSquareBrackets() {
        val gene = CqlCollectionGene("scores", CqlCollectionKind.LIST, arrayGeneOf(1, 2))

        assertEquals("[1, 2]", CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    @Test
    fun testSetIsWrittenBetweenBraces() {
        val gene = CqlCollectionGene("tags", CqlCollectionKind.SET, arrayGeneOf(1, 2))

        assertEquals("{1, 2}", CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    @Test
    fun testMapIsWrittenAsKeysAndValues() {
        val content = FixedMapGene("entries", key = StringGene("element"), value = IntegerGene("element"))
        content.addElement(PairGene("entry", StringGene("element", "a"), IntegerGene("element", 1)))

        val gene = CqlCollectionGene("favs", CqlCollectionKind.MAP, content)

        assertEquals("{'a': 1}", CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    /**
     * The elements have to be written the way a CQL literal of their own type is, which is what
     * would be lost by asking the collection gene to print itself instead of recursing.
     */
    @Test
    fun testTextElementsAreQuotedAndEscaped() {
        val content = ArrayGene("elements", template = StringGene("element"))
        content.addElement(StringGene("element", "a"))
        content.addElement(StringGene("element", "l'Alice"))

        val gene = CqlCollectionGene("tags", CqlCollectionKind.SET, content)

        assertEquals("{'a', 'l''Alice'}", CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    @Test
    fun testEmptyCollectionsAreWrittenWithTheirDelimitersOnly() {
        assertEquals("[]", CassandraLiteralRenderer.toCqlLiteral(
            CqlCollectionGene("scores", CqlCollectionKind.LIST, arrayGeneOf())))

        assertEquals("{}", CassandraLiteralRenderer.toCqlLiteral(
            CqlCollectionGene("tags", CqlCollectionKind.SET, arrayGeneOf())))

        assertEquals("{}", CassandraLiteralRenderer.toCqlLiteral(CqlCollectionGene(
            "favs",
            CqlCollectionKind.MAP,
            FixedMapGene("entries", key = StringGene("element"), value = IntegerGene("element"))
        )))
    }

    @Test
    fun testNestedCollectionIsRenderedByRecursing() {
        val content = FixedMapGene(
            "entries",
            key = StringGene("element"),
            value = CqlCollectionGene("element", CqlCollectionKind.LIST, arrayGeneOf())
        )
        content.addElement(PairGene(
            "entry",
            StringGene("element", "a"),
            CqlCollectionGene("element", CqlCollectionKind.LIST, arrayGeneOf(1, 2))
        ))

        val gene = CqlCollectionGene("data", CqlCollectionKind.MAP, content)

        assertEquals("{'a': [1, 2]}", CassandraLiteralRenderer.toCqlLiteral(gene))
    }

    @Test
    fun testGeneWithNoCqlRepresentationIsRejected() {
        assertThrows<IllegalArgumentException> {
            CassandraLiteralRenderer.toCqlLiteral(ObjectGene("obj", listOf()))
        }
    }
}
