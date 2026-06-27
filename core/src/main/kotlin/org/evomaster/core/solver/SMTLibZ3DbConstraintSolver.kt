package org.evomaster.core.solver

import com.google.inject.Inject

import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.insert.Insert
import org.apache.commons.io.FileUtils
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.client.java.sql.DataRow
import org.evomaster.client.java.sql.QueryResult
import org.evomaster.client.java.sql.QueryResultSet
import org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator
import org.evomaster.client.java.sql.heuristic.TableColumnResolver
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Statistics
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.*
import org.evomaster.core.utils.StringUtils.convertToAscii
import org.evomaster.solver.Z3DockerExecutor
import org.evomaster.solver.Z3Result
import org.evomaster.solver.smtlib.SMTLib
import org.evomaster.solver.smtlib.value.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
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
import kotlin.text.equals

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

    // Memoization cache: (sqlQuery, numberOfRows) -> Z3Result (SAT or UNSAT only; errors are not cached)
    // Schema is assumed stable within a single run, so only query + row count form the key.
    // Null until DSE is enabled in postConstruct — avoids allocating the map in runs where DSE is off.
    private var z3ResultCache: MutableMap<Pair<String, Int>, Z3Result>? = null

    companion object {
        private const val MAX_CACHE_SIZE = 500
    }

    @Inject
    private lateinit var config: EMConfig

    /*
        Held WEAKLY on purpose. This solver has @PreDestroy, so each instance is
        retained by Governator's predestroy-monitor thread (a GC root) for the whole
        lifetime of the JVM. A strong reference to Statistics would therefore pin
        Statistics -> Archive -> every individual, leaking across every injector ever
        created. That is harmless in production (a single injector, process exits) but
        OOMs test suites that build thousands of injectors (RestIndividualTestBase,
        SamplerVerifierTest). A weak reference lets that graph be collected once the
        owning injector is otherwise unreachable, while still resolving fine during an
        active search (Statistics is strongly held by SearchTimeController then).
     */
    private var statisticsRef: WeakReference<Statistics>? = null

    @Inject(optional = true)
    fun setStatistics(statistics: Statistics) {
        this.statisticsRef = WeakReference(statistics)
    }

    @PostConstruct
    private fun postConstruct() {
        if (config.generateSqlDataWithDSE) {
            initializeExecutor()
            z3ResultCache = object : LinkedHashMap<Pair<String, Int>, Z3Result>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<String, Int>, Z3Result>?) =
                    size > MAX_CACHE_SIZE
            }
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
        if (::executor.isInitialized) {
            executor.close()
        }
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
     * @return A list of SQL actions that can be executed to satisfy the query,
     *         or an empty list if the problem is UNSAT, unparseable, or an error occurred.
     */
    override fun solve(schemaDto: DbInfoDto, sqlQuery: String, numberOfRows: Int): List<SqlAction> {
        val collectStats = ::config.isInitialized && config.collectDseStats
        val stats: Statistics? = if (collectStats) statisticsRef?.get() else null

        stats?.reportDseQuerySeen(sqlQuery.hashCode())

        val cacheKey = Pair(sqlQuery, numberOfRows)
        val cached = z3ResultCache?.get(cacheKey)
        if (cached != null) {
            return when (cached.status) {
                Z3Result.Status.SAT -> toSqlActionList(schemaDto, cached.model)
                else -> emptyList()
            }
        }

        val queryStatement = try {
            parseStatement(sqlQuery)
        } catch (e: RuntimeException) {
            LoggingUtil.getInfoLogger().warn("DSE: failed to parse SQL query as SMT-LIB: '$sqlQuery'")
            stats?.reportDseParseFailure()
            return emptyList()
        }

        val smtlibGenStart = System.currentTimeMillis()
        val generator = SmtLibGenerator(schemaDto, numberOfRows)
        val smtLib = generator.generateSMT(queryStatement)
        val smtlibBytes = smtLib.toString().toByteArray(StandardCharsets.UTF_8).size
        val smtlibGenMs = System.currentTimeMillis() - smtlibGenStart
        stats?.reportDseSmtlibGenTime(smtlibGenMs, smtlibBytes)

        val fileName = storeToTmpFile(smtLib)

        val z3Start = System.currentTimeMillis()
        val z3Result = try {
            executor.solveFromFile(fileName)
        } finally {
            Files.deleteIfExists(Paths.get(leadingBarResourcesFolder() + fileName))
        }
        val z3TimeMs = System.currentTimeMillis() - z3Start

        return when (z3Result.status) {
            Z3Result.Status.SAT -> {
                stats?.reportDseSat(z3TimeMs)
                z3ResultCache?.set(cacheKey, z3Result)
                toSqlActionList(schemaDto, z3Result.model)
            }
            Z3Result.Status.UNSAT -> {
                stats?.reportDseUnsat(z3TimeMs)
                z3ResultCache?.set(cacheKey, z3Result)
                emptyList()
            }
            Z3Result.Status.ERROR -> {
                LoggingUtil.getInfoLogger().warn("DSE: Z3 error for query '$sqlQuery': ${z3Result.errorMessage}")
                stats?.reportDseError(z3TimeMs)
                // Errors are not cached — they may be transient Docker failures
                emptyList()
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
     * Converts Z3's model to a list of SqlActions.
     *
     * @param model The satisfying assignment from Z3 (non-null, status must be SAT).
     * @return A list of SQL actions.
     */
    private fun toSqlActionList(schemaDto: DbInfoDto, model: Map<String, org.evomaster.solver.smtlib.value.SMTLibValue>): List<SqlAction> {
        val actions = mutableListOf<SqlAction>()

        for (row in model) {
            val tableName = getTableName(row.key)
            val columns = row.value as StructValue

            val table = findTableByName(schemaDto, tableName)

            val actionId = idCounter
            idCounter++

            val genes = mutableListOf<Gene>()
            for (smtColumn in columns.fields) {
                val dbColumn = table.columns.firstOrNull {
                    convertToAscii(it.name).equals(smtColumn, ignoreCase = true)
                }
                val dbColumnName = dbColumn?.name ?: smtColumn

                var gene: Gene = IntegerGene(dbColumnName, 0)
                when (val columnValue = columns.getField(smtColumn)) {
                    is StringValue -> {
                        gene = if (hasColumnType(schemaDto, table, dbColumnName, "BOOLEAN")) {
                            BooleanGene(dbColumnName, toBoolean(columnValue.value))
                        } else {
                            StringGene(dbColumnName, columnValue.value)
                        }
                    }
                    is LongValue -> {
                        gene = if (hasColumnType(schemaDto, table, dbColumnName, "TIMESTAMP")) {
                            val epochSeconds = columnValue.value.toLong()
                            val localDateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC
                            )
                            val formatted = localDateTime.format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                            ImmutableDataHolderGene(dbColumnName, formatted, inQuotes = true)
                        } else {
                            LongGene(dbColumnName, columnValue.value.toLong())
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

    private fun hasColumnType(
        schemaDto: DbInfoDto,
        table: Table,
        columnName: String?,
        expectedType: String
    ): Boolean {

        if (columnName == null) return false

        val tableDto = schemaDto.tables.firstOrNull {
            it.id.name.equals(table.id.name, ignoreCase = true)
        } ?: return false

        val col = tableDto.columns.firstOrNull {
            it.name.equals(columnName, ignoreCase = true)
        } ?: return false

        return col.type.equals(expectedType, ignoreCase = true)
    }

    /**
     * Extracts the table name from the key by removing the last character (index).
     *
     * @param key The key containing the table name and index.
     * @return The extracted table name.
     */
    private fun getTableName(key: String): String {
        return key.substring(0, key.length - 1)
    }

    /**
     * Finds a table by its name from the schema and constructs a Table object.
     *
     * @param schema The database schema.
     * @param tableName The name of the table to find.
     * @return The Table object.
     */
    private fun findTableByName(schema: DbInfoDto, tableName: String): Table {
        val tableDto = schema.tables.find { it.id.name.equals(tableName, ignoreCase = true) }
            ?: throw RuntimeException("Table not found: $tableName")
        return Table(
            TableId.fromDto(schema.databaseType, tableDto.id),
            findColumns(schema, tableDto),
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
            else -> ColumnDataType.CHARACTER_VARYING
        }
    }

    // TODO: Implement this method
    private fun findForeignKeys(tableDto: TableDto): Set<ForeignKey> {
        return emptySet()
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
            val directory = Paths.get(directoryPath)
            if (!directory.exists()) {
                directory.createDirectories()
            }

            var fileName = "$fileNameBase$fileExtension"
            var filePath = directory.resolve(fileName)
            if (filePath.exists()) {
                val randomSuffix = (1000..9999).random()
                fileName = "${fileNameBase}_$randomSuffix$fileExtension"
                filePath = directory.resolve(fileName)
            }

            Files.write(filePath, smtLib.toString().toByteArray(StandardCharsets.UTF_8))

            return fileName
        } catch (e: IOException) {
            throw RuntimeException("Failed to write SMTLib to file: ${e.message}")
        }
    }

    private fun leadingBarResourcesFolder() = if (resourcesFolder.endsWith("/")) resourcesFolder else "$resourcesFolder/"

    private fun computeCorrectnessDistance(
        sqlQuery: String,
        schemaDto: DbInfoDto,
        sqlActions: List<SqlAction>
    ): SqlDistanceWithMetrics {
        val queryResultSet = toQueryResultSet(schemaDto, sqlActions)
        val calculator = SqlHeuristicsCalculator.SqlHeuristicsCalculatorBuilder()
            .withTableColumnResolver(TableColumnResolver(schemaDto))
            .withSourceQueryResultSet(queryResultSet)
            .build()
        return calculator.computeDistance(sqlQuery)
    }

    private fun toQueryResultSet(schemaDto: DbInfoDto, sqlActions: List<SqlAction>): QueryResultSet {
        val queryResultSet = QueryResultSet()
        val byTable = sqlActions.groupBy { it.table.id.name }
        for ((tableName, actions) in byTable) {
            val columnNames = actions.first().seeTopGenes().map { it.name }
            val queryResult = QueryResult(columnNames, tableName)
            for (action in actions) {
                val values: List<Any?> = action.seeTopGenes().map { gene -> extractGeneValue(gene) }
                queryResult.addRow(DataRow(tableName, columnNames, values))
            }
            queryResultSet.addQueryResult(queryResult)
        }
        // Tables not present in Z3's SAT model (e.g. the optional side of a LEFT OUTER JOIN)
        // still need an (empty) QueryResult, otherwise SqlHeuristicsCalculator NPEs when it
        // looks them up unconditionally while walking the FROM/JOIN clause.
        for (table in schemaDto.tables) {
            if (table.id.name !in byTable.keys) {
                val columnNames = table.columns.map { it.name }
                queryResultSet.addQueryResult(QueryResult(columnNames, table.id.name))
            }
        }
        return queryResultSet
    }

    private fun extractGeneValue(gene: Gene): Any? {
        val inner = if (gene is SqlPrimaryKeyGene) gene.gene else gene
        return when (inner) {
            is IntegerGene             -> inner.value
            is LongGene                -> inner.value
            is StringGene              -> inner.value
            is DoubleGene              -> inner.value
            is BooleanGene             -> inner.value
            is ImmutableDataHolderGene -> inner.value
            else -> {
                LoggingUtil.getInfoLogger().warn(
                    "DSE: extractGeneValue() fallback to raw string for unhandled gene type " +
                            "${inner.javaClass.name} (outer: ${gene.javaClass.name}, name: ${gene.name})"
                )
                inner.getValueAsRawString()
            }
        }
    }
}
