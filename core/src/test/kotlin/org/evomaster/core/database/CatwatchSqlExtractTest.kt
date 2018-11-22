package org.evomaster.core.database

import org.evomaster.clientJava.controller.internal.db.SchemaExtractor
import org.evomaster.clientJava.controllerApi.dto.database.schema.DatabaseType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class CatwatchSqlExtractTest : ExtractTestBase(){

    override fun getSchemaLocation() = "/sql_schema/catwatch.sql"


    @Test
    fun testCreateAndExtract() {
        
        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(5, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "CONTRIBUTOR" }) },
                Executable { assertTrue(schema.tables.any { it.name == "LANGUAGE_LIST" }) },
                Executable { assertTrue(schema.tables.any { it.name == "MAINTAINERS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "PROJECT" }) },
                Executable { assertTrue(schema.tables.any { it.name == "STATISTICS" }) }
        )

        assertEquals(listOf("ID", "ORGANIZATION_ID", "SNAPSHOT_DATE"), schema.tables.filter { it.name == "CONTRIBUTOR" }.first().primaryKeySequence)
        assertEquals(listOf("ID", "SNAPSHOT_DATE"), schema.tables.filter { it.name == "STATISTICS" }.first().primaryKeySequence)
        assertEquals(listOf("ID"), schema.tables.filter { it.name == "PROJECT" }.first().primaryKeySequence)
        assertEquals(listOf<String>(), schema.tables.filter { it.name == "MAINTAINERS" }.first().primaryKeySequence)

    }


}