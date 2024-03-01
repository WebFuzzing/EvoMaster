package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.sql.SqlLogSeqNumberGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class PgLsnTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_pg_lsn.sql"

    @Test
    fun testExtractionOfPgLsnType() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        assertTrue(schema.tables.any { it.name.equals("PgLsnType".lowercase()) })
        val table = schema.tables.find { it.name.equals("PgLsnType".lowercase()) }

        assertTrue(table!!.columns.any { it.name.equals("pglsnColumn".lowercase()) })
        val nonArrayColumnDto = table.columns.find { it.name.equals("pglsnColumn".lowercase()) }!!
        assertEquals("pg_lsn", nonArrayColumnDto.type)

    }

    @Test
    fun testBuildGenesOfPgLSN() {

        val schema = SchemaExtractor.extract(connection)


        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "PgLsnType",
            setOf(
                "dummyColumn",
                "pglsnColumn"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is IntegerGene)
        assertTrue(genes[1] is SqlLogSeqNumberGene)

        val pglsnColumn = genes[1] as SqlLogSeqNumberGene
        assertEquals("\"0/0\"", pglsnColumn.getValueAsPrintableString())

        val leftPartGene = pglsnColumn.getViewOfChildren()[0] as LongGene
        val rightPartGene = pglsnColumn.getViewOfChildren()[1] as LongGene

        leftPartGene.value = 4294967295L
        rightPartGene.value = 4294967295L

        assertEquals("\"FFFFFFFF/FFFFFFFF\"", pglsnColumn.getValueAsPrintableString())
    }

    @Test
    fun testInsertionOfGenesOfPgLSN() {

        val schema = SchemaExtractor.extract(connection)


        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "PgLsnType",
                setOf(
                        "dummyColumn",
                        "pglsnColumn"
                )
        )

        val genes = actions[0].seeTopGenes()
        val pglsnColumn = genes[1] as SqlLogSeqNumberGene
        val leftPartGene = pglsnColumn.getViewOfChildren()[0] as LongGene
        val rightPartGene = pglsnColumn.getViewOfChildren()[1] as LongGene
        leftPartGene.value = 4294967295L
        rightPartGene.value = 4294967295L

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }



}