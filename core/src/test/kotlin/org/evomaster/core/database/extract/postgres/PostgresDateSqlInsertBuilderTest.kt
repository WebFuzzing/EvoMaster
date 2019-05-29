package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 29-May-19.
 */
class PostgresDateSqlInsertBuilderTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/date_column_db.sql"

    @Test
    fun testDateGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("logins", setOf("userId", "lastLogin"))
        val genes = actions[0].seeGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        assertTrue(genes[1] is DateGene)
    }
}