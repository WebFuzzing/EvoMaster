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
import org.evomaster.core.search.gene.sql.SqlMultiRangeGene
import org.evomaster.core.search.gene.sql.SqlRangeGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 04-May-22.
 */
class MultiRangeTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_multirange_types.sql"


    @Test
    fun testEmptyMultiRangeTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "MultiRangeBuiltInTypes",
                setOf(
                        "dummyColumn",
                        "int4multirangeColumn",
                        "int8multirangeColumn",
                        "nummultirangeColumn",
                        "tsmultirangeColumn",
                        "tstzmultirangeColumn",
                        "datemultirangeColumn"
                )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(7, genes.size)
        assertTrue(genes[1] is SqlMultiRangeGene<*>)
        assertTrue(genes[2] is SqlMultiRangeGene<*>)
        assertTrue(genes[3] is SqlMultiRangeGene<*>)
        assertTrue(genes[4] is SqlMultiRangeGene<*>)
        assertTrue(genes[5] is SqlMultiRangeGene<*>)
        assertTrue(genes[6] is SqlMultiRangeGene<*>)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

    @Test
    fun testOneElementPerMultiRangeInsertion() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "MultiRangeBuiltInTypes",
                setOf(
                        "dummyColumn",

                        "int4multirangeColumn",
                        "int8multirangeColumn",
                        "nummultirangeColumn",
                        "tsmultirangeColumn",
                        "tstzmultirangeColumn",
                        "datemultirangeColumn"
                )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(7, genes.size)
        assertTrue(genes[1] is SqlMultiRangeGene<*>)
        assertTrue(genes[2] is SqlMultiRangeGene<*>)
        assertTrue(genes[3] is SqlMultiRangeGene<*>)
        assertTrue(genes[4] is SqlMultiRangeGene<*>)
        assertTrue(genes[5] is SqlMultiRangeGene<*>)
        assertTrue(genes[6] is SqlMultiRangeGene<*>)

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
                        "left", date = DateGene(
                        "date", year = IntegerGene("year", value = 1900),
                        month = IntegerGene("month", value = 1),
                        day = IntegerGene("day", value = 1)
                ),
                        time = TimeGene("time", hour = IntegerGene("hour", value = 0),
                                minute = IntegerGene("hour", value = 0)
                        )
                ),
                right = DateTimeGene(
                        "left", date = DateGene(
                        "date", year = IntegerGene("year", value = 2000),
                        month = IntegerGene("month", value = 1),
                        day = IntegerGene("day", value = 1)
                ),
                        time = TimeGene("time", hour = IntegerGene("hour", value = 0),
                                minute = IntegerGene("hour", value = 0)
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

        val int4MultiRangeGene = genes[1] as SqlMultiRangeGene<*>
        val newInt4rangeGene = int4MultiRangeGene.template.copy() as SqlRangeGene<IntegerGene>
        newInt4rangeGene.copyValueFrom(int4rangeGene)
        int4MultiRangeGene.rangeGenes.addElement(newInt4rangeGene.apply { doInitialize() })

        val int8MultiRangeGene = genes[2] as SqlMultiRangeGene<*>
        val newInt8rangeGene = int8MultiRangeGene.template.copy() as SqlRangeGene<LongGene>
        newInt8rangeGene.copyValueFrom(int8rangeGene)
        int8MultiRangeGene.rangeGenes.addElement(newInt8rangeGene.apply { doInitialize() })

        val numMultiRangeGene = genes[3] as SqlMultiRangeGene<*>
        val newNumrangeGene = numMultiRangeGene.template.copy() as SqlRangeGene<FloatGene>
        newNumrangeGene.copyValueFrom(numrangeGene)
        numMultiRangeGene.rangeGenes.addElement(newNumrangeGene.apply { doInitialize() })

        val timestampMultiRangeGene = genes[4] as SqlMultiRangeGene<*>
        val newTimestamprangeGene = timestampMultiRangeGene.template.copy() as SqlRangeGene<DateGene>
        newTimestamprangeGene.copyValueFrom(timestamprangeGene)
        timestampMultiRangeGene.rangeGenes.addElement(newTimestamprangeGene.apply { doInitialize() })

        val timestampTzMultiRangeGene = genes[5] as SqlMultiRangeGene<*>
        val newTimestampTzRangeGene = timestampTzMultiRangeGene.template.copy() as SqlRangeGene<DateGene>
        newTimestampTzRangeGene.copyValueFrom(timestamprangeGene)
        timestampTzMultiRangeGene.rangeGenes.addElement(newTimestampTzRangeGene.apply { doInitialize() })

        val dateMultiRangeGene = genes[6] as SqlMultiRangeGene<*>
        val newDateRangeGene = dateMultiRangeGene.template.copy() as SqlRangeGene<DateGene>
        newDateRangeGene.copyValueFrom(daterangeGene)
        dateMultiRangeGene.rangeGenes.addElement(newDateRangeGene.apply { doInitialize() })

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

}