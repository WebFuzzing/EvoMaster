package org.evomaster.core.database.sql.solver.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Statistics
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.database.sql.solver.DbConstraintSolver
import org.evomaster.core.database.sql.solver.SmtLibGenerator
import org.evomaster.core.database.sql.schema.Column
import org.evomaster.core.database.sql.schema.ColumnDataType
import org.evomaster.core.database.sql.schema.ForeignKey
import org.evomaster.core.database.sql.schema.Table
import org.evomaster.core.database.sql.schema.TableId
import org.evomaster.core.utils.StringUtils.convertToAscii
import org.evomaster.core.utils.TimeUtils
import org.evomaster.dbconstraint.ast.SqlCondition
import org.evomaster.solver.Z3DockerExecutor
import org.evomaster.solver.Z3Result
import org.evomaster.solver.Z3Solution
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
import kotlin.collections.iterator
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.text.equals

/**
 * A row already turned into an action, kept so that later rows can bind their foreign keys to it.
 *
 * @param values the value Z3 assigned to each column, keyed by the uppercase column name.
 */
private class InsertedRow(val tableId: TableId, val actionId: Long, val values: Map<String, String?>)

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

    // Memoization cache: (sqlQuery, numberOfRows) -> Z3Result (SAT, UNSAT, or an ERROR that is deterministic)
    // Schema is assumed stable within a single run, so only query + row count form the key.
    // Null until Z3 SQL generation is enabled in postConstruct — avoids allocating the map in runs where Z3 SQL generation is off.
    //
    // THREAD-SAFETY: the backing map is an access-ordered LinkedHashMap (a bounded LRU, whose get() mutates
    // internal order), wrapped in Collections.synchronizedMap so every operation is guarded by the map's
    // intrinsic lock. This is the standard idiom for a thread-safe bounded LRU (a plain ConcurrentHashMap
    // cannot do access-order eviction). Compound get-then-put in solve() is intentionally not atomic: at
    // worst two threads recompute the same query concurrently, which is harmless (idempotent), and never
    // corrupts the map. This makes the cache safe for the parallelized fitness evaluation that is planned.
    private var z3ResultCache: MutableMap<Pair<String, Int>, Z3Result>? = null

    /**
     * Queries that could not be translated at all, remembered so they are attempted once per run.
     *
     * Z3's UNKNOWN and ERROR outcomes are deliberately not written to any cache, since either can be
     * a timeout or a transient container fault and remembering one would turn a single bad run into a
     * permanent answer. A translation failure is different because it is deterministic: neither the
     * query nor the schema changes, so the second attempt fails exactly as the first did. The kind is
     * kept so the statistics keep attributing the failure to the step that produced it.
     */
    private var untranslatableQueries: MutableMap<Pair<String, Int>, Statistics.SqlZ3TranslationFailure>? = null

    /**
     * Shared across the generators built on each cache miss; see [SmtLibGenerator].
     *
     * Safe to share: the parsed AST is immutable (no setters, all fields final) and the visitor that
     * reads it is built fresh per use. Unbounded on purpose — the keys are the schema's own CHECK
     * expressions, a fixed set for the run, which is why [EMConfig.sqlZ3CacheSize] does not govern it.
     */
    private var checkExpressionCache: MutableMap<String, SqlCondition?>? = null

    companion object {
        // The single layout emitted when turning an epoch value back into a SQL literal. JSqlVisitor
        // reads a superset of the layouts a database may emit, of which this is one, so the value
        // round-trips: what is written here is read back to the same instant.
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"

        private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

        /** Rejects trailing content, so that e.g. "1 2" is not taken for the JSON document 1. */
        private val JSON_MAPPER = ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

        /**
         * Spellings [SmtLibGenerator.TYPE_MAP] already treats as the same type. Taken from the
         * generator rather than repeated, so the domain constraint and the gene reconstruction cannot
         * drift apart; consolidating all three type vocabularies into one source of truth remains
         * future work.
         */
        private val BOOLEAN_SPELLINGS = SmtLibGenerator.BOOLEAN_TYPES
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
        if (config.generateSqlDataWithZ3) {
            initializeExecutor()
            initializeCaches(config.sqlZ3CacheSize)
        }
    }

    /**
     * Extracted from [postConstruct] so a test can build the caches without a Docker executor.
     *
     * [maxSize] is passed in rather than read from [EMConfig] here: a test that builds this solver
     * directly has no injected configuration, and one exercising eviction wants a small bound so that
     * what it costs to run does not track whatever the production default happens to be.
     */
    internal fun initializeCaches(maxSize: Int) {
        val lru = object : LinkedHashMap<Pair<String, Int>, Z3Result>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<String, Int>, Z3Result>?) =
                size > maxSize
        }
        z3ResultCache = Collections.synchronizedMap(lru)

        val failedLru = object : LinkedHashMap<Pair<String, Int>, Statistics.SqlZ3TranslationFailure>(16, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<Pair<String, Int>, Statistics.SqlZ3TranslationFailure>?
            ) = size > maxSize
        }
        untranslatableQueries = Collections.synchronizedMap(failedLru)

        checkExpressionCache = Collections.synchronizedMap(HashMap())
    }

    internal fun untranslatableQueryCount(): Int = untranslatableQueries?.size ?: 0

    internal fun isRememberedUntranslatable(sqlQuery: String, numberOfRows: Int): Boolean =
        untranslatableQueries?.containsKey(Pair(sqlQuery, numberOfRows)) == true

    internal fun rememberedFailureKind(sqlQuery: String, numberOfRows: Int): Statistics.SqlZ3TranslationFailure? =
        untranslatableQueries?.get(Pair(sqlQuery, numberOfRows))

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
        val collectStats = ::config.isInitialized && config.collectSqlZ3Stats
        val stats: Statistics? = if (collectStats) statisticsRef?.get() else null

        /*
            Timed as a whole, and skipped entirely when nothing would report it. The two brackets that
            already existed, around Z3 and around formula generation, leave out writing the .smt2 file,
            rebuilding the gene tree from a solution, and the call overhead; measuring only those
            understates what the solver costs the search.

            A call that throws goes unmeasured, since the reporting happens on return. That only costs
            a slightly low total: this figure is a running sum with no companion count to fall out of
            step with, and an escaping exception here is a crash rather than a handled outcome.
         */
        if (stats == null) {
            return doSolve(schemaDto, sqlQuery, numberOfRows, null)
        }

        return TimeUtils.measureTimeMillis(
            { ms, _ -> stats.reportSqlZ3SolveTime(ms) },
            { doSolve(schemaDto, sqlQuery, numberOfRows, stats) }
        )
    }

    private fun doSolve(
        schemaDto: DbInfoDto,
        sqlQuery: String,
        numberOfRows: Int,
        stats: Statistics?
    ): List<SqlAction> {

        val cacheKey = Pair(sqlQuery, numberOfRows)
        // Track "seen" against the same key the cache uses, so unique/duplicate counts
        // line up with actual cache granularity.
        stats?.reportSqlZ3QuerySeen(cacheKey.hashCode())

        // A query that could not be translated will fail the same way every time, so the attempt is
        // made once. The failure is still counted on each encounter, so the reported totals keep
        // reflecting how often the search runs into one.
        untranslatableQueries?.get(cacheKey)?.let { kind ->
            stats?.reportSqlZ3ParseFailure(kind)
            return emptyList()
        }

        val cached = z3ResultCache?.get(cacheKey)
        if (cached != null) {
            stats?.reportSqlZ3CacheHit()
            return when (cached.status) {
                Z3Result.Status.SAT -> toSqlActionList(schemaDto, cached.solution)
                else -> emptyList()
            }
        }

        val queryStatement = try {
            parseStatement(sqlQuery)
        } catch (e: RuntimeException) {
            LoggingUtil.uniqueWarn(LoggingUtil.getInfoLogger(), "SQL-Z3: failed to parse SQL query: '$sqlQuery'")
            stats?.reportSqlZ3ParseFailure(Statistics.SqlZ3TranslationFailure.SQL_PARSE)
            untranslatableQueries?.put(cacheKey, Statistics.SqlZ3TranslationFailure.SQL_PARSE)
            return emptyList()
        }

        val smtlibGenStart = System.currentTimeMillis()
        val generator = SmtLibGenerator(schemaDto, numberOfRows, checkExpressionCache)
        // SMT-LIB generation can throw for unsupported column types or query shapes it cannot handle
        // (e.g. a cast failure on an unexpected statement structure). Degrade gracefully to an empty
        // result instead of letting the exception propagate into the structure mutator.
        val smtLib = try {
            generator.generateSMT(queryStatement)
        } catch (e: RuntimeException) {
            LoggingUtil.uniqueWarn(LoggingUtil.getInfoLogger(), "SQL-Z3: failed to generate SMT-LIB for query '$sqlQuery': ${e.message}")
            stats?.reportSqlZ3ParseFailure(Statistics.SqlZ3TranslationFailure.SMTLIB_GENERATION)
            untranslatableQueries?.put(cacheKey, Statistics.SqlZ3TranslationFailure.SMTLIB_GENERATION)
            return emptyList()
        }
        val smtlibBytes = smtLib.toString().toByteArray(StandardCharsets.UTF_8).size
        val smtlibGenMs = System.currentTimeMillis() - smtlibGenStart
        stats?.reportSqlZ3SmtlibGenTime(smtlibGenMs, smtlibBytes)

        // If a WHERE/JOIN condition could not be translated, it was dropped from the SMT problem, so the
        // generated data may not satisfy the original query. Record it: this failure mode is otherwise
        // invisible in the stats (Z3 typically still returns SAT on the weakened formula).
        if (generator.skippedQueryConstraints > 0) {
            stats?.reportSqlZ3PartialTranslation()
        }

        val fileName = storeToTmpFile(smtLib)

        val z3Start = System.currentTimeMillis()
        val z3Timeout = if (::config.isInitialized) config.sqlZ3TimeoutMs.toLong()
            else EMConfig.DEFAULT_SQL_Z3_TIMEOUT_MS.toLong()
        val z3Result = try {
            executor.solveFromFile(fileName, z3Timeout)
        } finally {
            Files.deleteIfExists(Paths.get(leadingBarResourcesFolder() + fileName))
        }
        val z3TimeMs = System.currentTimeMillis() - z3Start

        return when (z3Result.status) {
            Z3Result.Status.SAT -> {
                stats?.reportSqlZ3Sat(z3TimeMs)
                z3ResultCache?.set(cacheKey, z3Result)
                toSqlActionList(schemaDto, z3Result.solution)
            }
            Z3Result.Status.UNSAT -> {
                stats?.reportSqlZ3Unsat(z3TimeMs)
                z3ResultCache?.set(cacheKey, z3Result)
                emptyList()
            }
            Z3Result.Status.UNKNOWN -> {
                LoggingUtil.uniqueWarn(LoggingUtil.getInfoLogger(), "SQL-Z3: Z3 returned 'unknown' (incomplete theory or timeout) for query '$sqlQuery'")
                stats?.reportSqlZ3Unknown(z3TimeMs)
                // Not cached: an 'unknown' may be timeout-driven and therefore transient
                emptyList()
            }
            Z3Result.Status.ERROR -> {
                LoggingUtil.uniqueWarn(LoggingUtil.getInfoLogger(), "SQL-Z3: Z3 error for query '$sqlQuery': ${z3Result.errorMessage}")
                stats?.reportSqlZ3Error(z3TimeMs)
                // Only an error the same formula always reproduces (Z3 rejected it, or its output could
                // not be parsed) is cached; any other one may be a transient Docker failure
                if (z3Result.isDeterministicError) {
                    z3ResultCache?.set(cacheKey, z3Result)
                }
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
                // Not logged here: the single caller reports this failure with the surrounding
                // context, and once per distinct query rather than once per attempt.
                throw RuntimeException(e)
            }
        }
    }

    private fun removeNotSupportedKeywords(sqlQuery: String): String {
        return sqlQuery.replace("local temporary", "")
    }

    /**
     * Converts Z3's solution to a list of SqlActions.
     *
     * @param solution The satisfying assignment from Z3 (non-null, status must be SAT).
     * @return A list of SQL actions.
     */
    private fun toSqlActionList(schemaDto: DbInfoDto, solution: Z3Solution): List<SqlAction> {
        val actions = mutableListOf<SqlAction>()
        val insertedRows = mutableListOf<InsertedRow>()

        for (row in inInsertionOrder(schemaDto, solution.assignments)) {
            val tableName = getTableName(row.key)
            val columns = row.value as StructValue

            val table = findTableByName(schemaDto, tableName)

            val actionId = idCounter
            idCounter++

            val valueGenes = mutableListOf<Gene>()
            val rawValues = mutableMapOf<String, String?>()
            for (smtColumn in columns.fields) {
                val dbColumn = table.columns.firstOrNull {
                    convertToAscii(it.name).equals(smtColumn, ignoreCase = true)
                }
                val dbColumnName = dbColumn?.name ?: smtColumn
                val columnValue = columns.getField(smtColumn)

                rawValues[dbColumnName.uppercase()] = rawValue(columnValue)
                valueGenes.add(toValueGene(schemaDto, table, dbColumnName, columnValue))
            }

            val foreignKeyGenes = bindForeignKeys(table, actionId, rawValues, insertedRows)

            val genes = valueGenes.map { valueGene ->
                var gene: Gene = foreignKeyGenes[valueGene.name.uppercase()] ?: valueGene
                val dbColumn = table.columns.firstOrNull { it.name.equals(valueGene.name, ignoreCase = true) }
                if (dbColumn != null && dbColumn.primaryKey) {
                    gene = SqlPrimaryKeyGene(valueGene.name, table.id, gene, actionId)
                }
                gene.markAllAsInitialized()
                gene
            }

            actions.add(SqlAction(table, table.columns, actionId, genes))
            insertedRows.add(InsertedRow(table.id, actionId, rawValues))
        }

        return actions
    }

    /**
     * Builds a [SqlForeignKeyGene] for each foreign key column of a row, bound to the action of the row
     * it references.
     *
     * Z3 makes the value of a foreign key equal to the primary key of some row of the referenced
     * table, but a plain value gene would only copy that value: once the search mutates either side,
     * the reference breaks, and the FK repair of EvoMaster does not recognise the column as a foreign
     * key. Binding the gene to the referenced action keeps the two linked, as for any other insertion.
     *
     * The referenced row is the one, among the rows inserted before this one, whose target columns hold
     * the values Z3 assigned to the source columns. A foreign key is left as plain values when no such
     * row exists: when its target columns are not the primary key, which a [SqlForeignKeyGene] cannot
     * refer to, or when a row references itself, since no action precedes it.
     *
     * @return the foreign key genes, keyed by the uppercase name of their source column.
     */
    private fun bindForeignKeys(
        table: Table,
        actionId: Long,
        rawValues: Map<String, String?>,
        insertedRows: List<InsertedRow>
    ): Map<String, SqlForeignKeyGene> {
        val genes = mutableMapOf<String, SqlForeignKeyGene>()

        for (foreignKey in table.foreignKeys) {
            if (foreignKey.targetColumns.any { !it.primaryKey }) continue

            val referenced = insertedRows.lastOrNull { row ->
                row.tableId == foreignKey.targetTableId &&
                    foreignKey.sourceColumns.indices.all { i ->
                        val value = rawValues[foreignKey.sourceColumns[i].name.uppercase()]
                        value != null && value == row.values[foreignKey.targetColumns[i].name.uppercase()]
                    }
            } ?: continue

            foreignKey.sourceColumns.forEachIndexed { i, sourceColumn ->
                genes[sourceColumn.name.uppercase()] = SqlForeignKeyGene(
                    sourceColumn = sourceColumn.name,
                    uniqueId = actionId,
                    targetTable = foreignKey.targetTableId,
                    targetColumn = foreignKey.targetColumns[i].name,
                    nullable = sourceColumn.nullable,
                    uniqueIdOfPrimaryKey = referenced.actionId,
                    otherSourceColumnsInCompositeFK = foreignKey.sourceColumns
                        .filter { it != sourceColumn }.map { it.name }.toSet()
                )
            }
        }

        return genes
    }

    /**
     * The value Z3 assigned to a column, as a string that can be compared across rows.
     */
    private fun rawValue(value: SMTLibValue?): String? = when (value) {
        is LongValue -> value.value.toString()
        is RealValue -> value.value.toString()
        is StringValue -> value.value
        else -> null
    }

    /**
     * Builds the gene holding the value Z3 assigned to a column, according to the column's SQL type.
     */
    private fun toValueGene(schemaDto: DbInfoDto, table: Table, dbColumnName: String, columnValue: SMTLibValue?): Gene {
        return when (columnValue) {
            is StringValue -> {
                if (hasColumnType(schemaDto, table, dbColumnName, SmtLibGenerator.BOOLEAN_TYPE)) {
                    BooleanGene(dbColumnName, toBoolean(columnValue.value))
                } else {
                    StringGene(dbColumnName, validTextValue(schemaDto, table, dbColumnName, columnValue.value))
                }
            }
            is LongValue -> {
                if (hasColumnType(schemaDto, table, dbColumnName, SmtLibGenerator.TIMESTAMP_TYPE)) {
                    val epochSeconds = columnValue.value.toLong()
                    val localDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC
                    )
                    val formatted = localDateTime.format(
                        DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT)
                    )
                    ImmutableDataHolderGene(dbColumnName, formatted, inQuotes = true)
                } else {
                    LongGene(dbColumnName, columnValue.value.toLong())
                }
            }
            is RealValue -> DoubleGene(dbColumnName, columnValue.value)
            else -> IntegerGene(dbColumnName, 0)
        }
    }

    /**
     * Orders the rows of a solution so that each one comes after the rows it references through a
     * foreign key, and rows of the same table by their index.
     *
     * The parser returns the rows in a hash map, so their order is arbitrary. Inserting a row before
     * the row its foreign key points to makes the database reject it.
     */
    private fun inInsertionOrder(
        schemaDto: DbInfoDto,
        rows: Map<String, SMTLibValue>
    ): List<Map.Entry<String, SMTLibValue>> {
        val tableRank = tablesInDependencyOrder(schemaDto)
            .withIndex()
            .associate { (rank, table) -> convertToAscii(table.id.name).lowercase() to rank }

        return rows.entries.sortedWith(
            compareBy<Map.Entry<String, SMTLibValue>>(
                { tableRank[getTableName(it.key).lowercase()] ?: Int.MAX_VALUE },
                { it.key.substringAfterLast(SmtLibGenerator.ROW_INDEX_SEPARATOR).toIntOrNull() ?: 0 }
            )
        )
    }

    /**
     * The tables of the schema, each one after the tables its foreign keys reference. Self references
     * and cycles are ignored, as no order can satisfy them.
     */
    private fun tablesInDependencyOrder(schemaDto: DbInfoDto): List<TableDto> {
        val byName = schemaDto.tables.associateBy { it.id.name.lowercase() }
        val ordered = LinkedHashMap<String, TableDto>()
        val visiting = mutableSetOf<String>()

        fun visit(table: TableDto) {
            val name = table.id.name.lowercase()
            if (name in ordered || !visiting.add(name)) return
            for (foreignKey in table.foreignKeys) {
                byName[foreignKey.targetTable.lowercase()]?.let { visit(it) }
            }
            ordered[name] = table
        }

        schemaDto.tables.forEach { visit(it) }
        return ordered.values.toList()
    }

    private fun toBoolean(value: String?): Boolean {
        return value.equals("True", ignoreCase = true)
    }

    /**
     * Whether the given column's SQL type (as reported in [ColumnDto.type]) equals [expectedType],
     * compared case-insensitively as a raw string.
     *
     * CAVEAT: this relies on [ColumnDto.type] containing the exact spelling passed in (currently
     * "BOOLEAN" and "TIMESTAMP"). It is needed because the SMT sort alone cannot recover these types
     * (BOOLEAN is encoded as an SMT String, TIMESTAMP as an SMT Int), so gene reconstruction must
     * consult the original SQL type. The set of type spellings recognized here must stay consistent
     * with [SmtLibGenerator.TYPE_MAP]; if a backend reports a variant spelling (e.g. "BOOL" or
     * "TIMESTAMP WITHOUT TIME ZONE"), the special handling is silently skipped. Consolidating these
     * type vocabularies into a single source of truth is future work.
     */
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

        return typeMatches(col.type, expectedType)
    }

    /**
     * Makes the value Z3 assigned to a UUID, JSON or JSONB column one the database accepts.
     *
     * They are encoded as SMT Strings with no constraint on their form, so unless the query pins the
     * value, Z3 picks something like "" and the INSERT of the whole row is rejected. A value that is
     * already valid -- e.g. one copied from the query -- is kept. Any other is mapped
     * deterministically: equal strings give equal values and different strings different ones, so
     * the equalities and distinctions the formula imposes (foreign keys, unique and primary keys)
     * still hold. Constraining the form inside the formula instead would need Z3's regular
     * expressions, which make solving noticeably more expensive.
     */
    private fun validTextValue(schemaDto: DbInfoDto, table: Table, columnName: String, value: String): String =
        when {
            hasColumnType(schemaDto, table, columnName, SmtLibGenerator.UUID_TYPE) && !UUID_PATTERN.matches(value) ->
                UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8)).toString()
            (hasColumnType(schemaDto, table, columnName, SmtLibGenerator.JSON_TYPE) ||
                hasColumnType(schemaDto, table, columnName, SmtLibGenerator.JSONB_TYPE)) && !isJson(value) ->
                JSON_MAPPER.writeValueAsString(value) // the value as a JSON string
            else -> value
        }

    private fun isJson(value: String): Boolean =
        try {
            // readTree returns a MissingNode, rather than failing, on blank input
            !JSON_MAPPER.readTree(value).isMissingNode
        } catch (e: JsonProcessingException) {
            false
        }

    /**
     * Whether a column's declared type is the expected one, accounting for spellings that
     * [SmtLibGenerator.TYPE_MAP] already treats as equivalent.
     *
     * Without this, a column declared `BOOL` is encoded as an SMT String exactly like a `BOOLEAN`
     * one — the type map sends both to the same sort — but is not recognised as boolean when the
     * solution is turned back into genes, so it silently loses its boolean handling and arrives as
     * a string.
     */
    internal fun typeMatches(declaredType: String, expectedType: String): Boolean {
        val declared = declaredType.uppercase()
        val expected = expectedType.uppercase()
        if (declared == expected) return true
        return BOOLEAN_SPELLINGS.contains(declared) && BOOLEAN_SPELLINGS.contains(expected)
    }

    /**
     * Extracts the table name from a row-constant key by removing the trailing row index.
     *
     * Row constants are named "${smtName}${SEP}${i}" (e.g. "users__1", "users__2"; see
     * [SmtLibGenerator.rowConstantName]). Splitting on the last separator recovers the table name
     * unambiguously even when the table name itself ends in digits (e.g. "inventory2026__1" -> "inventory2026").
     *
     * @param key The key containing the table name and index.
     * @return The extracted table name.
     */
    private fun getTableName(key: String): String {
        return key.substringBeforeLast(SmtLibGenerator.ROW_INDEX_SEPARATOR)
    }

    /**
     * Finds a table by its name from the schema and constructs a Table object.
     *
     * The name comes from a row constant, so it is the ASCII-folded SMT name of the table (see
     * [org.evomaster.core.database.sql.solver.SmtTable.smtName]), not the name in the schema. The
     * schema names are folded the same way before comparing, as is already done for column names in
     * [toSqlActionList]; comparing against the original names would miss any table whose name contains
     * non-ASCII characters (e.g. "categoria" for "Categoría") and discard the whole solution.
     *
     * @param schema The database schema.
     * @param tableName The SMT name of the table to find.
     * @return The Table object.
     */
    private fun findTableByName(schema: DbInfoDto, tableName: String): Table {
        val tableDto = schema.tables.find { convertToAscii(it.id.name).equals(tableName, ignoreCase = true) }
            ?: throw RuntimeException("Table not found: $tableName")
        return Table(
            TableId.fromDto(schema.databaseType, tableDto.id),
            findColumns(schema, tableDto),
            findForeignKeys(schema, tableDto)
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
     * TODO: this recognizes only a small subset of SQL type spellings and falls back to
     * CHARACTER_VARYING for everything else. It is one of three independent type vocabularies that
     * interpret [ColumnDto.type] — the others being [SmtLibGenerator.TYPE_MAP] (SQL type -> SMT sort)
     * and [hasColumnType] (BOOLEAN/TIMESTAMP special-casing). These can silently disagree when a
     * backend reports a variant spelling. They should be consolidated into a single source of truth
     * so that generation and interpretation cannot drift; see the note on [hasColumnType].
     *
     * @param type The column type as a string.
     * @return The corresponding ColumnDataType.
     */
    internal fun getColumnDataType(type: String): ColumnDataType {
        // Matched case-insensitively. SmtLibGenerator.TYPE_MAP uppercases before looking up, so a
        // backend reporting a lowercase spelling used to be mapped there but fall through to the
        // default here — the two vocabularies disagreeing silently about the same column.
        return when (type.uppercase()) {
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

    /**
     * Rebuilds the foreign keys of a table from the schema, in the same way as
     * [org.evomaster.core.database.sql.SqlInsertBuilder]: when the schema does not name the target
     * columns, the foreign key references the primary key of the target table. A foreign key whose
     * table or columns cannot be resolved is skipped.
     */
    private fun findForeignKeys(schema: DbInfoDto, tableDto: TableDto): Set<ForeignKey> {
        val sourceColumns = findColumns(schema, tableDto)

        return tableDto.foreignKeys.mapNotNull { foreignKey ->
            val targetDto = schema.tables.find { it.id.name.equals(foreignKey.targetTable, ignoreCase = true) }
                ?: return@mapNotNull null
            val targetColumns = findColumns(schema, targetDto)

            val source = foreignKey.sourceColumns.map { name ->
                sourceColumns.find { it.name.equals(name, ignoreCase = true) } ?: return@mapNotNull null
            }
            val target = if (foreignKey.targetColumns.isEmpty()) {
                targetColumns.filter { it.primaryKey }
            } else {
                foreignKey.targetColumns.map { name ->
                    targetColumns.find { it.name.equals(name, ignoreCase = true) } ?: return@mapNotNull null
                }
            }
            if (source.isEmpty() || source.size != target.size) return@mapNotNull null

            ForeignKey(source, TableId.fromDto(schema.databaseType, targetDto.id), target)
        }.toSet()
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
}
