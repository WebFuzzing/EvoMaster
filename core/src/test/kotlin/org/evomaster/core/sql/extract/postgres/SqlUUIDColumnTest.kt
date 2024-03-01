package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.NullableGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.UUIDGene
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
        val genes = actions[0].seeTopGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue(genes[1] is UUIDGene)
        val uuidGene = genes[1] as UUIDGene

        val expectedUUIDStringValue = UUID(0L, 0L).toString()

        assertEquals(expectedUUIDStringValue, uuidGene.getValueAsRawString())
    }

    @Test
    fun testInsertion() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("purchases", setOf("id", "uuid"))
        val genes = actions[0].seeTopGenes()

        val idValue = ((genes[0] as SqlPrimaryKeyGene).gene as IntegerGene).value
        val expectedUUID = (genes[1] as UUIDGene).getValueAsUUID()

        val dbCommandDto = SqlActionTransformer.transform(actions)

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
        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue((genes[0] as SqlPrimaryKeyGene).gene is UUIDGene)
        val uuidGene = (genes[0] as SqlPrimaryKeyGene).gene as UUIDGene

        val expectedUUID = UUID(0L, 0L)

        val actualUUID = uuidGene.getValueAsUUID()

        assertEquals(expectedUUID, actualUUID)
    }

    @Test
    fun testInsertUUIDPrimaryKey() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("uuid"))
        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue((genes[0] as SqlPrimaryKeyGene).gene is UUIDGene)
        val uuidGene = (genes[0] as SqlPrimaryKeyGene).gene as UUIDGene
        val expectedUUID = uuidGene.getValueAsUUID()


        val query = "Select * from x where uuid='{%s}'".format(expectedUUID)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(actions)
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
        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is NullableGene)
        val uuidGene = (genes[0] as NullableGene).gene  as UUIDGene

        val expectedUUID = UUID(0L, 0L)

        val actualUUID = uuidGene.getValueAsUUID()

        assertEquals(expectedUUID, actualUUID)
    }

    @Test
    fun testInsertUUIFirstColumn() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("y", setOf("uuid"))
        val genes = actions[0].seeTopGenes()

        val uuidGene = (genes[0] as NullableGene).gene as UUIDGene

        val expectedUUID = uuidGene.getValueAsUUID()


        val query = "Select * from y where uuid='{%s}'".format(expectedUUID)

        val queryResultBeforeInsertion = SqlScriptRunner.execCommand(connection, query)
        assertTrue(queryResultBeforeInsertion.isEmpty)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        val queryResultAfterInsertion = SqlScriptRunner.execCommand(connection, query)
        Assertions.assertFalse(queryResultAfterInsertion.isEmpty)

        val row = queryResultAfterInsertion.seeRows()[0]
        assertEquals(expectedUUID, row.getValueByName("uuid"))

    }
}