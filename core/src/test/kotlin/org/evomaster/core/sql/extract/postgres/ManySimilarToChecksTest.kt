package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.sql.SQLException


/**
 * Created by jgaleotti on 24-May-23.
 */
class ManySimilarToChecksTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_many_similarto_checks_db.sql"

    @Test
    fun testManySimilarToPatternsSchemaExtraction() {
        val schema = SchemaExtractor.extract(connection)

        assertEquals(2, schema.tables.first { it.name.equals("email_table", ignoreCase = true) }.tableCheckExpressions.size)

        val builder = SqlInsertBuilder(schema)
        val emailTable = builder.getTable("email_table", useExtraConstraints = true)

        assertEquals(1, emailTable.columns.size)
        val emailColumn = emailTable.columns.first()

        assertNotNull(emailColumn.similarToPatterns)
        assertEquals(2, emailColumn.similarToPatterns!!.size)
    }

    @Test
    fun testCheckRegexGenesAreCreatedForMultiplePatterns() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("email_table", setOf("email_column"))
        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is RegexGene)

    }


    @Disabled("pending implementing support for conjunction of patterns")
    @Test
    fun testCheckInsertionOfValuesUsingMultipleSimilarToPatterns() {
        val randomness = Randomness()
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("email_table", setOf("email_column"))

        val genes = actions[0].seeTopGenes()
        val regexGene = genes.firstIsInstance<RegexGene>()
        regexGene.randomize(randomness, false)
        val expectedValue = regexGene.getValueAsRawString()

        val query = "Select * from email_table"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(actions)

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val textValue = row.getValueByName("email_column")

        assertEquals(expectedValue, textValue)
    }


    @Test
    fun testInsertingAnInvalidValue() {
        val query = "INSERT INTO email_table (email_column)  VALUES ('mymail@mydomain.com');"
        try {
            SqlScriptRunner.execCommand(connection, query)
            fail<Any>()
        } catch (ex: SQLException) {
            // pass
        }
    }

    @Test
    fun testInsertingValidValue() {
        val query = "INSERT INTO email_table (email_column)  VALUES ('mymail@yahoo.com');"
        SqlScriptRunner.execCommand(connection, query)
    }


}