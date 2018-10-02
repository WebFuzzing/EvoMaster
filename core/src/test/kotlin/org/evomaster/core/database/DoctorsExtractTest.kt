package org.evomaster.core.database

import org.evomaster.clientJava.controller.db.SqlScriptRunner
import org.evomaster.clientJava.controller.internal.db.SchemaExtractor
import org.evomaster.clientJava.controllerApi.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.SqlAutoIncrementGene
import org.evomaster.core.search.gene.SqlForeignKeyGene
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.sql.Connection
import java.sql.DriverManager

class DoctorsExtractTest {


    companion object {

        private lateinit var connection: Connection

        private val sqlSchema = this::class.java.getResource("/sql_schema/doctors.sql").readText()

        @BeforeAll
        @JvmStatic
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
    fun testIssueWithFK() {

        SqlScriptRunner.execCommand(connection, sqlSchema)

        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)

        val actions = builder.createSqlInsertionAction("DOCTORS", setOf("PERSON_ID"))

        val all = actions.flatMap { it.seeGenes() }.flatMap { it.flatView() }

        //force binding
        val randomness = Randomness()//.apply { updateSeed(1) }
        DbAction.randomizeDbActionGenes(actions, randomness)

        val dto = DbActionTransformer.transform(actions)


        assertEquals(actions.size, dto.insertions.size)
    }


}