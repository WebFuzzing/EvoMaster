package org.evomaster.core.database.sql.solver

import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.select.FromItem
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import net.sf.jsqlparser.util.TablesNamesFinder
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.ForeignKeyDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.dbconstraint.ConstraintDatabaseType
import org.evomaster.dbconstraint.ast.SqlAndCondition
import org.evomaster.dbconstraint.ast.SqlCondition
import net.sf.jsqlparser.JSQLParserException
import org.evomaster.core.utils.StringUtils.convertToAscii
import org.evomaster.dbconstraint.parser.SqlConditionParserException
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser
import org.evomaster.solver.smtlib.*
import org.evomaster.solver.smtlib.assertion.*

/**
 * Generates SMT-LIB constraints from SQL queries and schema definitions.
 *
 * @param schema The database schema containing tables and constraints.
 * @param numberOfRows The number of rows to be considered in constraints.
 */
class SmtLibGenerator(private val schema: DbInfoDto, private val numberOfRows: Int) {

    private var parser = JSqlConditionParser()

    /**
     * Number of query constraints (WHERE conditions, JOIN ON conditions) that could not be translated
     * to SMT-LIB and were skipped during [generateSMT]. When greater than 0, the generated formula is
     * weaker than the original query, so Z3 may return SAT with rows that do not satisfy the dropped
     * predicate. Exposed so the caller can record it in the solver statistics.
     */
    var skippedQueryConstraints = 0
        private set

    private val smtTables: List<SmtTable> = schema.tables.map { SmtTable(it) }
    private val smtTableByOriginalName: Map<String, SmtTable> = smtTables.associateBy { it.originalName }

    companion object {

        /**
         * Separator inserted between a table's SMT name and its 1-based row index in a row-constant
         * name (e.g. "users__1"). Using an explicit separator (rather than just appending the index)
         * keeps table names that end in digits (e.g. "inventory2026") unambiguous when the name is
         * parsed back into its table part in SMTLibZ3DbConstraintSolver.getTableName.
         */
        const val ROW_INDEX_SEPARATOR = "__"

        /** Bounds for TIMESTAMP columns, encoded as epoch seconds (SMT Int). */
        private const val TIMESTAMP_EPOCH_LOWER_BOUND = 0L            // Unix epoch start
        private const val TIMESTAMP_EPOCH_UPPER_BOUND = 32503680000L  // ~year 3000, in seconds

        /** SMT-LIB sorts used as TYPE_MAP targets. */
        private const val SMT_INT = "Int"
        private const val SMT_REAL = "Real"
        private const val SMT_STRING = "String"

        /**
         * SQL type names that need special interpretation beyond their SMT sort: BOOLEAN is encoded as
         * an SMT String and TIMESTAMP as an SMT Int, so gene reconstruction must consult the original
         * type. Shared with the comparison sites in this class and referenced by SMTLibZ3DbConstraintSolver.
         */
        const val BOOLEAN_TYPE = "BOOLEAN"
        const val TIMESTAMP_TYPE = "TIMESTAMP"

        /**
         * The canonical string values a BOOLEAN column may take (BOOLEAN is encoded as an SMT String).
         * These are generation constraints, so Z3 is forced to pick one of them; only the two canonical
         * lowercase spellings are needed, and toBoolean() reads them back case-insensitively.
         */
        private val BOOLEAN_LITERALS = listOf("true", "false")

        /**
         * Builds the SMT row-constant name for a table's SMT name and a 1-based row index,
         * e.g. rowConstantName("users", 1) == "users__1". Must be the single source of truth for
         * this naming so declarations, assertions, get-value nodes and parsing all stay in sync.
         */
        fun rowConstantName(smtTableName: String, rowIndex: Int): String =
            "$smtTableName$ROW_INDEX_SEPARATOR$rowIndex"

        /**
         * Maps database column types to SMT-LIB types.
         *
         * FIXME: this is one of three independent type vocabularies interpreting ColumnDto.type
         * (the others are SMTLibZ3DbConstraintSolver.getColumnDataType and .hasColumnType). They can
         * silently disagree when a backend reports a variant spelling; consolidating them into a single
         * source of truth is future work (see the note on SMTLibZ3DbConstraintSolver.hasColumnType).
         */
        private val TYPE_MAP = mapOf(
            "BIGINT" to SMT_INT,
            "BIT" to SMT_INT,
            "INTEGER" to SMT_INT,
            "INT" to SMT_INT,
            "INT2" to SMT_INT,
            "INT4" to SMT_INT,
            "INT8" to SMT_INT,
            "TINYINT" to SMT_INT,
            "SMALLINT" to SMT_INT,
            // FIXME: NUMERIC is mapped to Int, so any fractional part is truncated. This is
            // inconsistent with DECIMAL (mapped to Real). Mapping NUMERIC to Real (to preserve decimals)
            // is future work.
            "NUMERIC" to SMT_INT,
            "SERIAL" to SMT_INT,
            "SMALLSERIAL" to SMT_INT,
            "BIGSERIAL" to SMT_INT,
            TIMESTAMP_TYPE to SMT_INT,
            "DATE" to SMT_INT,
            "FLOAT" to SMT_REAL,
            "DOUBLE" to SMT_REAL,
            "DECIMAL" to SMT_REAL,
            "REAL" to SMT_REAL,
            "CHARACTER VARYING" to SMT_STRING,
            "CHAR" to SMT_STRING,
            "VARCHAR" to SMT_STRING,
            "TEXT" to SMT_STRING,
            "CHARACTER LARGE OBJECT" to SMT_STRING,
            BOOLEAN_TYPE to SMT_STRING,
            "BOOL" to SMT_STRING,
            "UUID" to SMT_STRING,
            "JSONB" to SMT_STRING,
            "BYTEA" to SMT_STRING,
        )
    }

