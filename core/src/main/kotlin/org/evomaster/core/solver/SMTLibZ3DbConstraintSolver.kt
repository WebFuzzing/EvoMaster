package org.evomaster.core.solver

import com.google.inject.Inject
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import org.apache.commons.io.FileUtils
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.EMConfig
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Column
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
import org.evomaster.core.sql.schema.Table
import org.evomaster.solver.Z3DockerExecutor
import org.evomaster.solver.smtlib.SMTLib
import org.evomaster.solver.smtlib.value.*
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * An SMT solver implementation using Z3 in a Docker container.
 * It generates the SMT problem from the database schema and the SQL query,
 * then executes Z3 to get values and returns the necessary list of SqlActions
 * to satisfy the query.
 */
class SMTLibZ3DbConstraintSolver() : DbConstraintSolver {

    // Create a temporary directory for tests
    var resourcesFolder = Files.createTempDirectory("tmp").toString()

    private lateinit var executor: Z3DockerExecutor
    private var idCounter: Long = 0L

    @Inject
    private lateinit var config: EMConfig

    @PostConstruct
    private fun postConstruct() {
        if (config.generateSqlDataWithDSE) {
            initializeExecutor()
        }
    }

    fun initializeExecutor() {
        executor = Z3DockerExecutor(resourcesFolder)
    }

    /**
     * Closes the Z3 Docker executor and cleans up temporary files.
     */
    @PreDestroy
    override fun close() {
        executor.close()
        try {
            FileUtils.cleanDirectory(File(resourcesFolder))
        } catch (e: IOException) {
            throw RuntimeException("Error cleaning up resources folder", e)
        }
    }

    /**
     * Generates the SMT problem from the SQL query, solves it using Z3,
     * and returns a list of SqlActions that satisfy the query.
     *
     * @param sqlQuery The SQL query to solve.
     * @return A list of SQL actions that can be executed to satisfy the query.
     */
    override fun solve(schemaDto: DbInfoDto, sqlQuery: String, numberOfRows: Int): List<SqlAction> {
        // TODO: Use memoized, if it's the same schema and query, return the same result and don't do any calculation

        val generator = SmtLibGenerator(schemaDto, numberOfRows)
        val queryStatement = parseStatement(sqlQuery)
        val smtLib = generator.generateSMT(queryStatement)
        val fileName = storeToTmpFile(smtLib)
        val z3Response = executor.solveFromFile(fileName)

        return toSqlActionList(schemaDto, z3Response)
    }

    /**
     * Parses the SQL query into a JSQLParser Statement.
     *
     * @param sqlQuery The SQL query string.
     * @return The parsed SQL statement.
     */
    private fun parseStatement(sqlQuery: String): Statement {
        return try {
            CCJSqlParserUtil.parse(sqlQuery)
        } catch (e: JSQLParserException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Converts Z3's response to a list of SqlActions.
     *
     * @param z3Response The response from Z3.
     * @return A list of SQL actions.
     */
    private fun toSqlActionList(schemaDto: DbInfoDto, z3Response: Optional<MutableMap<String, SMTLibValue>>): List<SqlAction> {
        if (!z3Response.isPresent) {
            return emptyList()
        }

        val actions = mutableListOf<SqlAction>()

        for (row in z3Response.get()) {
            val tableName = getTableName(row.key)
            val columns = row.value as StructValue

            // Find table from schema and create SQL actions
            val table = findTableByName(schemaDto, tableName)

            // Create the list of genes with the values
            val genes = mutableListOf<Gene>()
            for (columnName in columns.fields) {
                var gene: Gene = IntegerGene(columnName, 0)
                when (val columnValue = columns.getField(columnName)) {
                    is StringValue -> {
                        gene = if (isBoolean(schemaDto, table, columnName)) {
                            BooleanGene(columnName, toBoolean(columnValue.value))
                        } else {
                            StringGene(columnName, columnValue.value)
                        }
                    }
                    is LongValue -> {
                        gene = if (isTimestamp(table, columnName)) {
                            LongGene(columnName, columnValue.value.toLong())
                        } else {
                            IntegerGene(columnName, columnValue.value.toInt())
                        }
                    }
                    is RealValue -> {
                        gene = DoubleGene(columnName, columnValue.value)
                    }
                }
                val currentColumn = table.columns.firstOrNull(){ it.name == columnName}
                if (currentColumn != null &&  currentColumn.primaryKey) {
                    gene = SqlPrimaryKeyGene(columnName, tableName, gene, idCounter)
                    idCounter++
                }
                gene.markAllAsInitialized()
                genes.add(gene)
            }

            val sqlAction = SqlAction(table, table.columns, idCounter, genes.toList())
            idCounter++
            actions.add(sqlAction)
        }

        return actions
    }

    private fun toBoolean(value: String?): Boolean {
        return value.equals("True", ignoreCase = true)
    }

    private fun isBoolean(schemaDto: DbInfoDto, table: Table, columnName: String?): Boolean {
        val col = schemaDto.tables.first { it.name == table.name }.columns.first { it.name == columnName }
        return col.type == "BOOLEAN"
    }

    private fun isTimestamp(table: Table, columnName: String?): Boolean {
        val col = table.columns.first { it.name == columnName }
        return col.type == ColumnDataType.TIMESTAMP
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
    private fun findTableByName(schema: DbInfoDto, tableName: String): Table {
        val tableDto = schema.tables.find { it.name.equals(tableName, ignoreCase = true) }
            ?: throw RuntimeException("Table not found: $tableName")
        return Table(
            tableDto.name,
            findColumns(schema, tableDto), // Convert columns from DTO
            findForeignKeys(tableDto) // TODO: Implement this method
        )
    }

    /**
     * Converts a list of ColumnDto to a set of Column objects.
     *
     * @param tableDto The table DTO containing column definitions.
     * @return A set of Column objects.
     */
    private fun findColumns(schemaDto: DbInfoDto, tableDto: TableDto): Set<Column> {
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
            "TIMESTAMP" -> ColumnDataType.TIMESTAMP
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
     * Stores the SMTLib problem to a file in the resources' folder.
     *
     * @param smtLib The SMTLib problem.
     * @return The filename of the stored SMTLib problem.
     */
    private fun storeToTmpFile(smtLib: SMTLib): String {
        val directoryPath = leadingBarResourcesFolder()
        val fileNameBase = "smt2_${System.currentTimeMillis()}"
        val fileExtension = ".smt2"

        try {
            // Create dir if it doesn't exist
            val directory = Paths.get(directoryPath)
            if (!directory.exists()) {
                directory.createDirectories()
            }

            // Generate a unique file name
            var fileName = "$fileNameBase$fileExtension"
            var filePath = directory.resolve(fileName)
            if (filePath.exists()) {
                // Add a random suffix to the file name if it already exists
                val randomSuffix = (1000..9999).random()
                fileName = "${fileNameBase}_$randomSuffix$fileExtension"
                filePath = directory.resolve(fileName)
            }

            // Write the SMTLib content to the file
            Files.write(filePath, smtLib.toString().toByteArray(StandardCharsets.UTF_8))

            return fileName
        } catch (e: IOException) {
            throw RuntimeException("Failed to write SMTLib to file: ${e.message}")
        }
    }

    private fun leadingBarResourcesFolder() = if (resourcesFolder.endsWith("/")) resourcesFolder else "$resourcesFolder/"
}
