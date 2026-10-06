package org.evomaster.core.database.cassandra

import org.evomaster.client.java.controller.api.dto.database.cassandra.CassandraColumnDto
import org.evomaster.client.java.controller.api.dto.database.cassandra.CassandraTableSchemaDto
import org.evomaster.client.java.controller.api.dto.database.execution.CassandraExecutionsDto
import org.evomaster.client.java.controller.api.dto.database.execution.CassandraFailedQuery
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CassandraExecutionTest {

    private fun tableSchema(table: String) = CassandraTableSchemaDto(
        "ks",
        table,
        listOf(CassandraColumnDto("id", "int", true, false))
    )

    private fun failedQuery(table: String, schema: CassandraTableSchemaDto?) =
        CassandraFailedQuery("ks", table, schema)

    @Test
    fun testNoDtoGivesNoFailedQueries() {
        assertNull(CassandraExecution.fromDto(null).failedQueries)
    }

    @Test
    fun testFailedQueriesAreKeptWithTheirSchema() {
        val dto = CassandraExecutionsDto()
        dto.failedQueries = listOf(
            failedQuery("person", tableSchema("person")),
            failedQuery("event", tableSchema("event"))
        )

        val execution = CassandraExecution.fromDto(dto)

        assertEquals(2, execution.failedQueries!!.size)
        assertEquals(listOf("person", "event"), execution.failedQueries!!.map { it.tableName })
        assertEquals("id", execution.failedQueries!![0].tableSchema!!.columns[0].name)
    }

    /**
     * The schema is null for a table whose shape was never captured, and the execution must still
     * carry the query, as it is the aggregation that decides what to do with it.
     */
    @Test
    fun testFailedQueryWithNoSchemaIsKept() {
        val dto = CassandraExecutionsDto()
        dto.failedQueries = listOf(failedQuery("person", null))

        val execution = CassandraExecution.fromDto(dto)

        assertEquals(1, execution.failedQueries!!.size)
        assertNull(execution.failedQueries!![0].tableSchema)
    }
}