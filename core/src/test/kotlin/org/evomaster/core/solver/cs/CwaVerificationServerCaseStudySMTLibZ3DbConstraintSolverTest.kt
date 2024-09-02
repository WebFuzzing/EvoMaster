package org.evomaster.core.solver.cs

import net.sf.jsqlparser.JSQLParserException
import org.apache.commons.io.FileUtils
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.solver.SMTLibZ3DbConstraintSolver
import org.evomaster.solver.smtlib.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Instant


/**
 * Case Study Database Schema
 * copied from https://github.com/WebFuzzing/EMB/tree/master/jdk_11_maven/cs/rest/cwa-verification-server
 *
 */
class CwaVerificationServerCaseStudySMTLibZ3DbConstraintSolverTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var resourcesFolder: String

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:constraint_test", "sa", "")

            val resourcesFolder =
                System.getProperty("user.dir") + "/src/test/resources/solver/case-study-migrations/cwa-verification-server/"

            val migration1 = File(resourcesFolder + "v000-create-app-session-table.sql").readText()
            SqlScriptRunner.execCommand(connection, migration1)
            val migration2 = File(resourcesFolder + "v000-create-tan-table.sql").readText()
            SqlScriptRunner.execCommand(connection, migration2)
            val migration3 = File(resourcesFolder + "v001-add-dob-hash-column.sql").readText()
            SqlScriptRunner.execCommand(connection, migration3)
            val migration4 = File(resourcesFolder + "v002-add-teletan-type-column.sql").readText()
            SqlScriptRunner.execCommand(connection, migration4)
            val schemaDto = SchemaExtractor.extract(connection)

            Companion.resourcesFolder = tmpResourcesFolder()
            createFolder(Companion.resourcesFolder)
            solver = SMTLibZ3DbConstraintSolver(schemaDto, Companion.resourcesFolder)
        }

        @JvmStatic
        @AfterAll
        @Throws(SQLException::class)
        fun closeConnection() {
            try {
                connection.close()
                if (this::solver.isInitialized) {
                    solver.close()
                }
            } catch (error: Error) {
                println(error)
            } finally {
                removeFolder(resourcesFolder)
            }
        }

        private fun tmpResourcesFolder(): String {
            val instant = Instant.now().epochSecond.toString()
            val tmpFolderPath = "tmp-solver$instant/"
            return System.getProperty("user.dir") + "/src/test/resources/" + tmpFolderPath
        }

        private fun createFolder(folderPath: String) {
            try {
                Files.createDirectories(Paths.get(folderPath))
            } catch (e: IOException) {
                throw RuntimeException("Error creating tmp folder '$folderPath'. ", e)
            }
        }

        private fun removeFolder(folderPath: String) {
            try {
                FileUtils.deleteDirectory(File(folderPath))
            } catch (e: IOException) {
                throw RuntimeException("Error deleting tmp folder '$folderPath'. ", e)
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test1() {

        val newActions = solver.solve(
            "SELECT * FROM app_session WHERE hashed_guid = '1234567890abcdef' AND sot = 'ghijklmnopqrstuv'"
        )
        val appSession1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in appSession1) {
            when (gene.name) {
                "HASHED_GUID" -> {
                    assertTrue(gene is StringGene)
                    val dobHash = (gene as StringGene).value
                    assertEquals("1234567890abcdef", dobHash)
                }
                "SOT" -> {
                    assertTrue(gene is StringGene)
                    val teletanType = (gene as StringGene).value
                    assertEquals("ghijklmnopqrstuv", teletanType)
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test2() {

        val newActions = solver.solve(
            "SELECT * FROM app_session WHERE hashed_guid_dob = '1234567890abcdef' AND teletan_type = 'ghijklmnopqrstuv'"
        )
        val appSession1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in appSession1) {
            when (gene.name) {
                "HASHED_GUID_DOB" -> {
                    assertTrue(gene is StringGene)
                    val dobHash = (gene as StringGene).value
                    assertEquals("1234567890abcdef", dobHash)
                }
                "TELETAN_TYPE" -> {
                    assertTrue(gene is StringGene)
                    val teletanType = (gene as StringGene).value
                    assertEquals("ghijklmnopqrstuv", teletanType)
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test3() {

        val newActions = solver.solve(
            "SELECT * FROM tan WHERE redeemed = TRUE"
        )
        val tan1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in tan1) {
            when (gene.name) {
                "REDEEMED" -> {
                    assertTrue(gene is BooleanGene)
                    val redeemed = (gene as BooleanGene).value
                    assertTrue(redeemed)
                }
            }
        }
    }


}