package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.sql.SqlRangeGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class RangeTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_range_types.sql"


    @Test
    fun testEmptyRangeTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "RangeTypes",
            setOf(
                "int4rangeColumn",
                "int8rangeColumn",
                "numrangeColumn",
                "tsrangeColumn",
                "tstzrangeColumn",
                "daterangeColumn"

            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(6, genes.size)
        assertTrue(genes[0] is SqlRangeGene<*>)
        assertTrue(genes[1] is SqlRangeGene<*>)
        assertTrue(genes[2] is SqlRangeGene<*>)
        assertTrue(genes[3] is SqlRangeGene<*>)
        assertTrue(genes[4] is SqlRangeGene<*>)
        assertTrue(genes[5] is SqlRangeGene<*>)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

    @Test
    fun testNonEmptyRangeTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "RangeTypes",
            setOf(
                "int4rangeColumn",
                "int8rangeColumn",
                "numrangeColumn",
                "tsrangeColumn",
                "tstzrangeColumn",
                "daterangeColumn"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(6, genes.size)
        assertTrue(genes[0] is SqlRangeGene<*>)
        assertTrue(genes[1] is SqlRangeGene<*>)
        assertTrue(genes[2] is SqlRangeGene<*>)
        assertTrue(genes[3] is SqlRangeGene<*>)
        assertTrue(genes[4] is SqlRangeGene<*>)
        assertTrue(genes[5] is SqlRangeGene<*>)

        val int4rangeGene = SqlRangeGene(
            name = "int4range",
            template = IntegerGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = IntegerGene("left", value = 1),
            right = IntegerGene("right", value = 1),
            isRightClosed = BooleanGene("left", value = true)
        )

        val int8rangeGene = SqlRangeGene(
            name = "int8range",
            template = LongGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = LongGene("left", value = 1L),
            right = LongGene("right", value = 1L),
            isRightClosed = BooleanGene("right", value = true)
        )

        val numrangeGene = SqlRangeGene(
            name = "numrange",
            template = FloatGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = FloatGene("left", value = 1.0f),
            right = FloatGene("right", value = 1.0f),
            isRightClosed = BooleanGene("right", value = true)
        )

        val timestamprangeGene = SqlRangeGene(
            name = "daterange",
            template = DateTimeGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = DateTimeGene(
                "left", date =DateGene(
                    "date", year = IntegerGene("year", value = 1900),
                    month = IntegerGene("month", value = 1),
                    day = IntegerGene("day", value = 1)
                ),
                time = TimeGene("time", hour= IntegerGene("hour",value=0),
                minute = IntegerGene("hour",value=0)
                )
            ),
            right = DateTimeGene(
                "left", date =DateGene(
                    "date", year = IntegerGene("year", value = 2000),
                    month = IntegerGene("month", value = 1),
                    day = IntegerGene("day", value = 1)
                ),
                time = TimeGene("time", hour= IntegerGene("hour",value=0),
                    minute = IntegerGene("hour",value=0)
                )
            ),
            isRightClosed = BooleanGene("right", value = true)
        )


        val daterangeGene = SqlRangeGene(
            name = "daterange",
            template = DateGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = DateGene(
                "left", year = IntegerGene("year", value = 1900),
                month = IntegerGene("month", value = 1),
                day = IntegerGene("day", value = 1)
            ),
            right = DateGene(
                "right",
                year = IntegerGene("year", value = 2000),
                month = IntegerGene("month", value = 1),
                day = IntegerGene("day", value = 1)
            ),
            isRightClosed = BooleanGene("right", value = true)
        )

        genes[0].copyValueFrom(int4rangeGene)
        genes[1].copyValueFrom(int8rangeGene)
        genes[2].copyValueFrom(numrangeGene)
        genes[3].copyValueFrom(timestamprangeGene)
        genes[4].copyValueFrom(timestamprangeGene)
        genes[5].copyValueFrom(daterangeGene)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

}