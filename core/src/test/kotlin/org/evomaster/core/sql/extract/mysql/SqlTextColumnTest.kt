package org.evomaster.core.sql.extract.mysql

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlTextColumnTest : ExtractTestBaseMySQL() {

    override fun getSchemaLocation() = "/sql_schema/text_column_db.sql"


    @Test
    fun testExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("people", setOf("id", "name", "address"))
        val genes = actions[0].seeTopGenes()

        assertEquals(3, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue(genes[1] is StringGene)
        assertTrue(genes[2] is NullableGene && (genes[2] as NullableGene).gene is StringGene)
    }

    @Test
    fun testInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("people", setOf("id", "name", "address"))
        val genes = actions[0].seeTopGenes()

        val idValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value
        val nameValue = (genes[1] as StringGene).value
        val addressValue = ((genes[2] as NullableGene).gene as StringGene).value

        val query = "Select * from people where id=%s".format(idValue)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(actions)

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        assertEquals(nameValue, row.getValueByName("name"))
        assertEquals(addressValue, row.getValueByName("address"))

    }

    @Test
    fun testInsertionWithQuotes() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("people", setOf("id", "name", "address"))
        val genes = actions[0].seeTopGenes()

        val idValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value

        val oneQuoteStr = "'"
        val twoQuotesStr = "'hi'"

        (genes[1] as StringGene).copyValueFrom(StringGene(genes[1].name, oneQuoteStr))
        ((genes[2] as NullableGene).gene as StringGene).copyValueFrom(StringGene(genes[1].name, twoQuotesStr))

        val query = "Select * from people where id=%s".format(idValue)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(actions)

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        assertEquals(oneQuoteStr, row.getValueByName("name"))
        assertEquals(twoQuotesStr, row.getValueByName("address"))

    }
}