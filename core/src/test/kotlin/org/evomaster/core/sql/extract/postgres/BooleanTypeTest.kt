package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class BooleanTypeTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_boolean_type.sql"


    @Test
    fun testBooleanType() {

        val schema = DbInfoExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("postgres", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "BooleanType", setOf(
                "booleanColumn"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is BooleanGene)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }
}