package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.SqlInsertBuilder
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
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is RegexGene)

    }

    @Test
    fun testInsertRegexGene() {
        val randomness = Randomness()
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("x", setOf("f_id"))

        val genes = actions[0].seeGenes()
        val regexGene = genes.firstIsInstance<RegexGene>()

        for (i in 1..100) {
            regexGene.randomize(randomness, false, listOf())


            val dbCommandDto = DbActionTransformer.transform(actions)
            SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

        }
    }

}