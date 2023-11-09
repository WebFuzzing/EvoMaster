package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.sql.textsearch.SqlTextSearchQueryGene
import org.evomaster.core.search.gene.sql.textsearch.SqlTextSearchVectorGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class TextSearchTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_search_text_types.sql"


    @Test
    fun testInsertionOfOneLexemeTextSearchTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "TextSearchTypes", setOf(
                "tsvectorColumn",
                "tsqueryColumn"
        )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(2, genes.size)

        assertTrue(genes[0] is SqlTextSearchVectorGene)
        assertTrue(genes[1] is SqlTextSearchQueryGene)

        val textSearchVectorGene = genes[0] as SqlTextSearchVectorGene
        val stringGene = textSearchVectorGene.getViewOfChildren()[0] as StringGene
        stringGene.value = "foo bar"

        val textSearchQueryGene = genes[1] as SqlTextSearchQueryGene
        val textSearchQueryElementGene0 = StringGene("queryLexeme")
        textSearchQueryElementGene0.value = "foo"
        textSearchQueryGene.queryLexemes.addElement(textSearchQueryElementGene0)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

    @Test
    fun testInsertEmptyTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "TextSearchTypes", setOf(
                "tsvectorColumn",
                "tsqueryColumn"
        )
        )

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

    @Test
    fun testInsertionOfManyLexemesTextSearchQueryType() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "TextSearchTypes", setOf(
                "tsvectorColumn",
                "tsqueryColumn"
        )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(2, genes.size)

        assertTrue(genes[0] is SqlTextSearchVectorGene)
        assertTrue(genes[1] is SqlTextSearchQueryGene)

        val textSearchVectorGene = genes[0] as SqlTextSearchVectorGene
        val stringGene = textSearchVectorGene.getViewOfChildren()[0] as StringGene
        stringGene.value = "foo bar"

        val textSearchQueryGene = genes[1] as SqlTextSearchQueryGene
        val gene0 = textSearchQueryGene.queryLexemes.template.copy() as StringGene
        val gene1 = textSearchQueryGene.queryLexemes.template.copy() as StringGene
        gene0.value = "foo"
        gene1.value ="bar"
        textSearchQueryGene.queryLexemes.addElement(gene0)
        textSearchQueryGene.queryLexemes.addElement(gene1)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }


    @Test
    fun testInsertionOfBlankTextSearchVector() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "TextSearchTypes", setOf(
                "tsvectorColumn",
                "tsqueryColumn"
        )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(2, genes.size)

        assertTrue(genes[0] is SqlTextSearchVectorGene)
        assertTrue(genes[1] is SqlTextSearchQueryGene)

        val textSearchVectorGene = genes[0] as SqlTextSearchVectorGene
        val stringGene = textSearchVectorGene.getViewOfChildren()[0] as StringGene
        stringGene.value = "   "

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

}