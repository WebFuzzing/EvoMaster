package org.evomaster.core.database.extract

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 01-Apr-19.
 */
class Ind0ExtractTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/ind_0.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.toLowerCase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)
        assertTrue(schema.tables.any { it.name == "x" })
        assertTrue(schema.tables.any { it.name == "y" })

        assertEquals(11, schema.tables.first{ it.name == "x" }.columns.size)
        assertEquals(7, schema.tables.first{ it.name == "y" }.columns.size)

        assertTrue(schema.tables.first{it.name == "x"}.columns.any { it.type.equals("xml", true) })
        assertTrue(schema.tables.first{it.name == "y"}.columns.any { it.type.equals("jsonb", true) })

        //TODO check the 3 views and all constraints
    }
}