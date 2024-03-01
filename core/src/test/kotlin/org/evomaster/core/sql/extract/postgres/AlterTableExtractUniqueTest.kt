package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class AlterTableExtractUniqueTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/passports.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.POSTGRES, schema.databaseType) },
                Executable { assertEquals(2, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "countries" }, "missing table COUNTRIES") },
                Executable { assertTrue(schema.tables.any { it.name == "passports" }, "missing table PASSPORTS") }
        )


        assertEquals(true, schema.tables.filter { it.name == "passports" }.first().columns.filter { it.name == "country_id" }.first().unique)
        assertEquals(true, schema.tables.filter { it.name == "passports" }.first().columns.filter { it.name == "passport_number" }.first().unique)

        assertEquals(1, schema.tables.filter { it.name == "passports" }.first().tableCheckExpressions.size)
        assertEquals("(passport_number > 0)", schema.tables.filter { it.name == "passports" }.first().tableCheckExpressions[0].sqlCheckExpression)

    }


}