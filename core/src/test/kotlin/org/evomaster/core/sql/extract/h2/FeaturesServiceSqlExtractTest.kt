package org.evomaster.core.sql.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class FeaturesServiceSqlExtractTest : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/features_service.sql"



    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(6, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "CONSTRAINT_EXCLUDES" }) },
                Executable { assertTrue(schema.tables.any { it.name == "CONSTRAINT_REQUIRES" }) },
                Executable { assertTrue(schema.tables.any { it.name == "FEATURE" }) },
                Executable { assertTrue(schema.tables.any { it.name == "PRODUCT" }) },
                Executable { assertTrue(schema.tables.any { it.name == "PRODUCT_CONFIGURATION" }) },
                Executable { assertTrue(schema.tables.any { it.name == "PRODUCT_CONFIGURATION_ACTIVED_FEATURES" }) }
        )

        assertEquals(listOf("IN_CONFIGURATIONS_ID", "ACTIVED_FEATURES_ID"), schema.tables.filter { it.name == "PRODUCT_CONFIGURATION_ACTIVED_FEATURES" }.first().primaryKeySequence)
    }


}