package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.sql.time.SqlTimeIntervalGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class DatetimeTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_datetime_types.sql"


    @Test
    fun testDatetimeTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "DatetimeTypes", setOf(
                "timestampColumn",
                "timestampWithTimeZoneColumn",
                "dateColumn",
                "timeColumn",
                "timeWithTimeZoneColumn",
                "intervalColumn"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(6, genes.size)

        assertTrue(genes[0] is DateTimeGene)
        assertTrue(genes[1] is DateTimeGene)
        assertTrue(genes[2] is DateGene)
        assertTrue(genes[3] is TimeGene)
        assertTrue(genes[4] is TimeGene)
        assertTrue(genes[5] is SqlTimeIntervalGene)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }
}