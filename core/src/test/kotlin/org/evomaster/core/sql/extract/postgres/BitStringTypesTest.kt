package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.sql.SqlBitStringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 18-Apr-22.
 */
class BitStringTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_bitstring_types.sql"


    @Test
    fun testBitStringTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "BitStringTypes", setOf(
                "bitColumn",
                "bitvaryingColumn"
        )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(2, genes.size)
        assertTrue(genes[0] is SqlBitStringGene) //character varying
        val bitColumnGene = genes[0] as SqlBitStringGene
        assertEquals(5, bitColumnGene.minSize)
        assertEquals(5, bitColumnGene.maxSize)

        val arrayGene = bitColumnGene.getViewOfChildren()[0] as ArrayGene<BooleanGene>
        repeat(bitColumnGene.minSize) {
            arrayGene.addElement(BooleanGene("booleanGene"))
        }
        assertTrue(genes[1] is SqlBitStringGene) //character varying
        val bitVaryingColumnGene = genes[1] as SqlBitStringGene
        assertEquals(0, bitVaryingColumnGene.minSize)
        assertEquals(10, bitVaryingColumnGene.maxSize)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }
}