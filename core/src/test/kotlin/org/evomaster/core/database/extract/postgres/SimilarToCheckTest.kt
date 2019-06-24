package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.sql.SQLException


/**
 * Created by jgaleotti on 29-May-19.
 */
class SimilarToCheckTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/similarto_check_db.sql"

    @Test
    fun testSimilarToRegexGeneExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("w_id"))
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is RegexGene)

    }

    @Test
    fun testInsertRegexGene() {
        val randomness = Randomness()
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("w_id"))

        val genes = actions[0].seeGenes()
        val regexGene = genes.firstIsInstance<RegexGene>()
        regexGene.randomize(randomness, false, listOf())
        val expectedValue = regexGene.getValueAsRawString()

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(actions)

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val textValue = row.getValueByName("w_id")

        assertEquals(expectedValue, textValue)
    }


    @Test
    fun testInsertInvalidValue() {
        val query = "INSERT INTO x (w_id )  VALUES ('/foo/aa/bar/left/--');"
        try {
            SqlScriptRunner.execCommand(connection, query)
            fail<Object>()
        } catch (ex: SQLException) {

        }
    }

    @Test
    fun testInsertValidValue() {
        val query = "INSERT INTO x (w_id )  VALUES ('/foo/aa/bar/left/0000-00-00');"
        SqlScriptRunner.execCommand(connection, query)
    }


}