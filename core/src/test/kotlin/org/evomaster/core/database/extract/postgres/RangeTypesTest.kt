package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlNumericRangeGene
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
                "numrangeColumn"
            )
        )

        val genes = actions[0].seeGenes()

        assertEquals(3, genes.size)
        assertTrue(genes[0] is SqlNumericRangeGene<*>)
        assertTrue(genes[1] is SqlNumericRangeGene<*>)
        assertTrue(genes[2] is SqlNumericRangeGene<*>)

        val dbCommandDto = DbActionTransformer.transform(actions)
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
                "numrangeColumn"
            )
        )

        val genes = actions[0].seeGenes()

        assertEquals(3, genes.size)
        assertTrue(genes[0] is SqlNumericRangeGene<*>)
        assertTrue(genes[1] is SqlNumericRangeGene<*>)
        assertTrue(genes[2] is SqlNumericRangeGene<*>)

        val int4rangeGene = SqlNumericRangeGene<Int>(
            name = "int4range",
            template = IntegerGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = IntegerGene("left", value = 1),
            right = IntegerGene("right", value = 1),
            isRightClosed = BooleanGene("left", value = true)
        )

        val int8rangeGene = SqlNumericRangeGene<Long>(
            name = "int8range",
            template = LongGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = LongGene("left", value = 1L),
            right = LongGene("right", value = 1L),
            isRightClosed = BooleanGene("right", value = true)
        )

        val numrangeGene = SqlNumericRangeGene<Float>(
            name = "numrange",
            template = FloatGene(name = "template"),
            isLeftClosed = BooleanGene("left", value = true),
            left = FloatGene("left", value = 1.0f),
            right = FloatGene("right", value = 1.0f),
            isRightClosed = BooleanGene("right", value = true)
        )

        genes[0].copyValueFrom(int4rangeGene)
        genes[1].copyValueFrom(int8rangeGene)
        genes[2].copyValueFrom(numrangeGene)

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

}