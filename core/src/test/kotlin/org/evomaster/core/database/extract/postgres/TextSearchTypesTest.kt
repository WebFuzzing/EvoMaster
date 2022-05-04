package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.ArrayGene
import org.evomaster.core.search.gene.StringGene
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

        val genes = actions[0].seeGenes()

        assertEquals(2, genes.size)

        assertTrue(genes[0] is SqlTextSearchVectorGene)
        assertTrue(genes[1] is SqlTextSearchQueryGene)

        val textSearchVectorGene = genes[0] as SqlTextSearchVectorGene
        val textSearchVectorElementGene0 = StringGene("textLexeme")
        textSearchVectorElementGene0.value = "foo bar"
        val textLexemesArray = textSearchVectorGene.innerGene()[0] as ArrayGene<StringGene>
        textLexemesArray.addElement(textSearchVectorElementGene0)

        val textSearchQueryGene = genes[1] as SqlTextSearchQueryGene
        val textSearchQueryElementGene0 = StringGene("queryLexeme")
        textSearchQueryElementGene0.value = "foo"
        textSearchQueryGene.queryLexemes.addElement(textSearchQueryElementGene0)

        val dbCommandDto = DbActionTransformer.transform(actions)
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

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }
}