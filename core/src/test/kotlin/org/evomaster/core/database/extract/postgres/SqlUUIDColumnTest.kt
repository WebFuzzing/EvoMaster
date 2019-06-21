package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.SqlUUIDGene
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Created by jgaleotti on 29-May-19.
 */
class SqlUUIDColumnTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/uuid_column_db.sql"

    @Test
    fun testExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("purchases", setOf("id", "uuid"))
        val genes = actions[0].seeGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue(genes[1] is SqlUUIDGene)
        val uuidGene = genes[1] as SqlUUIDGene

        val expectedUUIDStringValue = UUID(0L, 0L).toString()

        assertEquals(expectedUUIDStringValue, uuidGene.getValueAsRawString())
    }

    @Test
    fun testInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("purchases", setOf("id", "uuid"))
        val genes = actions[0].seeGenes()

        val idValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value
        val expectedUUID = (genes[1] as SqlUUIDGene).getValueAsUUID()

        val dbCommandDto = DbActionTransformer.transform(actions)

        val query = "Select * from purchases where id=%s".format(idValue)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        Assertions.assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        assertEquals(expectedUUID, row.getValueByName("uuid"))
    }

    @Test
    fun testExtractUUIDPrimaryKey() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("uuid"))
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue((genes[0] as SqlPrimaryKeyGene).gene is SqlUUIDGene)
        val uuidGene = (genes[0] as SqlPrimaryKeyGene).gene as SqlUUIDGene

        val expectedUUID = UUID(0L, 0L)

        val actualUUID = uuidGene.getValueAsUUID()

        assertEquals(expectedUUID, actualUUID)
    }

    @Test
    fun testInsertUUIDPrimaryKey() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("uuid"))
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue((genes[0] as SqlPrimaryKeyGene).gene is SqlUUIDGene)
        val uuidGene = (genes[0] as SqlPrimaryKeyGene).gene as SqlUUIDGene
        val expectedUUID = uuidGene.getValueAsUUID()


        val query = "Select * from x where uuid='{%s}'".format(expectedUUID)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        Assertions.assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        assertEquals(expectedUUID, row.getValueByName("uuid"))

    }

    @Test
    fun testExtractUUIDFirstColumn() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("y", setOf("uuid"))
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlUUIDGene)
        val uuidGene = genes[0] as SqlUUIDGene

        val expectedUUID = UUID(0L, 0L)

        val actualUUID = uuidGene.getValueAsUUID()

        assertEquals(expectedUUID, actualUUID)
    }

    @Test
    fun testInsertUUIFirstColumn() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("y", setOf("uuid"))
        val genes = actions[0].seeGenes()

        val uuidGene = genes[0] as SqlUUIDGene

        val expectedUUID = uuidGene.getValueAsUUID()


        val query = "Select * from y where uuid='{%s}'".format(expectedUUID)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        Assertions.assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        assertEquals(expectedUUID, row.getValueByName("uuid"))

    }
}