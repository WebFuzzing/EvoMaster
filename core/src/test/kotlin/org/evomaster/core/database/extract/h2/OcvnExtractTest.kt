package org.evomaster.core.database.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable


class OcvnExtractTest : ExtractTestBaseH2() {

    override fun getSchemaLocation() = "/sql_schema/ocvn.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(34, schema.tables.size) }
        )

        val tableNames = listOf(
                "admin_settings_aud",
                "admin_settings",
                "category",
                "category_aud",
                "file_metadata_aud",
                "file_content",
                "file_metadata",
                "person",
                "person_aud",
                "person_roles",
                "person_roles_aud",
                "revinfo",
                "role",
                "role_aud",
                "test_form_aud",
                "test_form_entity_multi_select_aud",
                "test_form_file_input_aud",
                "test_form",
                "test_form_entity_multi_select",
                "test_form_file_input",
                "user_dashboard_aud",
                "user_dashboard_users_aud",
                "user_dashboard",
                "user_dashboard_users",
                "vietnam_import_source_files_aud",
                "vietnam_import_source_files_city_department_group_file_aud",
                "vietnam_import_source_files_locations_file_aud",
                "vietnam_import_source_files_prototype_database_file_aud",
                "vietnam_import_source_files_public_institutions_suppliers_file_aud",
                "vietnam_import_source_files",
                "vietnam_import_source_files_city_department_group_file",
                "vietnam_import_source_files_locations_file",
                "vietnam_import_source_files_prototype_database_file",
                "vietnam_import_source_files_public_institutions_suppliers_file"
        )

        for (name in tableNames) {
            assertTrue(schema.tables.any { it.name.equals(name, true) }, "Missing table $name")
        }
    }


    @Test
    fun testBlobIssue(){

        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)

        val tableName = "FILE_CONTENT"
        assertTrue(schema.tables.any { it.name.equals(tableName, true) })

        val columnName = "BYTES"
        val actions = builder.createSqlInsertionAction(tableName, setOf(columnName))

        val all = actions.flatMap { it.seeGenes() }.flatMap { it.flatView() }
        val gene = all.find { it.name.equals(columnName, true) }

        assertTrue(gene is StringGene)
    }
}