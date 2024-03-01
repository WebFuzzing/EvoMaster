package org.evomaster.core.sql.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 01-Apr-19.
 */
class AlterTableCheckEnumTest : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/alter_table_check_enum.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.toLowerCase())
        assertEquals(DatabaseType.H2, schema.databaseType)
        assertTrue(schema.tables.any { it.name == "X" })

        assertEquals(10, schema.tables.first { it.name == "X" }.columns.size)

        assertTrue(schema.tables.first { it.name == "X" }.columns.any { it.name == "STATUS" });

        assertEquals(1, schema.tables.first { it.name == "X" }.tableCheckExpressions.size)
        assertEquals("(\"STATUS\" IN('A', 'B'))", schema.tables.first { it.name == "X" }.tableCheckExpressions[0].sqlCheckExpression)

    }
}