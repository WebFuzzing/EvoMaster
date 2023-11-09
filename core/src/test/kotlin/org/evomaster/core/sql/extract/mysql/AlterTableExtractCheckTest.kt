package org.evomaster.core.sql.extract.mysql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AlterTableExtractCheckTest : ExtractTestBaseMySQL() {

    override fun getSchemaLocation() = "/sql_schema/alter_table_check.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("test", schema.name.toLowerCase())
        assertEquals(DatabaseType.MYSQL, schema.databaseType)
        assertTrue(schema.tables.any { it.name.equals("people", ignoreCase = true) })

        assertEquals(2, schema.tables.first { it.name.equals("people", ignoreCase = true) }.columns.size)

        assertTrue(schema.tables.first {
            it.name.equals(
                "people",
                ignoreCase = true
            )
        }.columns.any { it.name == "age" });

        assertEquals(
            1,
            schema.tables.first { it.name.equals("people", ignoreCase = true) }.tableCheckExpressions.size
        )
        assertEquals(
            "(age <= 100)",
            schema.tables.first {
                it.name.equals(
                    "people",
                    ignoreCase = true
                )
            }.tableCheckExpressions[0].sqlCheckExpression
        )

    }
}