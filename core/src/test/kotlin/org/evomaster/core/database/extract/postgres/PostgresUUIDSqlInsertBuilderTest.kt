package org.evomaster.core.database.extract.postgres

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.UUIDGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Created by jgaleotti on 29-May-19.
 */
class PostgresUUIDSqlInsertBuilderTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/uuid_column_db.sql"

    @Test
    fun testUUIDGeneCreation() {
        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("purchases", setOf("purchaseId"))
        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlPrimaryKeyGene)
        val purchaseIdGene = genes[0] as SqlPrimaryKeyGene
        assertTrue(purchaseIdGene.gene is UUIDGene)

        val uuidGene = purchaseIdGene.gene as UUIDGene

        val expectedUUIDStringValue = UUID(0L, 0L).toString()

        assertEquals(expectedUUIDStringValue, uuidGene.getValueAsRawString())
    }
}