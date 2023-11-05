package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class CreateTableExtractCheckTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/create_table_check.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.toLowerCase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)
        assertTrue(schema.tables.any { it.name == "people" })

        assertEquals(2, schema.tables.first { it.name == "people" }.columns.size)

        assertTrue(schema.tables.first { it.name == "people" }.columns.any { it.name == "age" });

        assertEquals(1, schema.tables.first { it.name == "people" }.tableCheckExpressions.size)
        assertEquals("(age <= 100)", schema.tables.first { it.name == "people" }.tableCheckExpressions[0].sqlCheckExpression)

    }
}