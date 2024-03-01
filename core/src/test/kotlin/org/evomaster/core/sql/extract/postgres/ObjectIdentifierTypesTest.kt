package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 05-May-22.
 */
class ObjectIdentifierTypesTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_object_identifiers.sql"

    @Test
    fun testInsertionFormat() {
        SqlScriptRunner.execInsert(connection, "INSERT INTO ObjectIdentifierTypes(dummyColumn, " +
                "oidColumn, " +
                "regclassColumn, " +
                "regcollationColumn, " +
                "regconfigColumn, " +
                "regdictionaryColumn," +
                "regnamespaceColumn," +
                "regoperColumn," +
                "regoperatorColumn," +
                "regprocColumn," +
                "regprocedureColumn," +
                "regroleColumn," +
                "regtypeColumn) " +
                "values (1,0,0,0,0,0,0,0,0,0,0,0,0);")
    }

    @Test
    fun testExtractionOfOidColumns() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("public", schema.name.lowercase())
        assertEquals(DatabaseType.POSTGRES, schema.databaseType)

        assertTrue(schema.tables.any { it.name.equals("ObjectIdentifierTypes".lowercase()) })
        val table = schema.tables.find { it.name.equals("ObjectIdentifierTypes".lowercase()) }

        assertTrue(table!!.columns.any { it.name.equals("oidColumn".lowercase()) })

        val oidColumnDto = table.columns.find { it.name.equals("oidColumn".lowercase()) }!!
        val regclassColumnDto = table.columns.find { it.name.equals("regclassColumn".lowercase()) }!!
        val regcollationColumnDto = table.columns.find { it.name.equals("regcollationColumn".lowercase()) }!!
        val regconfigColumnDto = table.columns.find { it.name.equals("regconfigColumn".lowercase()) }!!
        val regdictionaryColumnDto = table.columns.find { it.name.equals("regdictionaryColumn".lowercase()) }!!
        val regnamespaceColumnDto = table.columns.find { it.name.equals("regnamespaceColumn".lowercase()) }!!
        val regoperColumnDto = table.columns.find { it.name.equals("regoperColumn".lowercase()) }!!
        val regoperatorColumnDto = table.columns.find { it.name.equals("regoperatorColumn".lowercase()) }!!
        val regprocColumnDto = table.columns.find { it.name.equals("regprocColumn".lowercase()) }!!
        val regprocedureColumnDto = table.columns.find { it.name.equals("regprocedureColumn".lowercase()) }!!
        val regroleColumnDto = table.columns.find { it.name.equals("regroleColumn".lowercase()) }!!
        val regtypeColumnDto = table.columns.find { it.name.equals("regtypeColumn".lowercase()) }!!

        assertEquals("oid", oidColumnDto.type)
        assertEquals("regclass", regclassColumnDto.type)
        assertEquals("regcollation", regcollationColumnDto.type)
        assertEquals("regconfig", regconfigColumnDto.type)
        assertEquals("regdictionary", regdictionaryColumnDto.type)
        assertEquals("regnamespace", regnamespaceColumnDto.type)
        assertEquals("regoper", regoperColumnDto.type)
        assertEquals("regoperator", regoperatorColumnDto.type)
        assertEquals("regproc", regprocColumnDto.type)
        assertEquals("regprocedure", regprocedureColumnDto.type)
        assertEquals("regrole", regroleColumnDto.type)
        assertEquals("regtype", regtypeColumnDto.type)

    }

    @Test
    fun testBuildGenesOfOidColumns() {

        val schema = SchemaExtractor.extract(connection)


        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "ObjectIdentifierTypes",
                setOf(
                        "dummyColumn",
                        "oidColumn",
                        "regclassColumn",
                        "regcollationColumn",
                        "regconfigColumn",
                        "regdictionaryColumn",
                        "regnamespaceColumn",
                        "regoperColumn",
                        "regoperatorColumn",
                        "regprocColumn",
                        "regprocedureColumn",
                        "regroleColumn",
                        "regtypeColumn"
                )
        )

        val genes = actions[0].seeTopGenes()

        assertEquals(13, genes.size)
        assertTrue(genes[0] is IntegerGene)
        assertTrue(genes[1] is IntegerGene)
        assertTrue(genes[2] is IntegerGene)
        assertTrue(genes[3] is IntegerGene)
        assertTrue(genes[4] is IntegerGene)
        assertTrue(genes[5] is IntegerGene)
        assertTrue(genes[6] is IntegerGene)
        assertTrue(genes[7] is IntegerGene)
        assertTrue(genes[8] is IntegerGene)
        assertTrue(genes[9] is IntegerGene)
        assertTrue(genes[10] is IntegerGene)
        assertTrue(genes[11] is IntegerGene)
        assertTrue(genes[12] is IntegerGene)

    }

    @Test
    fun testInsertionOfGenesOfPgLSN() {

        val schema = SchemaExtractor.extract(connection)


        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction(
                "ObjectIdentifierTypes",
                setOf(
                        "dummyColumn",
                        "oidColumn",
                        "regclassColumn",
                        "regcollationColumn",
                        "regconfigColumn",
                        "regdictionaryColumn",
                        "regnamespaceColumn",
                        "regoperColumn",
                        "regoperatorColumn",
                        "regprocColumn",
                        "regprocedureColumn",
                        "regroleColumn",
                        "regtypeColumn"
                )
        )

        val dbCommandDto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dbCommandDto.insertions)
    }


}