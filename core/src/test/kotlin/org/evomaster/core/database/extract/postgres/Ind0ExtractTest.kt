package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 01-Apr-19.
 */
class Ind0ExtractTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/ind_0.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.toLowerCase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)
        assertTrue(schema.tables.any { it.name == "x" })
        assertTrue(schema.tables.any { it.name == "y" })

        assertEquals(11, schema.tables.first { it.name == "x" }.columns.size)
        assertEquals(7, schema.tables.first { it.name == "y" }.columns.size)

        assertTrue(schema.tables.first { it.name == "x" }.columns.any { it.type.equals("xml", true) })
        assertTrue(schema.tables.first { it.name == "y" }.columns.any { it.type.equals("jsonb", true) })

        assertEquals(4, schema.tables.first { it.name == "x" }.tableCheckExpressions.size)
        assertEquals(1, schema.tables.first { it.name == "y" }.tableCheckExpressions.size)

        // constraints on X table
        assertTrue(schema.tables.first { it.name == "x" }.tableCheckExpressions.any { it.sqlCheckExpression.equals("(status = ANY (ARRAY['A'::text, 'B'::text]))") });
        assertTrue(schema.tables.first { it.name == "x" }.tableCheckExpressions.any { it.sqlCheckExpression.equals("((status = 'B'::text) = (p_at IS NOT NULL))") });
        assertTrue(schema.tables.first { it.name == "x" }.tableCheckExpressions.any { it.sqlCheckExpression.equals("((f_id ~~ 'hi'::text) OR (f_id ~~ '%foo%'::text) OR (f_id ~~ '%foo%x%'::text) OR (f_id ~~ '%bar%'::text) OR (f_id ~~ '%bar%y%'::text) OR (f_id ~~ '%hello%'::text))") });
        assertTrue(schema.tables.first { it.name == "x" }.tableCheckExpressions.any { it.sqlCheckExpression.equals("(w_id ~ similar_escape('/foo/__/bar/(left|right)/[0-9]{4}-[0-9]{2}-[0-9]{2}(/[0-9]*)?'::text, NULL::text))") });

        // constraints on Y table
        assertTrue(schema.tables.first { it.name == "y" }.tableCheckExpressions.any { it.sqlCheckExpression.equals("(status = ANY (ARRAY['A'::text, 'B'::text, 'C'::text, 'D'::text, 'E'::text]))") });


        //TODO check the 3 views and all constraints
    }

    @Disabled("Requires implementing genes for postgres column data types: TEXT, XML, UUID and JSONB")
    @Test
    fun testGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("p_at", "created_at"))
        val genes = actions[0].seeGenes()

        //TODO check creation of genes for TEXT, UUID, XML and JSONB column types
    }
}