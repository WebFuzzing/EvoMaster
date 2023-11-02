package org.evomaster.core.sql.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class ScoutApiSqlExtractTest : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/scout-api.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.lowercase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(14, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "ACTIVITY" }, "missing table ACTIVITY") },
                Executable { assertTrue(schema.tables.any { it.name == "ACTIVITY_DERIVED" }, "missing table ACTIVITY_DERIVED") },
                Executable { assertTrue(schema.tables.any { it.name == "ACTIVITY_PROPERTIES" }, "missing table ACTIVITY_PROPERTIES") },
                Executable { assertTrue(schema.tables.any { it.name == "ACTIVITY_PROPERTIES_MEDIA_FILE" }, "missing table ACTIVITY_PROPERTIES_MEDIA_FILE") },
                Executable { assertTrue(schema.tables.any { it.name == "ACTIVITY_PROPERTIES_TAG" }, "missing table ACTIVITY_PROPERTIES_TAG") },
                Executable { assertTrue(schema.tables.any { it.name == "ACTIVITY_RATING" }, "missing table ACTIVITY_RATING") },
                Executable { assertTrue(schema.tables.any { it.name == "ACTIVITY_RELATION" }, "missing table ACTIVITY_RELATION") },
                Executable { assertTrue(schema.tables.any { it.name == "MEDIA_FILE" }, "missing table MEDIA_FILE") },
                Executable { assertTrue(schema.tables.any { it.name == "MEDIA_FILE_KEYWORDS" }, "missing table MEDIA_FILE_KEYWORDS") },
                Executable { assertTrue(schema.tables.any { it.name == "SYSTEM_MESSAGE" }, "missing table SYSTEM_MESSAGE") },
                Executable { assertTrue(schema.tables.any { it.name == "TAG" }, "missing table TAG") },
                Executable { assertTrue(schema.tables.any { it.name == "TAG_DERIVED" }, "missing table TAG_DERIVED") },
                Executable { assertTrue(schema.tables.any { it.name == "USER_IDENTITY" }, "missing table USER_IDENTITY") },
                Executable { assertTrue(schema.tables.any { it.name == "USERS" }, "missing table USERS") }
        )


        assertTrue(schema.tables.filter { it.name == "ACTIVITY" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertTrue(schema.tables.filter { it.name == "ACTIVITY_PROPERTIES" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertTrue(schema.tables.filter { it.name == "USERS" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertTrue(schema.tables.filter { it.name == "SYSTEM_MESSAGE" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertTrue(schema.tables.filter { it.name == "TAG" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertTrue(schema.tables.filter { it.name == "USER_IDENTITY" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)
        assertTrue(schema.tables.filter { it.name == "MEDIA_FILE" }.first().columns.filter { it.name == "ID" }.first().autoIncrement)


        assertEquals(5, schema.tables.filter { it.name == "USERS" }.first().columns.size)

        assertEquals(true, schema.tables.filter { it.name == "ACTIVITY_PROPERTIES" }.first().columns.filter { it.name == "publishing_activity_id".uppercase() }.first().unique)
        assertEquals(true, schema.tables.filter { it.name == "MEDIA_FILE" }.first().columns.filter { it.name == "uri".uppercase() }.first().unique)
        assertEquals(true, schema.tables.filter { it.name == "SYSTEM_MESSAGE" }.first().columns.filter { it.name == "keyColumn".uppercase() }.first().unique)

        assertEquals(2, schema.tables.filter { it.name == "ACTIVITY_PROPERTIES" }.first().tableCheckExpressions.size)
        assertEquals("(\"AGE_MAX\" <= 100)", schema.tables.filter { it.name == "ACTIVITY_PROPERTIES" }.first().tableCheckExpressions[0].sqlCheckExpression)
        assertEquals("(\"AGE_MIN\" <= 100)", schema.tables.filter { it.name == "ACTIVITY_PROPERTIES" }.first().tableCheckExpressions[1].sqlCheckExpression)


    }


}