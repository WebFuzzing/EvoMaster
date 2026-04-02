package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.DbInfoExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CompositeForeignKeyTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_composite_fk.sql"

    @Test
    fun testCompositeForeignKey() {

        val schema = DbInfoExtractor.extract(connection)

        assertNotNull(schema)
        assertEquals(2, schema.tables.size)

        val parent = schema.tables.find { it.id.name.lowercase() == "parent" }!!
        val child = schema.tables.find { it.id.name.lowercase() == "child" }!!

        assertEquals(0, parent.foreignKeys.size)
        assertEquals(1, child.foreignKeys.size)

        val foreignKey = child.foreignKeys[0]

        assertEquals(2, foreignKey.sourceColumns.size)
        assertTrue(foreignKey.sourceColumns.any { it.lowercase() == "pid1" })
        assertTrue(foreignKey.sourceColumns.any { it.lowercase() == "pid2" })
        assertTrue(foreignKey.targetTable.lowercase() == "parent")

        assertEquals(2, foreignKey.targetColumns.size)
        assertTrue(foreignKey.targetColumns.any { it.lowercase() == "id1" })
        assertTrue(foreignKey.targetColumns.any { it.lowercase() == "id2" })
    }
}
