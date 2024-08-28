package org.evomaster.core.solver

import net.sf.jsqlparser.JSQLParserException
import org.apache.commons.io.FileUtils
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
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
 * copied from https://github.com/WebFuzzing/EMB/tree/master/jdk_8_maven/cs/rest/original/catwatch
 *
 */
class CatWatchCaseStudySMTLibZ3DbConstraintSolverTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var resourcesFolder: String

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:constraint_test", "sa", "")

            val resourcesFolder =
                System.getProperty("user.dir") + "/src/test/resources/solver/case-study-migrations/catwatch/"

            val migration1 = File(resourcesFolder + "V001__base_version.sql").readText()
            SqlScriptRunner.execCommand(connection, migration1)
            val migration2 = File(resourcesFolder + "V002__test_baseline_on_migrate.sql").readText()
            SqlScriptRunner.execCommand(connection, migration2)
            val migration3 = File(resourcesFolder + "V003__change_varchar_to_text.sql").readText()
            SqlScriptRunner.execCommand(connection, migration3)
            val migration4 = File(resourcesFolder + "V004__maintainers.sql").readText()
            SqlScriptRunner.execCommand(connection, migration4)
            val migration5 = File(resourcesFolder + "V005__catwatch_yaml.sql").readText()
            SqlScriptRunner.execCommand(connection, migration5)
            val migration6 = File(resourcesFolder + "V006__add_external_contributors.sql").readText()
            SqlScriptRunner.execCommand(connection, migration6)
            val schemaDto = SchemaExtractor.extract(connection)

            this.resourcesFolder = tmpResourcesFolder()
            createFolder(this.resourcesFolder)
            solver = SMTLibZ3DbConstraintSolver(schemaDto, this.resourcesFolder)
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
            "select p from Project p where p.snapshot_date between 1 and 200 order by p.snapshot_date desc;"
        )
        val project1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in project1) {
            when (gene.name) {
                "SNAPSHOT_DATE" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using IntegerGene
                    assertTrue(gene is LongGene)
                    val snapshotValue = (gene as LongGene).value
                    assertTrue(snapshotValue >= 1)
                    assertTrue(snapshotValue <= 200)
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test2() {

        val newActions = solver.solve(
            "select p from Project p where p.organization_name in ('name1', 'name2', 'name3', 'name4') and" +
                    " p.snapshot_date between 46546 and 546546 order by p.snapshot_date desc;"
        )
        val project1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in project1) {
            when (gene.name) {
                "SNAPSHOT_DATE" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using ints
                    assertTrue(gene is LongGene)
                    val snapshotDate = (gene as LongGene).value
                    assertTrue(snapshotDate >= 46546)
                    assertTrue(snapshotDate <= 546546)
                }

                "ORGANIZATION_NAME" -> {
                    assertTrue(gene is StringGene)
                    val organizationName = (gene as StringGene).value
                    assertTrue(arrayOf("name1", "name2", "name3", "name4").contains(organizationName))
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test3() {

        val newActions = solver.solve("select p from Project p where p.snapshot_date in (1, 2, 3, 4);")
        val project1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in project1) {
            when (gene.name) {
                "SNAPSHOT_DATE" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using ints
                    assertTrue(gene is LongGene)
                    val snapshotDate = (gene as LongGene).value
                    assertTrue(longArrayOf(1, 2, 3, 4).contains(snapshotDate))
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test4() {
        val newActions = solver.solve(
            "select c from Contributor c where c.key.snapshot_date between 13543543354 and 13543549999 " +
                    "and c.organization_name in ('name1', 'name2', 'name3', 'name4') order by c.key.snapshot_date"
        )
        val contributor1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in contributor1) {
            when (gene.name) {
                "SNAPSHOT_DATE" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using ints
                    assertTrue(gene is LongGene)
                    val snapshotDate = (gene as LongGene).value
                    assertTrue(snapshotDate >= 13543543354)
                    assertTrue(snapshotDate <= 13543549999)
                }

                "ORGANIZATION_NAME" -> {
                    assertTrue(gene is StringGene)
                    val organizationName = (gene as StringGene).value
                    assertTrue(arrayOf("name1", "name2", "name3", "name4").contains(organizationName))
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test5() {

        val newActions =
            solver.solve("select c.key.organization_id from Contributor c where c.organization_name = 'Tina'")
        val contributor1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in contributor1) {
            when (gene.name) {
                "ORGANIZATION_NAME" -> {
                    assertTrue(gene is StringGene)
                    val organizationName = (gene as StringGene).value
                    assertEquals("Tina", organizationName)
                }
            }
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun test6() {

        val newActions =
            solver.solve("select c.key.snapshot_date from Contributor c where c.key.snapshot_date <= 1000 order by c.key.snapshot_date desc")
        val contributor1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in contributor1) {
            when (gene.name) {
                "SNAPSHOT_DATE" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using ints
                    assertTrue(gene is LongGene)
                    val snapshotDate = (gene as LongGene).value
                    assertTrue(snapshotDate >= 0)
                    assertTrue(snapshotDate <= 1000)
                }
            }
        }
    }


    @Test
    @Throws(JSQLParserException::class)
    fun test7() {

        val newActions =
            solver.solve("select s from Statistics s where s.organization_name = 'Agus' and s.key.snapshot_date between 15820 and 9999999999 order by s.key.snapshot_date desc")
        val statistics1: List<Gene> = newActions[0].seeTopGenes()

        for (gene in statistics1) {
            when (gene.name) {
                "SNAPSHOT_DATE" -> {
                    // TODO: Add support for Timestamps Genes, now it's a best effort using ints
                    assertTrue(gene is LongGene)
                    val snapshotDate = (gene as LongGene).value
                    assertTrue(snapshotDate >= 15820)
                    assertTrue(snapshotDate <= 9999999999)
                }

                "ORGANIZATION_NAME" -> {
                    assertTrue(gene is StringGene)
                    val organizationName = (gene as StringGene).value
                    assertEquals("Agus", organizationName)
                }
            }
        }
    }
}