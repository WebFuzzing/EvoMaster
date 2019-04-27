package org.evomaster.core.database.extract

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 01-Apr-19.
 */
class ExtractH2CheckInConstraint : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/check_in.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.toLowerCase())
        assertEquals(DatabaseType.H2, schema.databaseType)
        assertTrue(schema.tables.any { it.name == "X" })

        assertEquals(10, schema.tables.first { it.name == "X" }.columns.size)

        assertTrue(schema.tables.first { it.name == "X" }.columns.any { it.name == "STATUS" });

        val enumValuesAsStrings = schema.tables.first { it.name == "X" }.columns.first { it.name == "STATUS" }.enumValuesAsStrings
        assertEquals(listOf("A", "B"), enumValuesAsStrings);
    }
}