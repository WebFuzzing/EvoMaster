package org.evomaster.core.database

import org.evomaster.clientJava.controller.db.SqlScriptRunner
import org.evomaster.clientJava.controller.internal.db.SchemaExtractor
import org.evomaster.core.search.gene.IntegerGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

class SqlInsertBuilderTest{


    companion object {

        private lateinit var connection: Connection

        @BeforeAll @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")
        }
    }

    @BeforeEach
    fun initTest() {

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
    }


    @Test
    fun testSimpleInt() {

        SqlScriptRunner.execCommand(connection, "CREATE TABLE Foo(x INT);")

        val dto = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(dto)

        val actions = builder.createSqlInsertionAction("FOO", setOf("X"))

        assertEquals(1, actions.size)

        val genes = actions[0].seeGenes()

        assertEquals(1, genes.size)
        assertTrue(genes[0] is IntegerGene)
    }
}