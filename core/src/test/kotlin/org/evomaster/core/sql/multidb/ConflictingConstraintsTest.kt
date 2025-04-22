package org.evomaster.core.sql.multidb

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbInfoExtractor
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection

class ConflictingConstraintsTest : MultiDbTestBase() {

    override fun verify(databaseType: DatabaseType, connection: Connection, name: String) {

        val info = DbInfoExtractor.extract(connection)
        assertEquals(name.lowercase(), info.name.lowercase())
        assertEquals(2, info.tables.size)
        val first = info.tables.find { it.schema.equals("first",true) }!!
        val second = info.tables.find { it.schema.equals("other",true) }!!

        assertEquals(first.name, second.name)

        assertEquals(2, first.columns.size)
        assertTrue(first.columns.any { it.name.equals("x", true) })
        assertTrue(first.columns.any { it.name.equals("id",true) })

        assertEquals(2, second.columns.size)
        assertTrue(second.columns.any { it.name.equals("x", true) })
        assertTrue(second.columns.any { it.name.equals("id",true) })

        //different primary keys
        assertEquals("id", first.primaryKeySequence[0].lowercase())
        assertEquals("x", second.primaryKeySequence[0].lowercase())

        //inverted nullability
        assertFalse(first.columns.find { it.name.equals("id", true) }!!.nullable)
        assertTrue(first.columns.find { it.name.equals("x", true) }!!.nullable)
        assertTrue(second.columns.find { it.name.equals("id", true) }!!.nullable)
        assertFalse(second.columns.find { it.name.equals("x", true) }!!.nullable)
    }

    override fun getSchemaLocation() = "/sql_schema/multidb/conflictingconstraints.sql"
}