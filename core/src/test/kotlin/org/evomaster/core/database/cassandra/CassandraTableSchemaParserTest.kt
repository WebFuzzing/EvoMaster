package org.evomaster.core.database.cassandra

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CassandraTableSchemaParserTest {

    @Test
    fun testEmptySchema() {
        assertTrue(CassandraTableSchemaParser.parse("").isEmpty())
    }

    @Test
    fun testSingleRegularColumn() {
        val columns = CassandraTableSchemaParser.parse("name text")

        assertEquals(1, columns.size)
        assertEquals(CassandraColumn("name", "text"), columns[0])
    }

    @Test
    fun testPartitionKeyColumn() {
        val columns = CassandraTableSchemaParser.parse("id uuid PARTITION KEY")

        assertEquals(listOf(CassandraColumn("id", "uuid", isPartitionKey = true)), columns)
    }

    @Test
    fun testClusteringColumn() {
        val columns = CassandraTableSchemaParser.parse("created timestamp CLUSTERING")

        assertEquals(listOf(CassandraColumn("created", "timestamp", isClusteringColumn = true)), columns)
    }

    @Test
    fun testColumnMarkedBothAsPartitionKeyAndClustering() {
        val columns = CassandraTableSchemaParser.parse("id uuid PARTITION KEY CLUSTERING")

        assertEquals(
            listOf(CassandraColumn("id", "uuid", isPartitionKey = true, isClusteringColumn = true)),
            columns
        )
    }

    @Test
    fun testSeveralColumnsKeepTheirOrder() {
        val columns = CassandraTableSchemaParser.parse("id uuid PARTITION KEY, name text, created timestamp CLUSTERING")

        assertEquals(
            listOf(
                CassandraColumn("id", "uuid", isPartitionKey = true),
                CassandraColumn("name", "text"),
                CassandraColumn("created", "timestamp", isClusteringColumn = true)
            ),
            columns
        )
    }

    /**
     * The type of a collection is itself rendered with the same separator used between columns.
     */
    @Test
    fun testCollectionTypeIsNotSplit() {
        val columns = CassandraTableSchemaParser.parse("id uuid PARTITION KEY, data map<text, int>")

        assertEquals(2, columns.size)
        assertEquals(CassandraColumn("data", "map<text, int>"), columns[1])
    }

    @Test
    fun testNestedCollectionTypeIsNotSplit() {
        val columns = CassandraTableSchemaParser.parse("data map<text, frozen<list<int>>>, name text")

        assertEquals(2, columns.size)
        assertEquals(CassandraColumn("data", "map<text, frozen<list<int>>>"), columns[0])
        assertEquals(CassandraColumn("name", "text"), columns[1])
    }

    @Test
    fun testColumnWithNoTypeIsRejected() {
        assertThrows<IllegalArgumentException> { CassandraTableSchemaParser.parse("name") }
    }

    /**
     * With unbalanced type parameters, there is no telling which of the separators are the ones
     * between columns, so the description is rejected instead of being split at the wrong places.
     */
    @Test
    fun testUnclosedTypeParametersAreRejected() {
        assertThrows<IllegalArgumentException> { CassandraTableSchemaParser.parse("tags map<text, int") }
    }

    @Test
    fun testUnopenedTypeParametersAreRejected() {
        assertThrows<IllegalArgumentException> { CassandraTableSchemaParser.parse("a text>, b int") }
    }
}
