package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.sql.SqlBinaryStringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class BinaryTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_binary_types.sql"


    @Test
    fun testBinaryTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "BinaryTypes", setOf(
                "byteaColumn"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlBinaryStringGene) //character varying
        assertEquals(0, (genes[0] as SqlBinaryStringGene).minSize)

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

    @Test
    fun testFailure() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "BinaryTypes", setOf(
                "byteaColumn"
        )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is SqlBinaryStringGene)

        val sqlBinaryStringGene = genes[0] as SqlBinaryStringGene

        val arrayGene  = sqlBinaryStringGene.getViewOfChildren()[0] as ArrayGene<IntegerGene>
        val integerGene0 = arrayGene.template.copy() as IntegerGene
        integerGene0.value = 0

        val integerGene1 = arrayGene.template.copy() as IntegerGene
        integerGene1.value = 42

        val integerGene2 = arrayGene.template.copy() as IntegerGene
        integerGene2.value = 255

        val integerGene3= arrayGene.template.copy() as IntegerGene
        integerGene3.value = 16

        arrayGene.addElement(integerGene0)
        arrayGene.addElement(integerGene1)
        arrayGene.addElement(integerGene2)
        arrayGene.addElement(integerGene3)

        assertEquals("\"\\x002aff10\"",sqlBinaryStringGene.getValueAsPrintableString())

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }
}