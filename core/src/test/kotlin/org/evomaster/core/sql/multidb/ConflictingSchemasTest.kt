package org.evomaster.core.sql.multidb

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbInfoExtractor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.sql.Connection

class ConflictingSchemasTest : MultiDbTestBase() {

    override fun verify(databaseType: DatabaseType, connection: Connection, name: String) {

        val info = DbInfoExtractor.extract(connection)
        assertEquals(name.lowercase(), info.name.lowercase())
        assertEquals(2, info.tables.size)
        val first = info.tables.find { it.schema.equals("first",true) }!!
        val second = info.tables.find { it.schema.equals("other",true) }!!

        assertEquals(first.name, second.name)

        assertEquals(2, first.columns.size)
        assertEquals(2, second.columns.size)

        assertTrue(first.columns.any { it.name.equals("x", true) })
        assertTrue(second.columns.any { it.name.equals("y",true) })
    }

    override fun getSchemaLocation()= "/sql_schema/multidb/conflictingschemas.sql"
}