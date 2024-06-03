package org.evomaster.core.solver

import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
import org.evomaster.core.sql.schema.Table
import org.evomaster.solver.Z3DockerExecutor
import org.evomaster.solver.smtlib.SMTLib
import org.evomaster.solver.smtlib.value.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * A smt2 solver implementation using Z3 in a Docker container.
 * It generates the SMT problem from the database schema and the query
 * and then executes Z3 to get values and returns the necessary list of SqlActions
 * to satisfy the query.
 */
class SMTLibZ3DbConstraintSolver(private val schemaDto: DbSchemaDto, private val resourcesFolder: String, private val numberOfRows: Int = 2) : DbConstraintSolver {

    private val generator: SmtLibGenerator = SmtLibGenerator(schemaDto, numberOfRows)
    private val executor: Z3DockerExecutor = Z3DockerExecutor(resourcesFolder)

    /**
     * Deletes the tmp folder with all its content and then stops the Z3 Docker container.
     */
    override fun close() {
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

    private fun toSqlActionList(z3Response: Optional<MutableMap<String, SMTLibValue>>): List<SqlAction> {

        if (!z3Response.isPresent) {
            return emptyList()
        }

        val actions = mutableListOf<SqlAction>()

        for (row in z3Response.get()) {
            val tableName = getTableName(row.key)
            val columns = row.value as StructValue

            var table = findTableByName(schemaDto, tableName)

            // Create the list of genes with the values
            var genes = mutableListOf<Gene>()
            for (columnName in columns.getFields()) {
                val columnValue = columns.getField(columnName)
                when (columnValue) {
                    is StringValue -> {
                        val gene = StringGene(columnName, columnValue.getValue())
                        genes.add(gene)
                    }
                    is IntValue -> {
                        val gene = IntegerGene(columnName, columnValue.getValue())
                        genes.add(gene)
                    }
                    is RealValue -> {
                        val gene = DoubleGene(columnName, columnValue.getValue())
                        genes.add(gene)
                    }
                }
            }

            val sqlAction = SqlAction(table, table.columns, -1, genes.toList())
            actions.add(sqlAction)
        }

        return actions
    }

    /*
        Remove the suffix that is the index of the variable.
        As the variable name is the table name, we can get the table name by removing the last character,
        And the last character is the index of the variable which is 1 or 2
    */
    private fun getTableName(key: String): String {
        return key.substring(0, key.length - 1)
    }

    private fun findTableByName(schema: DbSchemaDto, tableName: String): Table {
        val tableDto = schema.tables.find { it.name.equals(tableName, ignoreCase = true) }
            ?: throw RuntimeException("Table not found: $tableName")
        val name = tableDto.name
        val columns = findColumns(tableDto)
        val foreignKeys = findForeignKeys(tableDto)
        return Table(name, columns, foreignKeys) // TODO: Calculate constraints
    }

    private fun findColumns(tableDto: TableDto): Set<Column> {
        val columnsDto = tableDto.columns
        val columns = mutableSetOf<Column>()
        val databaseType = schemaDto.databaseType
        for (columnDto in columnsDto) {
            columns.add(toColumnFromDto(columnDto, databaseType))
        }
        return columns
    }

    private fun toColumnFromDto(
        columnDto: ColumnDto,
        databaseType: DatabaseType
    ): Column {
        val name = columnDto.name
        val type = getColumnDataType(columnDto.type)
        val nullable: Boolean = columnDto.nullable
        val primaryKey = columnDto.primaryKey
        val unique = columnDto.unique
        val autoIncrement = columnDto.autoIncrement
        val foreignKeyToAutoIncrement = columnDto.foreignKeyToAutoIncrement
        val size = columnDto.size
        val lowerBound = null
        val upperBound = null
        val enumValuesAsStrings = null
        val similarToPatterns = null
        val likePatterns = null
        val isUnsigned = false
        val compositeType = null
        val compositeTypeName = 0
        val isNotBlank = null
        val minSize = null
        val maxSize = null
        val javaRegExPattern = null

        val column = Column(
            name,
            type,
            size,
            primaryKey,
            nullable,
            unique,
            autoIncrement,
            foreignKeyToAutoIncrement,
            lowerBound,
            upperBound,
            enumValuesAsStrings,
            similarToPatterns,
            likePatterns,
            databaseType,
            isUnsigned,
            compositeType,
            compositeTypeName,
            isNotBlank,
            minSize,
            maxSize,
            javaRegExPattern
        )
        return column
    }

    private fun getColumnDataType(type: String): ColumnDataType {
        return when (type) {
            "BIGINT" -> ColumnDataType.BIGINT
            "INTEGER" -> ColumnDataType.INTEGER
            "FLOAT" -> ColumnDataType.FLOAT
            "DOUBLE" -> ColumnDataType.DOUBLE
            "CHARACTER VARYING" -> ColumnDataType.CHARACTER_VARYING
            "CHAR" -> ColumnDataType.CHAR
            else -> ColumnDataType.CHARACTER_VARYING
        }
    }

    // TODO: Implement
    private fun findForeignKeys(tableDto: TableDto): Set<ForeignKey> {
        return emptySet()
    }

    /**
     * Stores the SMTLib problem to a file in the tmp folder
     * @param smtLib the SMTLib problem
     * @return the file name as it's the one needed for the Z3 Docker to run
     */
    private fun storeToTmpFile(smtLib: SMTLib): String {
        val fileName = "smt2_" + System.currentTimeMillis() + ".smt2"
        val filePath = this.resourcesFolder + fileName

        try {
            Files.write(Paths.get(filePath), smtLib.toString().toByteArray(StandardCharsets.UTF_8))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return fileName
    }

    companion object {
        private val log = LoggerFactory.getLogger(SMTLibZ3DbConstraintSolver::class.java.name)
    }
}
