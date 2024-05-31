package org.evomaster.core.solver

import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto
import org.evomaster.core.sql.SqlAction
import org.evomaster.solver.smtlib.SMTLib
import org.evomaster.solver.Z3DockerExecutor
import org.slf4j.LoggerFactory
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

/**
 * A smt2 solver implementation using Z3 in a Docker container.
 * It generates the SMT problem from the database schema and the query
 * and then executes Z3 to get values and returns the necessary list of SqlActions
 * to satisfy the query.
 */
class SMTLibZ3DbConstraintSolver(private val schemaDto: DbSchemaDto) : DbConstraintSolver {

    private val resourcesFolder: String = tmpResourcesFolder()
    private val generator: SmtLibGenerator = SmtLibGenerator(schemaDto)
    private val executor: Z3DockerExecutor = Z3DockerExecutor(resourcesFolder)

    init {
        createTmpFolder(resourcesFolder)
    }

    private fun tmpResourcesFolder(): String {
        val instant = Instant.now().epochSecond.toString()
        val tmpFolderPath = "/tmp/$instant/"
        return System.getProperty("user.dir") + tmpFolderPath
    }

    private fun createTmpFolder(resourcesFolderWithTmpDir: String) {
        try {
            Files.createDirectories(Paths.get(resourcesFolderWithTmpDir))
        } catch (e: IOException) {
            throw RuntimeException("Error creating tmp folder '$resourcesFolderWithTmpDir'. ", e)
        }
    }

    /**
     * Deletes the tmp folder with all its content and then stops the Z3 Docker container.
     */
    override fun close() {
        try {
            FileUtils.deleteDirectory(File(this.resourcesFolder))
        } catch (e: IOException) {
            log.error("Error deleting tmp folder '${this.resourcesFolder}'. ", e)
        }
        executor.close()
    }

    /**
     * From the database schema and the query, it generates the SMT problem to make the query return a value
     * Then executes Z3 to get values and returns the necessary list of SqlActions
     * @param sqlQuery the SQL query to solve
     * @return a list of SQL actions that can be executed to satisfy the query
     */
    override fun solve(sqlQuery: String): List<SqlAction> {
        val queryStatement = parseStatement(sqlQuery)
        val smtLib = this.generator.generateSMT(queryStatement)
        val fileName = storeToTmpFile(smtLib)
        val z3Response = executor.solveFromFile(fileName)

        return toSqlActionList(z3Response)
    }

    private fun parseStatement(sqlQuery: String): Statement {
        return try {
            CCJSqlParserUtil.parse(sqlQuery)
        } catch (e: JSQLParserException) {
            throw RuntimeException(e)
        }
    }

    private fun toSqlActionList(z3Response: String): List<SqlAction> {
        return emptyList()
    }

    private fun storeToTmpFile(smtLib: SMTLib): String {
        val fileName = "smt2_" + System.currentTimeMillis() + ".smt2"
        val filePath = this.resourcesFolder + fileName

        try {
            Files.write(Paths.get(filePath), smtLib.toString().toByteArray(StandardCharsets.UTF_8))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return filePath
    }

    companion object {
        private val log = LoggerFactory.getLogger(SMTLibZ3DbConstraintSolver::class.java.name)
    }
}
