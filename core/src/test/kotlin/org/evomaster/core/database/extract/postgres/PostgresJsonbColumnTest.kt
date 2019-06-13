package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject

/**
 * Created by jgaleotti on 29-May-19.
 */
class PostgresJsonbColumnTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/jsonb_column_db.sql"

    @Test
    fun testExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("people", setOf("id", "jsonData"))
        val genes = actions[0].seeGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue(genes[1] is SqlJSONGene)

    }

    @Test
    fun testInsertionEmptyObject() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("people", setOf("id", "jsonData"))
        val genes = actions[0].seeGenes()

        val idValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value
        assertTrue(genes[1] is SqlJSONGene)

        val query = "Select * from people where id=%s".format(idValue)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val jsonDataValue = queryResultAfterInsertion.seeRows()[0].getValueByName("jsonData");
        assertEquals("{}", jsonDataValue.toString())

    }

    @Test
    fun testInsertionNonEmptyObject() {
        val schema = SchemaExtractor.extract(connection)


        val tableDto = schema.tables[0]

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("people", setOf("id", "jsonData"))

        val action = actions[0]
        val genes = action.seeGenes()


        val idValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value
        assertTrue(genes[1] is SqlJSONGene)

        val objectGene = ObjectGene("jsondata", fields = listOf(IntegerGene("integerValue", value = 0), StringGene("stringValue", value = "Hello World"), BooleanGene("booleanValue", value = false)))
        val newGene = SqlJSONGene("jsondata", objectGene)

        val newInsertAction = DbAction(table = action.table, selectedColumns = action.selectedColumns, id = action.geInsertionId(), computedGenes = listOf(genes[0], newGene))

        val query = "Select * from people where id=%s".format(idValue)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(listOf(newInsertAction))
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        assertFalse(queryResultAfterInsertion.isEmpty)

        val jsonDataValue = queryResultAfterInsertion.seeRows()[0].getValueByName("jsonData");
        assertTrue(jsonDataValue is PGobject)
        jsonDataValue as PGobject

        val jsonString = jsonDataValue.value

        assertTrue(jsonString.contains("\"stringValue\": \"Hello World\""))
        assertTrue(jsonString.contains("\"booleanValue\": false"))
        assertTrue(jsonString.contains("\"integerValue\": 0"))

    }
}