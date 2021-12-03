package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.DoubleGene
import org.evomaster.core.search.gene.FloatGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class NumericTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_numeric_types.sql"


    @Test
    fun testNumericTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("NumericTypes", setOf("smallintColumn",
            "integerColumn",
            "bigintColumn",
            "decimalColumn",
            "numericColumn",
            "realColumn",
            "doublePrecisionColumn",
            "smallserialColumn",
            "serialColumn",
            "bigserialColumn"))

        val genes = actions[0].seeGenes()

        assertEquals(10, genes.size)
        assertTrue(genes[0] is IntegerGene) //smallint
        assertTrue(genes[1] is IntegerGene) // integer
        assertTrue(genes[2] is LongGene) // bigint
        assertTrue(genes[3] is FloatGene) //decimal
        assertTrue(genes[4] is FloatGene) // numeric
        assertTrue(genes[5] is DoubleGene) //real
        assertTrue(genes[6] is DoubleGene) // double precision
        assertTrue(genes[7] is SqlAutoIncrementGene) // smallserial
        assertTrue(genes[8] is SqlAutoIncrementGene) // serial
        assertTrue(genes[9] is SqlAutoIncrementGene) // bigserial

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }
}