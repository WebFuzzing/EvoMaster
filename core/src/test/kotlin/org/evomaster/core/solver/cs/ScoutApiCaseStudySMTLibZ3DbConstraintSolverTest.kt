package org.evomaster.core.solver.cs

import net.sf.jsqlparser.JSQLParserException
import org.apache.commons.io.FileUtils
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
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
 * copied from https://github.com/WebFuzzing/EMB/tree/master/jdk_8_maven/em/external/rest/scout-api
 *
 */
class ScoutApiCaseStudySMTLibZ3DbConstraintSolverTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var resourcesFolder: String

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:constraint_test", "sa", "")

            val resourcesFolder =
                System.getProperty("user.dir") + "/src/test/resources/solver/case-study-migrations/scout-api/"

            val migration1 = File(resourcesFolder + "changeset1.sql").readText()
            SqlScriptRunner.execCommand(connection, migration1)
            val migration2 = File(resourcesFolder + "changeset2.sql").readText()
            SqlScriptRunner.execCommand(connection, migration2)
            val migration3 = File(resourcesFolder + "changeset3.sql").readText()
            SqlScriptRunner.execCommand(connection, migration3)
            val migration4 = File(resourcesFolder + "changeset4.sql").readText()
            SqlScriptRunner.execCommand(connection, migration4)
            val schemaDto = SchemaExtractor.extract(connection)

            Companion.resourcesFolder = tmpResourcesFolder()
            createFolder(Companion.resourcesFolder)
            solver = SMTLibZ3DbConstraintSolver(schemaDto, Companion.resourcesFolder, 1)
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
            "SELECT id, key, value, valid_from, valid_to\n" +
                    "FROM system_message;\n"
        )

        val systemMessage1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in systemMessage1) {
            when (gene.name) {
                "ID" -> {
                    assertTrue(gene is IntegerGene)
                }
                "KEY" -> {
                    assertTrue(gene is StringGene)
                }
                "VALUE" -> {
                    assertTrue(gene is StringGene)
                }

                "VALID_FROM" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using ints
                    assertTrue(gene is LongGene)
                    val snapshotDate = (gene as LongGene).value
                    assertTrue(snapshotDate >= 0)
                    assertTrue(snapshotDate <= 32503680000)
                }

                "VALID_TO" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using ints
                    assertTrue(gene is LongGene)
                    val snapshotDate = (gene as LongGene).value
                    assertTrue(snapshotDate >= 0)
                    assertTrue(snapshotDate <= 32503680000)
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test2() {

        val newActions = solver.solve(
            "SELECT activity_id, ratings_avg\n" +
                    "FROM activity_derived\n" +
                    "WHERE ratings_avg IS NOT NULL;\n"
        )

        // TODO: Implement Support for Views (activity_derived is a view, not a table)
        assertEquals(0, newActions.size)
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test3() {

        val newActions = solver.solve(
            "SELECT u.name AS user_name, a.id AS activity_id, ar.rating, ar.favourite\n" +
                    "FROM users u\n" +
                    "JOIN activity_rating ar ON u.id = ar.user_id\n" +
                    "JOIN activity a ON ar.activity_id = a.id;\n"
        )

        assertTrue("users".equals(newActions[1].table.name, ignoreCase = true))
        val users1 = newActions[1].seeTopGenes()
        val users1Id = users1.find { it.name == "ID" } as IntegerGene
        for (gene in users1) {
            when (gene.name) {
                "ID" -> {
                    assertTrue(gene is IntegerGene)
                }
                "NAME" -> {
                    assertTrue(gene is StringGene)
                }
            }
        }
        assertTrue("activity_rating".equals(newActions[2].table.name, ignoreCase = true))
        val activityRating1 = newActions[2].seeTopGenes()
        val activityRating1ActivityId = activityRating1.find { it.name == "ACTIVITY_ID" } as IntegerGene
        for (gene in activityRating1) {
            when (gene.name) {
                "USER_ID" -> {
                    assertTrue(gene is IntegerGene)
                    assertEquals(users1Id.value, (gene as IntegerGene).value)
                }
                "ACTIVITY_ID" -> {
                    assertTrue(gene is IntegerGene)
                }
                "RATING" -> {
                    assertTrue(gene is IntegerGene)
                }
                "FAVOURITE" -> {
                    assertTrue(gene is BooleanGene)
                }
            }
        }
        assertTrue("activity".equals(newActions[0].table.name, ignoreCase = true))
        val activity1: List<Gene> = newActions[0].seeTopGenes()
        for (gene in activity1) {
            when (gene.name) {
                "ID" -> {
                    assertTrue(gene is IntegerGene)
                    assertEquals(activityRating1ActivityId.value, (gene as IntegerGene).value)
                }
            }
        }
    }
}