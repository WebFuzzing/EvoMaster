package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.service.Randomness
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


/**
 * Created by jgaleotti on 29-May-19.
 */
class LikeCheckTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/like_check_db.sql"

    @Test
    fun testLikeRegexGeneExtraction() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("f_id"))
        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is RegexGene)

    }

    @Test
    fun testInsertRegexGene() {
        val randomness = Randomness()
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("f_id"))

        val genes = actions[0].seeTopGenes()
        val regexGene = genes.firstIsInstance<RegexGene>()

        for (i in 1..100) {
            regexGene.randomize(randomness, false)


            val dbCommandDto = SqlActionTransformer.transform(actions)
            SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        }
    }

}