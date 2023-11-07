package org.evomaster.core.sql.extract.mysql

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlDateTypesColumnTest : ExtractTestBaseMySQL() {

    override fun getSchemaLocation() = "/sql_schema/mysql_create_datetypes.sql"

    @Test
    fun testExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("t1", setOf("t", "dt", "ts"))
        val genes = actions[0].seeTopGenes()

        assertEquals(3, genes.size)
        assertTrue(genes[0] is TimeGene)
        assertTrue(genes[1] is DateTimeGene)
        assertTrue(genes[2] is DateTimeGene)

    }
}