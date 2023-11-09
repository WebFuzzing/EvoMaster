package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 07-May-19.
 */
class CharacterTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_character_types.sql"


    @Test
    fun testCharacterTypes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
            "CharacterTypes", setOf(
                "characterVaryingColumn",
                "varcharColumn",
                "characterColumn",
                "charColumn",
                "textColumn"
            )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(5, genes.size)
        assertTrue(genes[0] is StringGene) //character varying
        assertEquals(0, (genes[0] as StringGene).minLength)
        assertEquals(1, (genes[0] as StringGene).maxLength)

        assertTrue(genes[1] is StringGene) // varchar
        assertEquals(0, (genes[1] as StringGene).minLength)
        assertEquals(1, (genes[1] as StringGene).maxLength)

        assertTrue(genes[2] is StringGene) // character
        assertEquals(1, (genes[2] as StringGene).minLength)
        assertEquals(1, (genes[2] as StringGene).maxLength)

        assertTrue(genes[3] is StringGene) // char
        assertEquals(1, (genes[3] as StringGene).minLength)
        assertEquals(1, (genes[3] as StringGene).maxLength)

        assertTrue(genes[4] is StringGene) // text
        assertEquals(0, (genes[4] as StringGene).minLength)
        assertEquals(Int.MAX_VALUE, (genes[4] as StringGene).maxLength)

        (genes[0] as StringGene).value = "f"
        (genes[1] as StringGene).value = "f"
        (genes[2] as StringGene).value = "f"
        (genes[3] as StringGene).value = "f"
        (genes[4] as StringGene).value = "f"

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }

    @Test
    fun testInsertionOfQuotes() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "CharacterTypes", setOf(
                "characterVaryingColumn",
                "varcharColumn",
                "characterColumn",
                "charColumn",
                "textColumn"
        )
        )

        val genes = actions[0].seeTopGenes()

        (genes[0] as StringGene).value = "f"
        (genes[1] as StringGene).value = "f"
        (genes[2] as StringGene).value = "f"
        (genes[3] as StringGene).value = "f"
        (genes[4] as StringGene).value = "Hello\""

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)

    }


}