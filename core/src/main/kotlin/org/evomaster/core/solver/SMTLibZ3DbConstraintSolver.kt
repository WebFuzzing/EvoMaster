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
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.*
import org.evomaster.core.utils.StringUtils.convertToAscii
import org.evomaster.solver.Z3DockerExecutor
import org.evomaster.solver.smtlib.SMTLib
import org.evomaster.solver.smtlib.value.*
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

        // Convert DTOs to domain objects at the boundary, before any business logic
        val smtTables = Companion.buildSmtTables(schemaDto)

        val generator = SmtLibGenerator(smtTables, schemaDto.databaseType, numberOfRows)
        val queryStatement = parseStatement(sqlQuery)
        val smtLib = generator.generateSMT(queryStatement)
        val fileName = storeToTmpFile(smtLib)
        val z3Response = executor.solveFromFile(fileName)

        return toSqlActionList(smtTables, z3Response)
    }

    companion object {

        /**
         * Converts a [DbInfoDto] schema to a list of [SmtTable] domain objects.
         *
         * This is the boundary where DTOs are translated into domain objects.
         * Exposed as a companion method so tests can build a [SmtLibGenerator] directly
         * without going through the full solver pipeline.
         */
        fun buildSmtTables(schemaDto: DbInfoDto): List<SmtTable> {
            return schemaDto.tables.map { tableDto ->
                val columns = buildColumns(schemaDto.databaseType, tableDto)
                val foreignKeys = buildForeignKeys(tableDto, columns)
                val table = Table(TableId(tableDto.id.name), columns, foreignKeys)
                val checkExpressions = tableDto.tableCheckExpressions.map { it.sqlCheckExpression }
                SmtTable(table, checkExpressions)
            }
        }

        private fun buildColumns(databaseType: DatabaseType, tableDto: TableDto): Set<Column> {
            return tableDto.columns.map { columnDto ->
                toColumn(columnDto, databaseType)
            }.toSet()
        }

        private fun buildForeignKeys(tableDto: TableDto, columns: Set<Column>): Set<ForeignKey> {
            return tableDto.foreignKeys.map { fkDto ->
                val sourceColumns = fkDto.sourceColumns.mapNotNull { colName ->
                    columns.firstOrNull { it.name.equals(colName, ignoreCase = true) }
                }.toSet()
                ForeignKey(sourceColumns, TableId(fkDto.targetTable))
            }.toSet()
        }

        private fun toColumn(columnDto: ColumnDto, databaseType: DatabaseType): Column {
            return Column(
                name = columnDto.name,
                type = getColumnDataType(columnDto.type),
                size = columnDto.size,
                primaryKey = columnDto.primaryKey,
                nullable = columnDto.nullable,
                unique = columnDto.unique,
                autoIncrement = columnDto.autoIncrement,
                foreignKeyToAutoIncrement = columnDto.foreignKeyToAutoIncrement,
                lowerBound = null,
                upperBound = null,
                enumValuesAsStrings = null,
                similarToPatterns = null,
                likePatterns = null,
                databaseType = databaseType,
                isUnsigned = false,
                compositeType = null,
                compositeTypeName = null,
                isNotBlank = null,
                minSize = null,
                maxSize = null,
                javaRegExPattern = null
            )
        }

        /**
         * Maps column type strings (from SQL schema) to [ColumnDataType] enum values.
         */
        private fun getColumnDataType(type: String): ColumnDataType {
            return when (type.uppercase()) {
                "BIGINT" -> ColumnDataType.BIGINT
                "INTEGER", "INT" -> ColumnDataType.INTEGER
                "FLOAT" -> ColumnDataType.FLOAT
                "DOUBLE" -> ColumnDataType.DOUBLE
                "TIMESTAMP" -> ColumnDataType.TIMESTAMP
                "CHARACTER VARYING" -> ColumnDataType.CHARACTER_VARYING
                "CHAR" -> ColumnDataType.CHAR
                else -> ColumnDataType.CHARACTER_VARYING // Default type
            }
        }
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
        } catch (_: JSQLParserException) {
            val sanitizedQuery = removeNotSupportedKeywords(sqlQuery)
            return try {
                CCJSqlParserUtil.parse(sanitizedQuery)
            } catch (e: JSQLParserException) {
                LoggingUtil.getInfoLogger().error("Failed to parse SQL query '$sqlQuery' as SQL Statement")
                throw RuntimeException(e)
            }
        }
    }

    private fun removeNotSupportedKeywords(sqlQuery: String): String {
        return sqlQuery.replace("local temporary", "")
    }

    /**
     * Converts Z3's response to a list of SqlActions.
     *
     * @param smtTables The pre-built domain tables, used to look up schema metadata.
     * @param z3Response The response from Z3.
     * @return A list of SQL actions.
     */
    private fun toSqlActionList(smtTables: List<SmtTable>, z3Response: Optional<MutableMap<String, SMTLibValue>>): List<SqlAction> {
        if (!z3Response.isPresent) {
            return emptyList()
        }

        val actions = mutableListOf<SqlAction>()

        for (row in z3Response.get()) {
            val smtTableName = getTableName(row.key)
            val columns = row.value as StructValue

            // Find the SmtTable by its SMT-safe name (which is what Z3 returns)
            val smtTable = smtTables.firstOrNull { it.smtName == smtTableName }
                ?: throw RuntimeException("Table not found for SMT name: $smtTableName")
            val table = smtTable.table

            /*
             * The invariant requires that action.insertionId == primaryKey.uniqueId (and same for FK).
             * So we must use the same id for the action and all its PK/FK genes.
             */
            val actionId = idCounter
            idCounter++

            // Create the list of genes with the values
            val genes = mutableListOf<Gene>()
            // smtColumn is the ASCII version from SmtLib; resolve back to original DB column name
            for (smtColumn in columns.fields) {
                val dbColumn = table.columns.firstOrNull {
                    convertToAscii(it.name).equals(smtColumn, ignoreCase = true)
                }
                val dbColumnName = dbColumn?.name ?: smtColumn

                var gene: Gene = IntegerGene(dbColumnName, 0)
                when (val columnValue = columns.getField(smtColumn)) {
                    is StringValue -> {
                        gene = if (dbColumn?.type == ColumnDataType.BOOLEAN || dbColumn?.type == ColumnDataType.BOOL) {
                            BooleanGene(dbColumnName, toBoolean(columnValue.value))
                        } else {
                            StringGene(dbColumnName, columnValue.value)
                        }
                    }
                    is LongValue -> {
                        gene = if (dbColumn?.type == ColumnDataType.TIMESTAMP) {
                            val epochSeconds = columnValue.value.toLong()
                            val localDateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC
                            )
                            val formatted = localDateTime.format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                            ImmutableDataHolderGene(dbColumnName, formatted, inQuotes = true)
                        } else {
                            IntegerGene(dbColumnName, columnValue.value.toInt())
                        }
                    }
                    is RealValue -> {
                        gene = DoubleGene(dbColumnName, columnValue.value)
                    }
                }
                if (dbColumn != null && dbColumn.primaryKey) {
                    gene = SqlPrimaryKeyGene(dbColumnName, table.id, gene, actionId)
                }
                gene.markAllAsInitialized()
                genes.add(gene)
            }

            val sqlAction = SqlAction(table, table.columns, actionId, genes.toList())
            actions.add(sqlAction)
        }

        return actions
    }

    private fun toBoolean(value: String?): Boolean {
        return value.equals("True", ignoreCase = true)
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
