package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.sql.SqlXMLGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
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
        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlXMLGene)

    }

    @Test
    fun testEmptyXMLInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("xmlData"))

        val genes = actions[0].seeTopGenes()


        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(actions)

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
        val newInsertAction = SqlAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(listOf(newInsertAction))

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
        val newInsertAction = SqlAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(listOf(newInsertAction))

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
        val newInsertAction = SqlAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(listOf(newInsertAction))

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
        val newInsertAction = SqlAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(listOf(newInsertAction))

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
        val expectedXML = newGene.getValueAsPrintableString(mode = GeneUtils.EscapeMode.XML)

        val newInsertAction = SqlAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(newGene))

        val query = "Select * from x"

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(listOf(newInsertAction))

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