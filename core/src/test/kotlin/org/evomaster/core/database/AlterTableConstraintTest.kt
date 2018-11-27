package org.evomaster.core.database

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class AlterTableConstraintTest : ExtractTestBase() {

    override fun getSchemaLocation()= "/sql_schema/passports.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(2, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "COUNTRIES" }, "missing table COUNTRIES") },
                Executable { assertTrue(schema.tables.any { it.name == "PASSPORTS" }, "missing table PASSPORTS") }
        )


        assertEquals(true, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "COUNTRY_ID"}.first().unique)
        assertEquals(true, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "PASSPORT_NUMBER"}.first().unique)

        assertEquals(1, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "PASSPORT_NUMBER"}.first().lowerBound)
        assertNull(schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "PASSPORT_NUMBER"}.first().upperBound)

    }


}