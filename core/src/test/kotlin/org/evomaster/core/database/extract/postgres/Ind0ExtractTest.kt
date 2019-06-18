package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.regex.RegexGene
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Assertions.*
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


        //TODO check the 3 views
    }

    @Test
    fun testDateGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("expr_date"))
        val genes = actions[0].seeGenes()

        assertTrue(genes.any { it is DateGene })

    }

    @Test
    fun testUUIDGeneCreation() {
        val schema = SchemaExtractor.extract(connection)
        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("id"))
        val genes = actions[0].seeGenes()
        assertTrue(genes.firstIsInstance<SqlPrimaryKeyGene>().gene is SqlUUIDGene)
    }

    @Test
    fun testXMLGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmlData"))
        val genes = actions[0].seeGenes()

        assertTrue(genes.any { it is SqlXMLGene })
    }


    @Test
    fun testJSONGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("y", setOf("jsonData"))
        val genes = actions[0].seeGenes()

        assertTrue(genes.any { it is SqlJSONGene })
    }


    @Test
    fun testSimilarToGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("w_id"))
        val genes = actions[0].seeGenes()

        assertTrue(genes.any { it is RegexGene })
        assertTrue(genes.filterIsInstance<RegexGene>().any { g -> g.name.equals("w_id", ignoreCase = true) })
    }

    @Test
    fun testLikeGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("f_id"))
        val genes = actions[0].seeGenes()

        assertTrue(genes.any { it is RegexGene })
        val regexGene = genes.firstIsInstance<RegexGene>()
        assertTrue(genes.filterIsInstance<RegexGene>().any { g -> g.name.equals("f_id", ignoreCase = true) })
    }

}