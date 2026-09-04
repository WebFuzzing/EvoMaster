package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CassandraDbActionTransformerTest {

    private fun anAction(): CassandraDbAction {
        return CassandraDbAction(
            "ks", "users",
            listOf(CassandraColumn("name", "text"), CassandraColumn("age", "int")),
            listOf(StringGene("name", "Alice"), IntegerGene("age", 42))
        )
    }

    @Test
    fun testNoAction() {
        assertTrue(CassandraDbActionTransformer.transform(listOf()).insertions.isEmpty())
    }

    @Test
    fun testKeyspaceAndTableAreReported() {
        val dto = CassandraDbActionTransformer.transform(listOf(anAction()))

        assertEquals(1, dto.insertions.size)
        assertEquals("ks", dto.insertions[0].keyspaceName)
        assertEquals("users", dto.insertions[0].tableName)
    }

    @Test
    fun testOneEntryPerColumnWithItsCqlLiteral() {
        val dto = CassandraDbActionTransformer.transform(listOf(anAction()))

        val data = dto.insertions[0].data
        assertEquals(2, data.size)

        assertEquals("name", data[0].columnName)
        assertEquals("'Alice'", data[0].printableValue)

        assertEquals("age", data[1].columnName)
        assertEquals("42", data[1].printableValue)
    }

    @Test
    fun testSeveralActionsKeepTheirOrder() {
        val other = CassandraDbAction(
            "ks", "events",
            listOf(CassandraColumn("note", "text")),
            listOf(StringGene("note", "hello"))
        )

        val dto = CassandraDbActionTransformer.transform(listOf(anAction(), other))

        assertEquals(2, dto.insertions.size)
        assertEquals("users", dto.insertions[0].tableName)
        assertEquals("events", dto.insertions[1].tableName)
    }

    @Test
    fun testActionWithNoColumn() {
        val action = CassandraDbAction("ks", "empty", listOf(), listOf())

        val dto = CassandraDbActionTransformer.transform(listOf(action))

        assertEquals(1, dto.insertions.size)
        assertTrue(dto.insertions[0].data.isEmpty())
    }
}