    /**
     * Main method to generate SMT-LIB representation from SQL query.
     *
     * @param sqlQuery The SQL query to be converted.
     * @return An SMTLib object containing the generated SMT-LIB constraints.
     */
    fun generateSMT(sqlQuery: Statement): SMTLib {
        val smt = SMTLib()

        appendTableDefinitions(smt)
        appendTableConstraints(smt)
        appendKeyConstraints(smt)
        appendTimestampConstraints(smt)
        appendBooleanConstraints(smt)
        appendQueryConstraints(smt, sqlQuery)
        appendGetValuesFromQuery(smt, sqlQuery)

        return smt
    }

    /**
     * Appends table definitions to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which table definitions are added.
     */
    private fun appendTableDefinitions(smt: SMTLib) {
        for (smtTable in smtTables) {
            // Declare datatype for the table
            smt.addNode(
                DeclareDatatypeSMTNode(smtTable.dataTypeName, getConstructors(smtTable))
            )

            // Declare constants for each row
            for (i in 1..numberOfRows) {
                smt.addNode(DeclareConstSMTNode(rowConstantName(smtTable.smtName, i), smtTable.dataTypeName))
            }
        }
    }

    /**
     * Appends table constraints (unique and check constraints) to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which table constraints are added.
     */
    private fun appendTableConstraints(smt: SMTLib) {
        for (smtTable in smtTables) {
            appendUniqueConstraints(smt, smtTable)
            appendCheckConstraints(smt, smtTable)
        }
    }

    /**
     * Appends unique constraints for each table to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which unique constraints are added.
     * @param smtTable The table for which unique constraints are added.
     */
    private fun appendUniqueConstraints(smt: SMTLib, smtTable: SmtTable) {
        for (column in smtTable.dto.columns) {
            if (column.unique) {
                val nodes = assertForDistinctField(smtTable.smtColumnName(column.name), smtTable.smtName)
                smt.addNodes(nodes)
            }
        }
    }

