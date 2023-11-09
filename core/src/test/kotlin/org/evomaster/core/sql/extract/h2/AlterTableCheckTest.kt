package org.evomaster.core.sql.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class AlterTableCheckTest : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/alter_table_check.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.toLowerCase())
        assertEquals(DatabaseType.H2, schema.databaseType)
        assertTrue(schema.tables.any { it.name == "PEOPLE" })

        assertEquals(2, schema.tables.first { it.name == "PEOPLE" }.columns.size)

        assertTrue(schema.tables.first { it.name == "PEOPLE" }.columns.any { it.name == "AGE" });

        assertEquals(1, schema.tables.first { it.name == "PEOPLE" }.tableCheckExpressions.size)
        assertEquals("(\"AGE\" <= 100)", schema.tables.first { it.name == "PEOPLE" }.tableCheckExpressions[0].sqlCheckExpression)

    }
}