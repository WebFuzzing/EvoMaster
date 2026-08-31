package org.evomaster.core.database.cassandra

import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.cassandra.CqlCollectionGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    /**
     * An insertion with no column at all could only be rejected, so none is built.
     */
    @Test
    fun testTableWithNoSupportedColumnIsRejected() {
        assertThrows<IllegalArgumentException> {
            builder.createCassandraInsertionAction("ks", "blobs", "content blob")
        }
        assertFalse(builder.canBuildInsertionFor("content blob"))
    }

    /**
     * Cassandra requires a full primary key in an INSERT, so an insertion leaving out one of the
     * columns composing it could only be rejected.
     */
    @Test
    fun testTableWithUnsupportedPartitionKeyIsRejected() {
        assertThrows<IllegalArgumentException> {
            builder.createCassandraInsertionAction("ks", "users", "id blob PARTITION KEY, name text")
        }
        assertFalse(builder.canBuildInsertionFor("id blob PARTITION KEY, name text"))
    }

    @Test
    fun testTableWithUnsupportedClusteringColumnIsRejected() {
        val schema = "id uuid PARTITION KEY, at blob CLUSTERING, note text"

        assertThrows<IllegalArgumentException> {
            builder.createCassandraInsertionAction("ks", "events", schema)
        }
        assertFalse(builder.canBuildInsertionFor(schema))
    }

    @Test
    fun testInsertionCanBeBuiltWhenOnlyRegularColumnsAreSkipped() {
        assertTrue(builder.canBuildInsertionFor("id uuid PARTITION KEY, picture blob, name text"))
        assertTrue(builder.canBuildInsertionFor("id uuid PARTITION KEY, name text"))
    }

    /**
     * Cassandra only allows a frozen collection in a primary key, and a frozen type is reported as
     * a plain one, so a collection in that position is handled as any other supported column.
     */
    @Test
    fun testTableWithACollectionAsPartitionKeyIsAccepted() {
        val schema = "tags set<text> PARTITION KEY, v int"

        assertTrue(builder.canBuildInsertionFor(schema))

        val action = builder.createCassandraInsertionAction("ks", "images", schema)

        assertEquals(listOf("tags", "v"), action.seeTopGenes().map { it.name })
        assertTrue(action.seeTopGenes()[0] is CqlCollectionGene)
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
