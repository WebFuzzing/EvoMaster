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
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * An SMT solver implementation using Z3 in a Docker container.
 * It generates the SMT problem from the database schema and the SQL query,
 * then executes Z3 to get values and returns the necessary list of SqlActions
 * to satisfy the query.
 */
class SMTLibZ3DbConstraintSolver(
    private val schemaDto: DbSchemaDto, // Database schema
    private val resourcesFolder: String, // Folder for temporary resources
    numberOfRows: Int = 2 // Number of rows to generate
) : DbConstraintSolver {

    private val generator: SmtLibGenerator = SmtLibGenerator(schemaDto, numberOfRows)
    private val executor: Z3DockerExecutor = Z3DockerExecutor(resourcesFolder)

    /**
     * Closes the Z3 Docker executor and cleans up temporary files.
     */
    override fun close() {
        executor.close()
    }

    /**
     * Generates the SMT problem from the SQL query, solves it using Z3,
     * and returns a list of SqlActions that satisfy the query.
     *
     * @param sqlQuery The SQL query to solve.
     * @return A list of SQL actions that can be executed to satisfy the query.
     */
    override fun solve(sqlQuery: String): List<SqlAction> {
        val queryStatement = parseStatement(sqlQuery) // Parse SQL query
        val smtLib = this.generator.generateSMT(queryStatement) // Generate SMTLib problem
        val fileName = storeToTmpFile(smtLib) // Store SMTLib to a temporary file
        val z3Response = executor.solveFromFile(fileName) // Solve using Z3

        return toSqlActionList(z3Response) // Convert Z3 response to SQL actions
    }

    /**
     * Parses the SQL query into a JSQLParser Statement.
     *
     * @param sqlQuery The SQL query string.
     * @return The parsed SQL statement.
     */
    private fun parseStatement(sqlQuery: String): Statement {
        return try {
            CCJSqlParserUtil.parse(sqlQuery) // Parse query using JSQLParser
        } catch (e: JSQLParserException) {
            throw RuntimeException(e) // Rethrow exception if parsing fails
        }
    }

    /**
     * Converts Z3's response to a list of SqlActions.
     *
     * @param z3Response The response from Z3.
     * @return A list of SQL actions.
     */
    private fun toSqlActionList(z3Response: Optional<MutableMap<String, SMTLibValue>>): List<SqlAction> {
        if (!z3Response.isPresent) {
            return emptyList() // Return empty list if no response
        }

        val actions = mutableListOf<SqlAction>()

        for (row in z3Response.get()) {
            val tableName = getTableName(row.key) // Extract table name from key
            val columns = row.value as StructValue

            // Find table from schema and create SQL actions
            val table = findTableByName(schemaDto, tableName)

            // Create the list of genes with the values
            val genes = mutableListOf<Gene>()
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

    /**
     * Extracts the table name from the key by removing the last character (index).
     *
     * @param key The key containing the table name and index.
     * @return The extracted table name.
     */
    private fun getTableName(key: String): String {
        return key.substring(0, key.length - 1) // Remove last character
    }

    /**
     * Finds a table by its name from the schema and constructs a Table object.
     *
     * @param schema The database schema.
     * @param tableName The name of the table to find.
     * @return The Table object.
     */
    private fun findTableByName(schema: DbSchemaDto, tableName: String): Table {
        val tableDto = schema.tables.find { it.name.equals(tableName, ignoreCase = true) }
            ?: throw RuntimeException("Table not found: $tableName")
        return Table(
            tableDto.name,
            findColumns(tableDto), // Convert columns from DTO
            findForeignKeys(tableDto) // TODO: Implement this method
        )
    }

    /**
     * Converts a list of ColumnDto to a set of Column objects.
     *
     * @param tableDto The table DTO containing column definitions.
     * @return A set of Column objects.
     */
    private fun findColumns(tableDto: TableDto): Set<Column> {
        return tableDto.columns.map { columnDto ->
            toColumnFromDto(columnDto, schemaDto.databaseType)
        }.toSet()
    }

    /**
     * Converts ColumnDto to a Column object.
     *
     * @param columnDto The column DTO.
     * @param databaseType The type of the database.
     * @return The Column object.
     */
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

    /**
     * Maps column types to ColumnDataType.
     *
     * @param type The column type as a string.
     * @return The corresponding ColumnDataType.
     */
    private fun getColumnDataType(type: String): ColumnDataType {
        return when (type) {
            "BIGINT" -> ColumnDataType.BIGINT
            "INTEGER" -> ColumnDataType.INTEGER
            "FLOAT" -> ColumnDataType.FLOAT
            "DOUBLE" -> ColumnDataType.DOUBLE
            "CHARACTER VARYING" -> ColumnDataType.CHARACTER_VARYING
            "CHAR" -> ColumnDataType.CHAR
            else -> ColumnDataType.CHARACTER_VARYING // Default type
        }
    }

    // TODO: Implement this method
    private fun findForeignKeys(tableDto: TableDto): Set<ForeignKey> {
        return emptySet() // Placeholder
    }

    /**
     * Stores the SMTLib problem to a file in the resources folder.
     *
     * @param smtLib The SMTLib problem.
     * @return The filename of the stored SMTLib problem.
     */
    private fun storeToTmpFile(smtLib: SMTLib): String {
        val fileName = "smt2_${System.currentTimeMillis()}.smt2"
        val filePath = this.resourcesFolder + fileName

        try {
            Files.write(Paths.get(filePath), smtLib.toString().toByteArray(StandardCharsets.UTF_8))
        } catch (e: IOException) {
            throw RuntimeException("Error writing SMTLib to file", e)
        }
        return fileName
    }
}
