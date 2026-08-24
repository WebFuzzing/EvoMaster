package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CassandraInsertBuilderTest {

    private val builder = CassandraInsertBuilder()

    @Test
    fun testOneGenePerColumn() {
        val action = builder.createCassandraInsertionAction("ks", "users", "id uuid PARTITION KEY, name text")

        assertEquals(listOf("id", "name"), action.seeTopGenes().map { it.name })
        assertTrue(action.seeTopGenes()[0] is UUIDGene)
        assertTrue(action.seeTopGenes()[1] is StringGene)
    }

    @Test
    fun testKeyspaceAndTableAreKept() {
        val action = builder.createCassandraInsertionAction("ks", "users", "id uuid PARTITION KEY")

        assertEquals("ks", action.keyspace)
        assertEquals("users", action.table)
    }

    @Test
    fun testActionName() {
        val action = builder.createCassandraInsertionAction("ks", "users", "id uuid PARTITION KEY")

        assertEquals("CASSANDRA_Insert_ks_users", action.getName())
    }

    @Test
    fun testKeyRolesAreKept() {
        val action = builder.createCassandraInsertionAction(
            "ks", "events", "id uuid PARTITION KEY, created timestamp CLUSTERING, note text")

        assertTrue(action.columns[0].isPartitionKey)
        assertTrue(action.columns[1].isClusteringColumn)
        assertTrue(!action.columns[2].isPartitionKey && !action.columns[2].isClusteringColumn)
    }

    /**
     * No value can be generated for a column whose type is not handled, so it is just left out of
     * the insertion instead of preventing the other columns from being inserted.
     */
    @Test
    fun testColumnsWithUnsupportedTypeAreSkipped() {
        val action = builder.createCassandraInsertionAction(
            "ks", "users", "id uuid PARTITION KEY, picture blob, name text")

        assertEquals(listOf("id", "name"), action.seeTopGenes().map { it.name })
        assertEquals(listOf("id", "name"), action.columns.map { it.name })
    }

    @Test
    fun testTableWithNoSupportedColumn() {
        val action = builder.createCassandraInsertionAction("ks", "blobs", "content blob")

        assertTrue(action.seeTopGenes().isEmpty())
    }

    @Test
    fun testCopyKeepsTheColumns() {
        val action = builder.createCassandraInsertionAction("ks", "users", "id uuid PARTITION KEY, name text")
        val copy = action.copy() as CassandraDbAction

        assertEquals(action.keyspace, copy.keyspace)
        assertEquals(action.table, copy.table)
        assertEquals(action.columns, copy.columns)
        assertEquals(action.seeTopGenes().map { it.name }, copy.seeTopGenes().map { it.name })
    }
}
