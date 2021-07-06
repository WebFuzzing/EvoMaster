package org.evomaster.core.database.extract.mysql

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.regex.RegexGene
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LikeCheckTest : ExtractTestBaseMySQL() {

    override fun getSchemaLocation(): String = "/sql_schema/like_check_db.sql"

    @Test
    fun testRegex(){
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("f_id"))
        val genes = actions[0].seeGenes()

        Assertions.assertEquals(1, genes.size)
        Assertions.assertTrue(genes[0] is RegexGene)
    }

}