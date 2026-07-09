package org.evomaster.core.solver

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
                smt.addNode(DeclareConstSMTNode("${smtTable.smtName}$i", smtTable.dataTypeName))
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
                                        EqualsAssertion(listOf("($columnName ${smtTable.smtName}$i)", "\"$literal\""))
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
                                    "($columnName ${smtTable.smtName}$i)",
                                    TIMESTAMP_EPOCH_LOWER_BOUND.toString()
                                )
                            )
                        )
                        smt.addNode(
                            AssertSMTNode(
                                LessThanOrEqualsAssertion(
                                    "($columnName ${smtTable.smtName}$i)",
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
                                "(${pkSelector.uppercase()} $tableName$i)",
                                "(${pkSelector.uppercase()} $tableName$j)"
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
                        "(${selector.uppercase()} $tableName$i)",
                        "(${selector.uppercase()} $tableName$j)"
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

            // KNOWN LIMITATION: composite foreign keys are not fully supported. Each source column is
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
                        "(${sourceColumnSelector.uppercase()} $sourceTableName$i)",
                        "(${referencedColumnSelector.uppercase()} $referencedTableName$j)"
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
        val tableAliases = extractTableAliases(sqlQuery)

        appendJoinConstraints(smt, sqlQuery, tableAliases)

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
            try {
                val condition = parser.parse(where.toString(), toDBType(schema.databaseType))
                for (i in 1..numberOfRows) {
                    val constraint = parseQueryCondition(tableAliases, defaultTable, condition, i)
                    smt.addNode(constraint)
                }
            } catch (e: RuntimeException) {
                skippedQueryConstraints++
                LoggingUtil.getInfoLogger().warn("Could not translate WHERE clause to SMT-LIB, skipping: ${where}. Reason: ${e.message}")
            }
        }
    }

    /**
     * Appends join constraints to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which join constraints are added.
     * @param sqlQuery The SQL query containing join constraints.
     * @param tableAliases The map of table aliases.
     */
    private fun appendJoinConstraints(smt: SMTLib, sqlQuery: Statement, tableAliases: Map<String, String>) {
        if (sqlQuery is Select) { // TODO: Handle other queries
            val plainSelect = sqlQuery.selectBody as PlainSelect
            val joins = plainSelect.joins
            if (joins != null) {
                for (join in joins) {
                    val onExpressions = join.onExpressions
                    if (onExpressions.isNotEmpty()) {
                        // KNOWN LIMITATION: only the first ON expression is used; a composite ON
                        // (e.g. "a = b AND c = d") drops all but the first conjunct.
                        val onExpression = onExpressions.elementAt(0)
                        try {
                            val condition = parser.parse(onExpression.toString(), toDBType(schema.databaseType))
                            val tableFromQuery = TablesNamesFinder().getTables(sqlQuery as Statement).first()
                            // KNOWN LIMITATION: the ON condition is translated with the SAME row index on
                            // both sides ("diagonal pairing"): row i of one table is matched only with row i
                            // of the other. This is sufficient at the default numberOfRows=1 to force a
                            // non-empty JOIN, but it does not model full INNER JOIN semantics: for
                            // numberOfRows>=2 it never explores mismatched-index pairs (e.g. users2 with
                            // products1). Matching arbitrary row combinations is future work.
                            for (i in 1..numberOfRows) {
                                val constraint = parseQueryCondition(tableAliases, tableFromQuery, condition, i)
                                smt.addNode(constraint)
                            }
                        } catch (e: RuntimeException) {
                            skippedQueryConstraints++
                            LoggingUtil.getInfoLogger().warn("Could not translate JOIN ON clause to SMT-LIB, skipping: ${onExpression}. Reason: ${e.message}")
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
    private fun parseQueryCondition(tableAliases: Map<String, String>, defaultTableName: String, condition: SqlCondition, index: Int): SMTNode {
        val smtDefaultTableName = smtTableByOriginalName[defaultTableName.lowercase()]?.smtName
            ?: convertToAscii(defaultTableName)
        val visitor = SMTConditionVisitor(smtDefaultTableName, tableAliases, schema.tables, index)
        return condition.accept(visitor, null) as SMTNode
    }

    /**
     * Extracts table aliases from the SQL query.
     *
     * @param sqlQuery The SQL query from which aliases are extracted.
     * @return A map of table aliases.
     */
    private fun extractTableAliases(sqlQuery: Statement): Map<String, String> {
        val tableAliasMap = mutableMapOf<String, String>()
        when (sqlQuery) {
            is Select -> {
                val plainSelect = sqlQuery.selectBody as PlainSelect
                val fromItem = plainSelect.fromItem
                if (fromItem != null) {
                    val tableName = getTableName(fromItem)
                    val alias = fromItem.alias?.name ?: tableName
                    tableAliasMap[alias] = tableName

                    val joins = plainSelect.joins
                    if (joins != null) {
                        for (join in joins) {
                            val joinAlias = join.rightItem.alias?.name ?: join.rightItem.toString()
                            val joinName = getTableName(join.rightItem)
                            tableAliasMap[joinAlias] = joinName
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
        return tableAliasMap
    }

    private fun getTableName(fromItem: FromItem?): String =
        (fromItem as Table).getName()

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
                    smt.addNode(GetValueSMTNode("${smtTable.smtName}$i"))
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

    companion object {

        // Bounds for TIMESTAMP columns, encoded as epoch seconds (SMT Int).
        private const val TIMESTAMP_EPOCH_LOWER_BOUND = 0L            // Unix epoch start
        private const val TIMESTAMP_EPOCH_UPPER_BOUND = 32503680000L  // ~year 3000, in seconds

        // SMT-LIB sorts used as TYPE_MAP targets.
        private const val SMT_INT = "Int"
        private const val SMT_REAL = "Real"
        private const val SMT_STRING = "String"

        // SQL type names that need special interpretation beyond their SMT sort: BOOLEAN is encoded as an
        // SMT String and TIMESTAMP as an SMT Int, so gene reconstruction must consult the original type.
        // Shared with the comparison sites in this class and referenced by SMTLibZ3DbConstraintSolver.
        const val BOOLEAN_TYPE = "BOOLEAN"
        const val TIMESTAMP_TYPE = "TIMESTAMP"

        // The string values a BOOLEAN column may take (BOOLEAN is encoded as an SMT String).
        private val BOOLEAN_LITERALS = listOf("true", "True", "TRUE", "false", "False", "FALSE")

        // Maps database column types to SMT-LIB types.
        // FUTURE WORK: this is one of three independent type vocabularies interpreting ColumnDto.type
        // (the others are SMTLibZ3DbConstraintSolver.getColumnDataType and .hasColumnType). They can
        // silently disagree when a backend reports a variant spelling; consolidating them into a single
        // source of truth is future work (see the note on SMTLibZ3DbConstraintSolver.hasColumnType).
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
            // KNOWN LIMITATION: NUMERIC is mapped to Int, so any fractional part is truncated. This is
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
}
