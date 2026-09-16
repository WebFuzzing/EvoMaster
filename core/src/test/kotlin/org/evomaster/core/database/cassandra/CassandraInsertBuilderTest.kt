package org.evomaster.core.database.cassandra

import org.evomaster.client.java.controller.api.dto.database.cassandra.CassandraColumnDto
import org.evomaster.client.java.controller.api.dto.database.cassandra.CassandraTableSchemaDto
import org.evomaster.core.search.gene.UUIDGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CassandraInsertBuilderTest {

    private val builder = CassandraInsertBuilder()

    private fun schema(keyspace: String, table: String, vararg columns: CassandraColumnDto) =
        CassandraTableSchemaDto(keyspace, table, columns.toList())

    private fun column(name: String, cqlType: String) = CassandraColumnDto(name, cqlType, false, false)

    private fun partitionKey(name: String, cqlType: String) = CassandraColumnDto(name, cqlType, true, false)

    private fun clusteringColumn(name: String, cqlType: String) = CassandraColumnDto(name, cqlType, false, true)

    @Test
    fun testOneGenePerColumn() {
        val action = builder.createCassandraInsertionAction(
            schema("ks", "users", partitionKey("id", "uuid"), column("name", "text")))

        assertEquals(listOf("id", "name"), action.seeTopGenes().map { it.name })
        assertTrue(action.seeTopGenes()[0] is UUIDGene)
        assertTrue(action.seeTopGenes()[1] is StringGene)
    }

    @Test
    fun testKeyspaceAndTableAreKept() {
        val action = builder.createCassandraInsertionAction(
            schema("ks", "users", partitionKey("id", "uuid")))

        assertEquals("ks", action.keyspace)
        assertEquals("users", action.table)
    }

    @Test
    fun testActionName() {
        val action = builder.createCassandraInsertionAction(
            schema("ks", "users", partitionKey("id", "uuid")))

        assertEquals("CASSANDRA_Insert_ks_users", action.getName())
    }

    @Test
    fun testKeyRolesAreKept() {
        val action = builder.createCassandraInsertionAction(
            schema("ks", "events",
                partitionKey("id", "uuid"),
                clusteringColumn("created", "timestamp"),
                column("note", "text")))

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
            schema("ks", "users",
                partitionKey("id", "uuid"),
                column("picture", "blob"),
                column("name", "text")))

        assertEquals(listOf("id", "name"), action.seeTopGenes().map { it.name })
        assertEquals(listOf("id", "name"), action.columns.map { it.name })
    }

    /**
     * An insertion with no column at all could only be rejected, so none is built.
     */
    @Test
    fun testTableWithNoSupportedColumnIsRejected() {
        val schema = schema("ks", "blobs", column("content", "blob"))

        assertThrows<IllegalArgumentException> { builder.createCassandraInsertionAction(schema) }
        assertFalse(builder.canBuildInsertionFor(schema))
    }

    /**
     * Cassandra requires a full primary key in an INSERT, so an insertion leaving out one of the
     * columns composing it could only be rejected.
     */
    @Test
    fun testTableWithUnsupportedPartitionKeyIsRejected() {
        val schema = schema("ks", "users", partitionKey("id", "blob"), column("name", "text"))

        assertThrows<IllegalArgumentException> { builder.createCassandraInsertionAction(schema) }
        assertFalse(builder.canBuildInsertionFor(schema))
    }

    @Test
    fun testTableWithUnsupportedClusteringColumnIsRejected() {
        val schema = schema("ks", "events",
            partitionKey("id", "uuid"),
            clusteringColumn("at", "blob"),
            column("note", "text"))

        assertThrows<IllegalArgumentException> { builder.createCassandraInsertionAction(schema) }
        assertFalse(builder.canBuildInsertionFor(schema))
    }

    @Test
    fun testInsertionCanBeBuiltWhenOnlyRegularColumnsAreSkipped() {
        assertTrue(builder.canBuildInsertionFor(
            schema("ks", "users",
                partitionKey("id", "uuid"),
                column("picture", "blob"),
                column("name", "text"))))

        assertTrue(builder.canBuildInsertionFor(
            schema("ks", "users", partitionKey("id", "uuid"), column("name", "text"))))
    }

    /**
     * Cassandra only allows a frozen collection in a primary key, and a frozen type is reported as
     * a plain one, so a collection in that position is handled as any other supported column.
     */
    @Test
    fun testTableWithACollectionAsPartitionKeyIsAccepted() {
        val schema = schema("ks", "images", partitionKey("tags", "set<text>"), column("v", "int"))

        assertTrue(builder.canBuildInsertionFor(schema))

        val action = builder.createCassandraInsertionAction(schema)

        assertEquals(listOf("tags", "v"), action.seeTopGenes().map { it.name })
        assertTrue(action.seeTopGenes()[0] is ArrayGene<*>)
    }

    @Test
    fun testCopyKeepsTheColumns() {
        val action = builder.createCassandraInsertionAction(
            schema("ks", "users", partitionKey("id", "uuid"), column("name", "text")))
        val copy = action.copy() as CassandraDbAction

        assertEquals(action.keyspace, copy.keyspace)
        assertEquals(action.table, copy.table)
        assertEquals(action.columns, copy.columns)
        assertEquals(action.seeTopGenes().map { it.name }, copy.seeTopGenes().map { it.name })
    }
}