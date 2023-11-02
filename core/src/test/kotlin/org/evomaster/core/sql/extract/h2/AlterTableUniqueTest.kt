package org.evomaster.core.sql.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class AlterTableUniqueTest : ExtractTestBaseH2() {

    override fun getSchemaLocation()= "/sql_schema/passports.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.lowercase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(2, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "COUNTRIES" }, "missing table COUNTRIES") },
                Executable { assertTrue(schema.tables.any { it.name == "PASSPORTS" }, "missing table PASSPORTS") }
        )


        assertEquals(true, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "COUNTRY_ID"}.first().unique)
        assertEquals(true, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "PASSPORT_NUMBER"}.first().unique)

        assertEquals(1, schema.tables.filter { it.name == "PASSPORTS" }.first().tableCheckExpressions.size)
        assertEquals("(\"PASSPORT_NUMBER\" > CAST(0 AS BIGINT))", schema.tables.filter { it.name == "PASSPORTS" }.first().tableCheckExpressions[0].sqlCheckExpression)

    }


}