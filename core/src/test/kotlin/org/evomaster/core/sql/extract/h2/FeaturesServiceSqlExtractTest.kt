package org.evomaster.core.sql.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbInfoExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class FeaturesServiceSqlExtractTest : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/features_service.sql"



    @Test
    fun testCreateAndExtract() {

        val schema = DbInfoExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("db_test", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(6, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.id.name == "CONSTRAINT_EXCLUDES" }) },
                Executable { assertTrue(schema.tables.any { it.id.name == "CONSTRAINT_REQUIRES" }) },
                Executable { assertTrue(schema.tables.any { it.id.name == "FEATURE" }) },
                Executable { assertTrue(schema.tables.any { it.id.name == "PRODUCT" }) },
                Executable { assertTrue(schema.tables.any { it.id.name == "PRODUCT_CONFIGURATION" }) },
                Executable { assertTrue(schema.tables.any { it.id.name == "PRODUCT_CONFIGURATION_ACTIVED_FEATURES" }) }
        )

        assertEquals(listOf("IN_CONFIGURATIONS_ID", "ACTIVED_FEATURES_ID"), schema.tables.filter { it.id.name == "PRODUCT_CONFIGURATION_ACTIVED_FEATURES" }.first().primaryKeySequence)
    }


}