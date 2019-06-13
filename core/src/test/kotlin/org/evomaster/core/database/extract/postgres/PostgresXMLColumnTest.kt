package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.SqlXMLGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.postgresql.jdbc.PgSQLXML


/**
 * Created by jgaleotti on 29-May-19.
 */
class PostgresXMLColumnTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/xml_column_db.sql"

    @Test
    fun testExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmlData"))
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlXMLGene)

    }

    @Test
    fun testInsertionEmptyXML() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmlData"))

        val genes = actions[0].seeGenes()


        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(actions)

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val xmlDataValue = row.getValueByName("xmlData")

        assertTrue(xmlDataValue is PgSQLXML)
        xmlDataValue as PgSQLXML
        assertEquals("<xmldata></xmldata>", xmlDataValue.string)
    }
}