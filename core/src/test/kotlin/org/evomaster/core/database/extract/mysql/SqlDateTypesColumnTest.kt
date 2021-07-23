package org.evomaster.core.database.extract.mysql

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.DateTimeGene
import org.evomaster.core.search.gene.TimeGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlDateTypesColumnTest : ExtractTestBaseMySQL() {

    override fun getSchemaLocation() = "/sql_schema/mysql_create_datetypes.sql"

    @Test
    fun testExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("t1", setOf("t", "dt", "ts"))
        val genes = actions[0].seeGenes()

        assertEquals(3, genes.size)
        assertTrue(genes[0] is TimeGene)
        assertTrue(genes[1] is DateTimeGene)
        assertTrue(genes[2] is DateTimeGene)

    }
}