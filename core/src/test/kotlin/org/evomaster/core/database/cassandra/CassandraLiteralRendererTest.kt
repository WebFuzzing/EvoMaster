package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.UUIDGene
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

    @Test
    fun testGeneWithNoCqlRepresentationIsRejected() {
        assertThrows<IllegalArgumentException> {
            CassandraLiteralRenderer.toCqlLiteral(ObjectGene("obj", listOf()))
        }
    }
}
