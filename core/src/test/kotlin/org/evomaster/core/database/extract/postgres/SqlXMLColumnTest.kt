package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.postgresql.jdbc.PgSQLXML


/**
 * Created by jgaleotti on 29-May-19.
 */
class SqlXMLColumnTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/xml_column_db.sql"

    @Test
    fun testXMLColumnExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmlData"))
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlXMLGene)

    }

    @Test
    fun testEmptyXMLInsertion() {
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

    @Test
    fun testIntegerElementInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmldata"))

        val action = actions[0]

        val newGene = SqlXMLGene("xmldata", ObjectGene("anElement", listOf(IntegerGene("integerElement", value = 0))))
        val newInsertAction = DbAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(listOf(newInsertAction))

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val xmlDataValue = row.getValueByName("xmlData")

        assertTrue(xmlDataValue is PgSQLXML)
        xmlDataValue as PgSQLXML
        assertEquals("<anElement>0</anElement>", xmlDataValue.string)
    }

    @Test
    fun testBooleanElementInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmldata"))

        val action = actions[0]

        val newGene = SqlXMLGene("xmldata", ObjectGene("anElement", listOf(BooleanGene("booleanElement", value = false))))
        val newInsertAction = DbAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(listOf(newInsertAction))

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val xmlDataValue = row.getValueByName("xmlData")

        assertTrue(xmlDataValue is PgSQLXML)
        xmlDataValue as PgSQLXML
        assertEquals("<anElement>false</anElement>", xmlDataValue.string)
    }

    @Test
    fun testStringElementInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmldata"))

        val action = actions[0]

        val newGene = SqlXMLGene("xmldata", ObjectGene("anElement", listOf(StringGene("stringElement", value = "Hello World"))))
        val newInsertAction = DbAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(listOf(newInsertAction))

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val xmlDataValue = row.getValueByName("xmlData")

        assertTrue(xmlDataValue is PgSQLXML)
        xmlDataValue as PgSQLXML
        assertEquals("<anElement>Hello World</anElement>", xmlDataValue.string)
    }

    @Test
    fun testEscapeString() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmldata"))

        val action = actions[0]

        val newGene = SqlXMLGene("xmldata", ObjectGene("anElement", listOf(StringGene("stringElement", value = "<xml>This should be escaped</xml>"))))
        val newInsertAction = DbAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(listOf(newInsertAction))

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val xmlDataValue = row.getValueByName("xmlData")

        assertTrue(xmlDataValue is PgSQLXML)
        xmlDataValue as PgSQLXML
        assertEquals("<anElement>&lt;xml&gt;This should be escaped&lt;/xml&gt;</anElement>", xmlDataValue.string)
    }


    @Test
    fun testNestedXMLInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmldata"))

        val action = actions[0]

        val childElement = ObjectGene("childElement", listOf())
        val parentElement = ObjectGene("parentElement", listOf(childElement))
        val newGene = SqlXMLGene("xmldata", parentElement)
        val expectedXML = newGene.getValueAsPrintableString(mode = "xml")

        val newInsertAction = DbAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(listOf(newInsertAction))

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        val xmlDataValue = row.getValueByName("xmlData")

        assertTrue(xmlDataValue is PgSQLXML)
        xmlDataValue as PgSQLXML
        assertEquals("<parentElement><childElement></childElement></parentElement>", xmlDataValue.string)
    }


}