    /**
     * Appends check constraints for each table to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which check constraints are added.
     * @param smtTable The table for which check constraints are added.
     */
    private fun appendCheckConstraints(smt: SMTLib, smtTable: SmtTable) {
        for (check in smtTable.dto.tableCheckExpressions) {
            try {
                val condition: SqlCondition = parser.parse(check.sqlCheckExpression, toDBType(schema.databaseType))
                for (i in 1..numberOfRows) {
                    val constraint: SMTNode = parseCheckExpression(smtTable, condition, i)
                    smt.addNode(constraint)
                }
            } catch (e: SqlConditionParserException) {
                LoggingUtil.getInfoLogger().warn("Could not translate CHECK constraint to SMT-LIB, skipping: ${check.sqlCheckExpression}. Reason: ${e.message}")
            } catch (e: JSQLParserException) {
                LoggingUtil.getInfoLogger().warn("Could not translate CHECK constraint to SMT-LIB, skipping: ${check.sqlCheckExpression}. Reason: ${e.message}")
            }
        }
    }

    /**
     * Parses a check expression and returns the corresponding SMT node.
     *
     * @param smtTable The table containing the check expression.
     * @param condition The SQL condition to be parsed.
     * @param index The index of the row.
     * @return The corresponding SMT node.
     */
    private fun parseCheckExpression(smtTable: SmtTable, condition: SqlCondition, index: Int): SMTNode {
        val visitor = SMTConditionVisitor(smtTable.smtName, emptyMap(), schema.tables, index)
        return condition.accept(visitor, null) as SMTNode
    }

    /**
     * Maps database types to constraint database types.
     *
     * @param databaseType The type of the database.
     * @return The corresponding constraint database type.
     */
    private fun toDBType(databaseType: DatabaseType?): ConstraintDatabaseType {
        return when (databaseType) {
            DatabaseType.H2 -> ConstraintDatabaseType.H2
            DatabaseType.DERBY -> ConstraintDatabaseType.DERBY
            DatabaseType.MYSQL -> ConstraintDatabaseType.MYSQL
            DatabaseType.POSTGRES -> ConstraintDatabaseType.POSTGRES
            DatabaseType.MARIADB -> ConstraintDatabaseType.MARIADB
            DatabaseType.MS_SQL_SERVER -> ConstraintDatabaseType.MS_SQL_SERVER
            else -> ConstraintDatabaseType.OTHER
        }
    }

    /**
     * Appends primary key and foreign key constraints to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which key constraints are added.
     */
    private fun appendKeyConstraints(smt: SMTLib) {
        for (smtTable in smtTables) {
            appendPrimaryKeyConstraints(smt, smtTable)
            appendForeignKeyConstraints(smt, smtTable)
        }
    }

