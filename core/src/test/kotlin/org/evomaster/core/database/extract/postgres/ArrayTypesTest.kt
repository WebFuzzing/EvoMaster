package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class ArrayTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_array_types.sql"


    @Test
    fun testArrayTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("ArrayTypes",
            setOf("arrayColumn",
                "matrixColumn",
                "spaceColumn",
                "exactSizeArrayColumn",
                "exactSizeMatrixColumn"))

        val genes = actions[0].seeGenes()

        assertEquals(5, genes.size)
        assertTrue(genes[0] is ArrayGene<*>) // money

        val dbCommandDto = DbActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }
}