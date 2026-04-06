package org.evomaster.core.solver

import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.select.FromItem
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.util.TablesNamesFinder
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.sql.schema.ColumnDataType
import org.evomaster.core.sql.schema.ForeignKey
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
 * @param smtTables The tables in the schema, pre-wrapped with SMT-safe identifiers.
 * @param databaseType The database type, used to select the correct SQL dialect when parsing constraints.
 * @param numberOfRows The number of rows to be considered in constraints.
 */
class SmtLibGenerator(
    private val smtTables: List<SmtTable>,
    private val databaseType: DatabaseType,
    private val numberOfRows: Int
) {

    private val parser = JSqlConditionParser()

    private val smtTableByOriginalName: Map<String, SmtTable> = smtTables.associateBy { it.originalName }

    /** Flat list of domain [org.evomaster.core.sql.schema.Table] objects, passed to [SMTConditionVisitor]. */
    private val tables: List<org.evomaster.core.sql.schema.Table> = smtTables.map { it.table }

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
        for (column in smtTable.table.columns) {
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
        for (sqlExpression in smtTable.checkExpressions) {
            try {
                val condition: SqlCondition = parser.parse(sqlExpression, toDBType(databaseType))
                for (i in 1..numberOfRows) {
                    val constraint: SMTNode = parseCheckExpression(smtTable, condition, i)
                    smt.addNode(constraint)
                }
            } catch (e: SqlConditionParserException) {
                LoggingUtil.getInfoLogger().warn("Could not translate CHECK constraint to SMT-LIB, skipping: $sqlExpression. Reason: ${e.message}")
            } catch (e: JSQLParserException) {
                LoggingUtil.getInfoLogger().warn("Could not translate CHECK constraint to SMT-LIB, skipping: $sqlExpression. Reason: ${e.message}")
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
        val visitor = SMTConditionVisitor(smtTable.smtName, emptyMap(), tables, index)
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
            for (column in smtTable.table.columns) {
                if (column.type == ColumnDataType.BOOLEAN || column.type == ColumnDataType.BOOL) {
                    val columnName = smtTable.smtColumnName(column.name).uppercase()
                    for (i in 1..numberOfRows) {
                        smt.addNode(
                            AssertSMTNode(
                                OrAssertion(
                                    listOf(
                                        EqualsAssertion(listOf("($columnName ${smtTable.smtName}$i)", "\"true\"")),
                                        EqualsAssertion(listOf("($columnName ${smtTable.smtName}$i)", "\"True\"")),
                                        EqualsAssertion(listOf("($columnName ${smtTable.smtName}$i)", "\"TRUE\"")),
                                        EqualsAssertion(listOf("($columnName ${smtTable.smtName}$i)", "\"false\"")),
                                        EqualsAssertion(listOf("($columnName ${smtTable.smtName}$i)", "\"False\"")),
                                        EqualsAssertion(listOf("($columnName ${smtTable.smtName}$i)", "\"FALSE\""))
                                    )
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
            for (column in smtTable.table.columns) {
                if (column.type == ColumnDataType.TIMESTAMP) {
                    val columnName = smtTable.smtColumnName(column.name).uppercase()
                    val lowerBound = 0 // Example for Unix epoch start
                    val upperBound = 32503680000 // Example for year 3000 in seconds

                    for (i in 1..numberOfRows) {
                        smt.addNode(
                            AssertSMTNode(
                                GreaterThanOrEqualsAssertion(
                                    "($columnName ${smtTable.smtName}$i)",
                                    lowerBound.toString()
                                )
                            )
                        )
                        smt.addNode(
                            AssertSMTNode(
                                LessThanOrEqualsAssertion(
                                    "($columnName ${smtTable.smtName}$i)",
                                    upperBound.toString()
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
        for (primaryKey in smtTable.table.primaryKeys()) {
            val nodes = assertForDistinctField(smtTable.smtColumnName(primaryKey.name), smtTable.smtName)
            smt.addNodes(nodes)
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
     * Appends foreign key constraints for each table to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which foreign key constraints are added.
     * @param smtTable The table for which foreign key constraints are added.
     */
    private fun appendForeignKeyConstraints(smt: SMTLib, smtTable: SmtTable) {
        for (foreignKey in smtTable.table.foreignKeys) {
            val referencedSmtTable = findReferencedSmtTable(foreignKey)
            val referencedColumnSelector = referencedSmtTable.smtColumnName(
                findReferencedPKSelector(smtTable.table, referencedSmtTable.table, foreignKey)
            )

            for (sourceColumn in foreignKey.sourceColumns) {
                val nodes = assertForEqualsAny(
                    smtTable.smtColumnName(sourceColumn.name), smtTable.smtName,
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
     * Finds the primary key column name in the referenced table that is type-compatible with the source FK column.
     *
     * @param sourceTable The source table containing the foreign key.
     * @param referencedTable The referenced table.
     * @param foreignKey The foreign key constraint.
     * @return The primary key column name in the referenced table.
     */
    private fun findReferencedPKSelector(
        sourceTable: org.evomaster.core.sql.schema.Table,
        referencedTable: org.evomaster.core.sql.schema.Table,
        foreignKey: ForeignKey
    ): String {
        val referencedPrimaryKeys = referencedTable.primaryKeys()
        val sourceColumnName = foreignKey.sourceColumns.firstOrNull()?.name
        val sourceSmtType = sourceColumnName?.let { scn ->
            sourceTable.columns.firstOrNull { it.name.equals(scn, ignoreCase = true) }
                ?.let { TYPE_MAP[it.type] }
        }
        if (referencedPrimaryKeys.isNotEmpty() &&
            (sourceSmtType == null || TYPE_MAP[referencedPrimaryKeys[0].type] == sourceSmtType)) {
            return referencedPrimaryKeys[0].name
        }
        // No PK or type mismatch: find a type-compatible column
        if (sourceSmtType != null) {
            referencedTable.columns.firstOrNull { TYPE_MAP[it.type] == sourceSmtType }
                ?.let { return it.name }
        }
        return referencedTable.columns.firstOrNull()?.name
            ?: throw RuntimeException("Referenced table has no columns: ${foreignKey.targetTableId.name}")
    }

    /**
     * Finds the [SmtTable] for the table referenced by the given foreign key.
     *
     * @param foreignKey The foreign key constraint.
     * @return The referenced [SmtTable].
     */
    private fun findReferencedSmtTable(foreignKey: ForeignKey): SmtTable {
        return smtTableByOriginalName[foreignKey.targetTableId.name.lowercase()]
            ?: throw RuntimeException("Referenced table not found: ${foreignKey.targetTableId.name}")
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

        if (sqlQuery is Select) { // TODO: Handle other queries
            val plainSelect = sqlQuery.selectBody as PlainSelect
            val where = plainSelect.where

            if (where != null) {
                try {
                    val condition = parser.parse(where.toString(), toDBType(databaseType))
                    val tableFromQuery = TablesNamesFinder().getTables(sqlQuery as Statement).first()
                    for (i in 1..numberOfRows) {
                        val constraint = parseQueryCondition(tableAliases, tableFromQuery, condition, i)
                        smt.addNode(constraint)
                    }
                } catch (e: RuntimeException) {
                    LoggingUtil.getInfoLogger().warn("Could not translate WHERE clause to SMT-LIB, skipping: ${where}. Reason: ${e.message}")
                }
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
                        val onExpression = onExpressions.elementAt(0)
                        try {
                            val condition = parser.parse(onExpression.toString(), toDBType(databaseType))
                            val tableFromQuery = TablesNamesFinder().getTables(sqlQuery as Statement).first()
                            for (i in 1..numberOfRows) {
                                val constraint = parseQueryCondition(tableAliases, tableFromQuery, condition, i)
                                smt.addNode(constraint)
                            }
                        } catch (e: RuntimeException) {
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
        val visitor = SMTConditionVisitor(smtDefaultTableName, tableAliases, tables, index)
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
        if (sqlQuery is Select) { // TODO: Handle other queries
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
        val queryTables = try {
            tablesFinder.getTables(sqlQuery)
        } catch (e: Exception) {
            // This is because the jsqlParser does not support visit(Execute execute) {
            //        throw new UnsupportedOperationException(NOT_SUPPORTED_YET); }
            // https://github.com/JSQLParser/JSqlParser/blob/484eaa1c0f623cc67f8bf324e4367f8474eb77f1/src/main/java/net/sf/jsqlparser/util/TablesNamesFinder.java#L1180
            LoggingUtil.getInfoLogger().error("Failed to find tables: ${e.message}")
            emptySet<String>()
        }

        for (tableName in queryTables){
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
        return smtTable.table.columns.map { c ->
            val smtType = TYPE_MAP[c.type]
                ?: throw RuntimeException("Unsupported column type: ${c.type}")
            DeclareConstSMTNode(smtTable.smtColumnName(c.name), smtType)
        }
    }

    companion object {

        // Maps domain column types to SMT-LIB types
        private val TYPE_MAP = mapOf(
            ColumnDataType.BIGINT to "Int",
            ColumnDataType.BIT to "Int",
            ColumnDataType.INTEGER to "Int",
            ColumnDataType.INT to "Int",
            ColumnDataType.INT2 to "Int",
            ColumnDataType.INT4 to "Int",
            ColumnDataType.INT8 to "Int",
            ColumnDataType.TINYINT to "Int",
            ColumnDataType.SMALLINT to "Int",
            ColumnDataType.NUMERIC to "Int",
            ColumnDataType.SERIAL to "Int",
            ColumnDataType.SMALLSERIAL to "Int",
            ColumnDataType.BIGSERIAL to "Int",
            ColumnDataType.TIMESTAMP to "Int",
            ColumnDataType.DATE to "Int",
            ColumnDataType.FLOAT to "Real",
            ColumnDataType.DOUBLE to "Real",
            ColumnDataType.DECIMAL to "Real",
            ColumnDataType.REAL to "Real",
            ColumnDataType.CHARACTER_VARYING to "String",
            ColumnDataType.CHAR to "String",
            ColumnDataType.VARCHAR to "String",
            ColumnDataType.TEXT to "String",
            ColumnDataType.CHARACTER_LARGE_OBJECT to "String",
            ColumnDataType.BOOLEAN to "String",
            ColumnDataType.BOOL to "String",
            ColumnDataType.UUID to "String",
            ColumnDataType.JSONB to "String",
            ColumnDataType.BYTEA to "String",
        )
    }
}