    private fun appendBooleanConstraints(smt: SMTLib) {
        for (smtTable in smtTables) {
            for (column in smtTable.dto.columns) {
                if (column.type.equals(BOOLEAN_TYPE, ignoreCase = true)) {
                    val columnName = smtTable.smtColumnName(column.name).uppercase()
                    for (i in 1..numberOfRows) {
                        smt.addNode(
                            AssertSMTNode(
                                OrAssertion(
                                    BOOLEAN_LITERALS.map { literal ->
                                        EqualsAssertion(listOf("($columnName ${rowConstantName(smtTable.smtName, i)})", "\"$literal\""))
                                    }
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun appendTimestampConstraints(smt: SMTLib) {
        for (smtTable in smtTables) {
            for (column in smtTable.dto.columns) {
                if (column.type.equals(TIMESTAMP_TYPE, ignoreCase = true)) {
                    val columnName = smtTable.smtColumnName(column.name).uppercase()

                    for (i in 1..numberOfRows) {
                        smt.addNode(
                            AssertSMTNode(
                                GreaterThanOrEqualsAssertion(
                                    "($columnName ${rowConstantName(smtTable.smtName, i)})",
                                    TIMESTAMP_EPOCH_LOWER_BOUND.toString()
                                )
                            )
                        )
                        smt.addNode(
                            AssertSMTNode(
                                LessThanOrEqualsAssertion(
                                    "($columnName ${rowConstantName(smtTable.smtName, i)})",
                                    TIMESTAMP_EPOCH_UPPER_BOUND.toString()
                                )
                            )
                        )
                    }
                }
            }
        }
    }


    /**
     * Appends primary key constraints for each table to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which primary key constraints are added.
     * @param smtTable The table for which primary key constraints are added.
     */
    private fun appendPrimaryKeyConstraints(smt: SMTLib, smtTable: SmtTable) {
        val primaryKeys = smtTable.dto.columns.filter { it.primaryKey }

        if (primaryKeys.size <= 1) {
            // Single-column PK: the column must be individually distinct across all row pairs.
            for (primaryKey in primaryKeys) {
                smt.addNodes(assertForDistinctField(smtTable.smtColumnName(primaryKey.name), smtTable.smtName))
            }
        } else {
            // Composite PK: the *tuple* of PK columns must be distinct across all row pairs,
            // meaning at least one column must differ — not necessarily all of them.
            // Emitting per-column distinctness (the old behaviour) was over-constrained: it
            // prevented valid rows like (emp=1, proj=2) and (emp=1, proj=3) because it forced
            // every PK column to differ individually, rather than just the tuple.
            val pkSelectors = primaryKeys.map { smtTable.smtColumnName(it.name) }
            smt.addNodes(assertForDistinctCompositePK(pkSelectors, smtTable.smtName))
        }
    }

    /**
     * Generates distinct assertions for a primary key field across all rows.
     *
     * @param pkSelector The primary key column name.
     * @param tableName The name of the table.
     * @return A list of SMT nodes representing distinct assertions.
     */
    private fun assertForDistinctField(pkSelector: String, tableName: String): List<SMTNode> {
        val nodes = mutableListOf<AssertSMTNode>()
        for (i in 1..numberOfRows) {
            for (j in i + 1..numberOfRows) {
                nodes.add(
                    AssertSMTNode(
                        DistinctAssertion(
                            listOf(
                                "(${pkSelector.uppercase()} ${rowConstantName(tableName, i)})",
                                "(${pkSelector.uppercase()} ${rowConstantName(tableName, j)})"
                            )
                        )
                    )
                )
            }
        }
        return nodes
    }

    /**
     * Generates composite PK distinctness assertions across all row pairs.
     * For each pair (i, j), asserts that at least one PK column differs between row i and row j.
     *
     * @param pkSelectors The list of PK column names (SMT form).
     * @param tableName The SMT name of the table.
     * @return A list of SMT nodes representing composite PK distinctness assertions.
     */
    private fun assertForDistinctCompositePK(pkSelectors: List<String>, tableName: String): List<SMTNode> {
        val nodes = mutableListOf<AssertSMTNode>()
        for (i in 1..numberOfRows) {
            for (j in i + 1..numberOfRows) {
                val columnDistinctness = pkSelectors.map { selector ->
                    DistinctAssertion(listOf(
                        "(${selector.uppercase()} ${rowConstantName(tableName, i)})",
                        "(${selector.uppercase()} ${rowConstantName(tableName, j)})"
                    ))
                }
                nodes.add(AssertSMTNode(OrAssertion(columnDistinctness)))
            }
        }
        return nodes
    }

    /**
     * Appends foreign key constraints for each table to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which foreign key constraints are added.
     * @param smtTable The table for which foreign key constraints are added.
     */
    private fun appendForeignKeyConstraints(smt: SMTLib, smtTable: SmtTable) {
        for (foreignKey in smtTable.dto.foreignKeys) {
            val referencedSmtTable = findReferencedSmtTable(foreignKey)
            val referencedColumnSelector = referencedSmtTable.smtColumnName(
                findReferencedPKSelector(smtTable.dto, referencedSmtTable.dto, foreignKey)
            )

            // TODO: composite foreign keys are not fully supported. Each source column is
            // matched independently against a single referenced column, rather than constraining the
            // whole tuple of source columns to match a referenced tuple. This is correct for
            // single-column FKs (the common case) but under-models multi-column FKs. Fully supporting
            // composite FKs (as a tuple-level OR over referenced rows) is future work.
            for (sourceColumn in foreignKey.sourceColumns) {
                val nodes = assertForEqualsAny(
                    smtTable.smtColumnName(sourceColumn), smtTable.smtName,
                    referencedColumnSelector, referencedSmtTable.smtName
                )
                smt.addNodes(nodes)
            }
        }
    }

    /**
     * Generates equality assertions for a foreign key column to match any primary key column in the referenced table.
     *
     * @param sourceColumnSelector The source column name.
     * @param sourceTableName The source table name.
     * @param referencedColumnSelector The referenced column name.
     * @param referencedTableName The referenced table name.
     * @return A list of SMT nodes representing equality assertions.
     */
    private fun assertForEqualsAny(
        sourceColumnSelector: String, sourceTableName: String,
        referencedColumnSelector: String, referencedTableName: String
    ): List<AssertSMTNode> {
        val nodes = mutableListOf<AssertSMTNode>()

        for (i in 1..numberOfRows) {
            val conditions = (1..numberOfRows).map { j ->
                EqualsAssertion(
                    listOf(
                        "(${sourceColumnSelector.uppercase()} ${rowConstantName(sourceTableName, i)})",
                        "(${referencedColumnSelector.uppercase()} ${rowConstantName(referencedTableName, j)})"
                    )
                )
            }
            if (conditions.size == 1) {
                nodes.add(AssertSMTNode(conditions[0]))
            } else {
                nodes.add(AssertSMTNode(OrAssertion(conditions)))
            }
        }
        return nodes
    }

    /**
     * Finds the primary key column name in the referenced table.
     *
     * @param referencedTable The referenced table.
     * @param foreignKey The foreign key constraint.
     * @return The primary key column name in the referenced table.
     */
    private fun findReferencedPKSelector(sourceTable: TableDto, referencedTable: TableDto, foreignKey: ForeignKeyDto): String {
        val referencedPrimaryKeys = referencedTable.columns.filter { it.primaryKey }
        val sourceColumnName = foreignKey.sourceColumns.firstOrNull()
        val sourceSmtType = sourceColumnName?.let { scn ->
            sourceTable.columns.firstOrNull { it.name.equals(scn, ignoreCase = true) }
                ?.let { TYPE_MAP[it.type.uppercase()] }
        }
        if (referencedPrimaryKeys.isNotEmpty() &&
            (sourceSmtType == null || TYPE_MAP[referencedPrimaryKeys[0].type.uppercase()] == sourceSmtType)) {
            return referencedPrimaryKeys[0].name
        }
        // No PK or type mismatch: find a type-compatible column
        if (sourceSmtType != null) {
            referencedTable.columns.firstOrNull { TYPE_MAP[it.type.uppercase()] == sourceSmtType }
                ?.let { return it.name }
        }
        return referencedTable.columns.firstOrNull()?.name
            ?: throw RuntimeException("Referenced table has no columns: ${foreignKey.targetTable}")
    }

    /**
     * Finds the [SmtTable] for the table referenced by the given foreign key.
     *
     * @param foreignKey The foreign key constraint.
     * @return The referenced [SmtTable].
     */
    private fun findReferencedSmtTable(foreignKey: ForeignKeyDto): SmtTable {
        return smtTableByOriginalName[foreignKey.targetTable.lowercase()]
            ?: throw RuntimeException("Referenced table not found: ${foreignKey.targetTable}")
    }

    /**
     * Appends query-specific constraints to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which query constraints are added.
     * @param sqlQuery The SQL query containing constraints.
     */
    private fun appendQueryConstraints(smt: SMTLib, sqlQuery: Statement) {
        val fromScope = extractFromScope(sqlQuery)
        val tableAliases = fromScope.tableAliases
        val columnScope = columnScopeOf(fromScope.derivedAliases)

        appendJoinConstraints(smt, sqlQuery, tableAliases, columnScope)

        val (where, defaultTable) = when (sqlQuery) {
            is Select -> {
                val plainSelect = sqlQuery.selectBody as PlainSelect
                Pair(plainSelect.where, TablesNamesFinder().getTables(sqlQuery as Statement).firstOrNull())
            }
            is Delete -> Pair(sqlQuery.where, sqlQuery.table.getName())
            is Update -> Pair(sqlQuery.where, sqlQuery.table.getName())
            else -> Pair(null, null)
        }

        if (where != null && defaultTable != null) {
            val condition = try {
                parser.parse(where.toString(), toDBType(schema.databaseType))
            } catch (e: RuntimeException) {
                skippedQueryConstraints++
                LoggingUtil.getInfoLogger()
                    .warn("Could not parse WHERE clause, skipping: ${where}. Reason: ${e.message}")
                null
            }

            if (condition != null) {
                appendConjuncts(smt, condition, "WHERE clause") { conjunct, i ->
                    parseQueryCondition(tableAliases, defaultTable, conjunct, i, columnScope)
                }
            }
        }
    }

    /**
     * Translates a clause one top-level conjunct at a time, so that a conjunct which cannot be
     * expressed in SMT-LIB costs only itself.
     *
     * The clause used to be translated as a single unit, which meant that one untranslatable
     * condition — a column of a derived table, say — discarded every other condition sharing the
     * `AND` with it, including ones that were perfectly expressible. Splitting first bounds that loss.
     *
     * The split stops at `AND` and does not descend into `OR`, because the two are not equally safe
     * to prune. Dropping a conjunct yields a formula weaker than the query, which is the failure mode
     * this class already accepts and counts: Z3 may return rows that do not satisfy the dropped
     * predicate. Dropping a disjunct would instead yield a formula *stronger* than the query, which
     * can turn a satisfiable query into `unsat` and produce no rows at all. So a disjunction is
     * translated whole or not at all.
     *
     * Note that this is a property of the split, not of the translation as a whole:
     * [SMTConditionVisitor.visit] for a disjunction already discards operands that translate to
     * nothing and keeps the rest, so `A OR (X IS NULL)` still reaches the formula as `A`. That
     * predates this method and is part of the untracked `NULL` gap, not something the split
     * introduces or can repair.
     *
     * @param clause a human-readable name for the clause, used only in the log message.
     * @param translate translates one conjunct for a given 1-based row index.
     */
    private fun appendConjuncts(
        smt: SMTLib,
        condition: SqlCondition,
        clause: String,
        translate: (SqlCondition, Int) -> SMTNode
    ) {
        for (conjunct in flattenConjuncts(condition)) {
            try {
                /*
                    Every row is translated before any node is added, so that a conjunct which fails
                    part-way through cannot leave the rows it already produced behind.
                 */
                val nodes = (1..numberOfRows).map { translate(conjunct, it) }
                    .filterNot { it is EmptySMTNode }
                smt.addNodes(nodes)
            } catch (e: RuntimeException) {
                skippedQueryConstraints++
                LoggingUtil.getInfoLogger().warn(
                    "Could not translate a condition of the $clause to SMT-LIB, skipping it:" +
                        " ${conjunct.toSql()}. Reason: ${e.message}"
                )
            }
        }
    }

    /**
     * Flattens a chain of `AND` conditions into its top-level conjuncts, leaving anything else as a
     * single element.
     */
    private fun flattenConjuncts(condition: SqlCondition): List<SqlCondition> =
        if (condition is SqlAndCondition)
            flattenConjuncts(condition.leftExpr) + flattenConjuncts(condition.rightExpr)
        else
            listOf(condition)

    /**
     * Appends join constraints to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which join constraints are added.
     * @param sqlQuery The SQL query containing join constraints.
     * @param tableAliases The map of table aliases.
     */
    private fun appendJoinConstraints(
        smt: SMTLib,
        sqlQuery: Statement,
        tableAliases: Map<String, String>,
        columnScope: SMTConditionVisitor.ColumnScope
    ) {
        if (sqlQuery is Select) { // TODO: Handle other queries
            val plainSelect = sqlQuery.selectBody as PlainSelect
            val joins = plainSelect.joins
            if (joins != null) {
                for (join in joins) {
                    val onExpressions = join.onExpressions
                    if (onExpressions.isNotEmpty()) {
                        // TODO: only the first ON expression is used; a composite ON
                        // (e.g. "a = b AND c = d") drops all but the first conjunct.
                        val onExpression = onExpressions.elementAt(0)
                        try {
                            val condition = parser.parse(onExpression.toString(), toDBType(schema.databaseType))
                            val tableFromQuery = TablesNamesFinder().getTables(sqlQuery as Statement).first()
                            // TODO: the ON condition is translated with the SAME row index on
                            // both sides ("diagonal pairing"): row i of one table is matched only with row i
                            // of the other. This is sufficient at the default numberOfRows=1 to force a
                            // non-empty JOIN, but it does not model full INNER JOIN semantics: for
                            // numberOfRows>=2 it never explores mismatched-index pairs (e.g. users2 with
                            // products1). Matching arbitrary row combinations is future work.
                            appendConjuncts(smt, condition, "JOIN ON clause") { conjunct, i ->
                                parseQueryCondition(tableAliases, tableFromQuery, conjunct, i, columnScope)
                            }
                        } catch (e: RuntimeException) {
                            skippedQueryConstraints++
                            LoggingUtil.getInfoLogger().warn("Could not parse JOIN ON clause, skipping: ${onExpression}. Reason: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses a query condition and returns the corresponding SMT node.
     *
     * @param tableAliases The map of table aliases.
     * @param defaultTableName The default table name to use.
     * @param condition The SQL condition to be parsed.
     * @param index The index of the row.
     * @return The corresponding SMT node.
     */
    private fun parseQueryCondition(
        tableAliases: Map<String, String>,
        defaultTableName: String,
        condition: SqlCondition,
        index: Int,
        columnScope: SMTConditionVisitor.ColumnScope
    ): SMTNode {
        val smtDefaultTableName = smtTableByOriginalName[defaultTableName.lowercase()]?.smtName
            ?: convertToAscii(defaultTableName)
        val visitor = SMTConditionVisitor(smtDefaultTableName, tableAliases, schema.tables, index, columnScope)
        return condition.accept(visitor, null) as SMTNode
    }

    /**
     * Builds the [SMTConditionVisitor.ColumnScope] for one query from the aliases its `FROM` and
     * `JOIN` items declare.
     *
     * Asking the schema whether a qualifier names a table answers by absence, and gets one shape
     * wrong: a sub-select aliased with the name of a real table passes the check, and the generator
     * emits a constraint selecting from that table's row a column it may not even have. Asking the
     * query's own `FROM` first settles it, because the alias is declared right there.
     *
     * The scope only ever answers "derived" or "unknown". A qualifier it does not recognise falls
     * through to the schema check, so this can drop conditions the previous behaviour kept but never
     * the other way round.
     */
    private fun columnScopeOf(derivedAliases: Set<String>) =
        SMTConditionVisitor.ColumnScope { qualifier, _ ->
            if (qualifier != null && derivedAliases.any { it.equals(qualifier, ignoreCase = true) })
                true
            else
                null
        }

    /**
     * Extracts table aliases from the SQL query.
     *
     * @param sqlQuery The SQL query from which aliases are extracted.
     * @return A map of table aliases.
     */
    private fun extractTableAliases(sqlQuery: Statement): Map<String, String> =
        extractFromScope(sqlQuery).tableAliases

    /**
     * What the `FROM` and `JOIN` items of a query declare: the aliases that stand for a schema table,
     * and the aliases that stand for a derived one.
     */
    private data class FromScope(
        val tableAliases: Map<String, String>,
        val derivedAliases: Set<String>
    )

    /**
     * Reads the aliases a query declares.
     *
     * A FROM item with no schema table behind it is a derived table. Its alias is recorded rather
     * than dropped, because that alias is the only place the query says so: matching it against the
     * schema instead would call it a real table whenever the two happen to share a name.
     */
    private fun extractFromScope(sqlQuery: Statement): FromScope {
        val tableAliasMap = mutableMapOf<String, String>()
        val derivedAliases = mutableSetOf<String>()

        when (sqlQuery) {
            is Select -> {
                val plainSelect = sqlQuery.selectBody as PlainSelect
                val fromItem = plainSelect.fromItem
                if (fromItem != null) {
                    val tableName = getTableName(fromItem)
                    if (tableName != null) {
                        tableAliasMap[fromItem.alias?.name ?: tableName] = tableName
                    } else {
                        fromItem.alias?.name?.let { derivedAliases.add(it) }
                    }

                    val joins = plainSelect.joins
                    if (joins != null) {
                        for (join in joins) {
                            val rightItem = join.rightItem
                            val joinName = getTableName(rightItem)
                            if (joinName != null) {
                                tableAliasMap[rightItem.alias?.name ?: rightItem.toString()] = joinName
                            } else {
                                rightItem.alias?.name?.let { derivedAliases.add(it) }
                            }
                        }
                    }
                }
            }
            is Delete -> {
                val tableName = sqlQuery.table.getName()
                val alias = sqlQuery.table.alias?.name ?: tableName
                tableAliasMap[alias] = tableName
            }
            is Update -> {
                val tableName = sqlQuery.table.getName()
                val alias = sqlQuery.table.alias?.name ?: tableName
                tableAliasMap[alias] = tableName
            }
        }
        return FromScope(tableAliasMap, derivedAliases)
    }

    /**
     * The schema table behind a FROM item, or null when there is none — a parenthesised sub-select,
     * which an ORM emits to resolve entity inheritance through UNION ALL.
     *
     * This used to be an unchecked cast to [Table]. The resulting `ClassCastException` was caught far
     * upstream, where it discarded the *whole* query rather than the one FROM item that could not be
     * mapped — so a query joining a real table against a derived one lost its real constraints too.
     * On one system under test that single shape accounted for every SMT-LIB generation failure it
     * reported, consuming close to half the search budget to produce nothing.
     */
    private fun getTableName(fromItem: FromItem?): String? =
        (fromItem as? Table)?.getName()

    /**
     * Appends value checking constraints to the SMT-LIB only from the tables mentioned in the select
     *
     * @param smt The SMT-LIB object to which value checking constraints are added.
     */
    private fun appendGetValuesFromQuery(smt: SMTLib, sqlQuery: Statement) {
        smt.addNode(CheckSatSMTNode())

        // Find the tables mentioned in the query
        val tablesMentioned = mutableSetOf<String>()
        val tablesFinder = TablesNamesFinder()

        // Add tables from the FROM clause
        val tables = try {
            tablesFinder.getTables(sqlQuery)
        } catch (e: Exception) {
            // This is because the jsqlParser does not support visit(Execute execute) {
            //        throw new UnsupportedOperationException(NOT_SUPPORTED_YET); }
            // https://github.com/JSQLParser/JSqlParser/blob/484eaa1c0f623cc67f8bf324e4367f8474eb77f1/src/main/java/net/sf/jsqlparser/util/TablesNamesFinder.java#L1180
            LoggingUtil.getInfoLogger().error("Failed to find tables: ${e.message}")
            emptySet<String>()
        }

        for (tableName in tables){
            tablesMentioned.add(tableName.lowercase())
        }

        // Add tables from JOINs and WHERE clause if they exist
        if (sqlQuery is Select) {
            val plainSelect = sqlQuery.selectBody as PlainSelect

            // Add tables from JOINs
            plainSelect.joins?.forEach { join ->
                join.rightItem?.let {
                    tablesMentioned.add(it.toString().lowercase())
                }
            }

            // Add tables from WHERE clause
            if (plainSelect.where != null) {
                for (tableName in TablesNamesFinder().getTables(sqlQuery as Statement)) {
                    tablesMentioned.add(tableName.lowercase())
                }
            }
        }

        // Only add GetValueSMTNode for the mentioned tables
        for (smtTable in smtTables) {
            if (tablesMentioned.contains(smtTable.originalName)) {
                for (i in 1..numberOfRows) {
                    smt.addNode(GetValueSMTNode(rowConstantName(smtTable.smtName, i)))
                }
            }
        }
    }

    /**
     * Gets the constructors for a table's columns to be used in SMT-LIB.
     *
     * @param smtTable The table for which constructors are generated.
     * @return A list of SMT nodes for column constructors.
     */
    private fun getConstructors(smtTable: SmtTable): List<DeclareConstSMTNode> {
        return smtTable.dto.columns.map { c ->
            val smtType = TYPE_MAP[c.type.uppercase()]
                ?: throw RuntimeException("Unsupported column type: ${c.type}")
            DeclareConstSMTNode(smtTable.smtColumnName(c.name), smtType)
        }
    }
}
