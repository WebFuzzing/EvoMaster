package org.evomaster.core.solver

import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.select.FromItem
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.util.TablesNamesFinder
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.ForeignKeyDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.utils.StringUtils
import org.evomaster.dbconstraint.ConstraintDatabaseType
import org.evomaster.dbconstraint.ast.SqlCondition
import org.evomaster.dbconstraint.parser.SqlConditionParserException
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser
import org.evomaster.solver.smtlib.*
import org.evomaster.solver.smtlib.assertion.*
import java.util.*

/**
 * Generates SMT-LIB constraints from SQL queries and schema definitions.
 *
 * @param schema The database schema containing tables and constraints.
 * @param numberOfRows The number of rows to be considered in constraints.
 */
class SmtLibGenerator(private val schema: DbInfoDto, private val numberOfRows: Int) {

    private var parser = JSqlConditionParser()

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
        for (table in schema.tables) {
            val dataTypeName = "${StringUtils.capitalization(table.name)}Row"

            // Declare datatype for the table
            smt.addNode(
                DeclareDatatypeSMTNode(dataTypeName, getConstructors(table))
            )

            // Declare constants for each row
            for (i in 1..numberOfRows) {
                smt.addNode(
                    DeclareConstSMTNode("${table.name.lowercase(Locale.getDefault())}$i", dataTypeName)
                )
            }
        }
    }

    /**
     * Appends table constraints (unique and check constraints) to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which table constraints are added.
     */
    private fun appendTableConstraints(smt: SMTLib) {
        for (table in schema.tables) {
            appendUniqueConstraints(smt, table)
            appendCheckConstraints(smt, table)
        }
    }

    /**
     * Appends unique constraints for each table to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which unique constraints are added.
     * @param table The table for which unique constraints are added.
     */
    private fun appendUniqueConstraints(smt: SMTLib, table: TableDto) {
        val tableName = table.name.lowercase(Locale.getDefault())
        for (column in table.columns) {
            if (column.unique) {
                val nodes = assertForDistinctField(column.name, tableName)
                smt.addNodes(nodes)
            }
        }
    }

    /**
     * Appends check constraints for each table to the SMT-LIB.
     *
     * @param smt The SMT-LIB object to which check constraints are added.
     * @param table The table for which check constraints are added.
     */
    private fun appendCheckConstraints(smt: SMTLib, table: TableDto) {
        for (check in table.tableCheckExpressions) {
            try {
                val condition: SqlCondition = parser.parse(check.sqlCheckExpression, toDBType(schema.databaseType))
                for (i in 1..numberOfRows) {
                    val constraint: SMTNode = parseCheckExpression(table, condition, i)
                    smt.addNode(constraint)
                }
            } catch (e: SqlConditionParserException) {
                throw RuntimeException("Error parsing check expression: " + check.sqlCheckExpression, e)
            }
        }
    }

    /**
     * Parses a check expression and returns the corresponding SMT node.
     *
     * @param table The table containing the check expression.
     * @param condition The SQL condition to be parsed.
     * @param index The index of the row.
     * @return The corresponding SMT node.
     */
    private fun parseCheckExpression(table: TableDto, condition: SqlCondition, index: Int): SMTNode {
        val visitor = SMTConditionVisitor(table.name.lowercase(Locale.getDefault()), emptyMap(), schema.tables, index)
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
        for (table in schema.tables) {
            appendPrimaryKeyConstraints(smt, table)
            appendForeignKeyConstraints(smt, table)
        }
    }

    private fun appendBooleanConstraints(smt: SMTLib) {
        for (table in schema.tables) {
            val tableName = table.name.lowercase(Locale.getDefault())
            for (column in table.columns) {
                if (column.type.equals("BOOLEAN", ignoreCase = true)) {
                    val columnName = column.name.uppercase()
                    for (i in 1..numberOfRows) {
                        smt.addNode(
                            AssertSMTNode(
                                OrAssertion(
                                    listOf(
                                        EqualsAssertion(listOf("($columnName $tableName$i)", "\"true\"")),
                                        EqualsAssertion(listOf("($columnName $tableName$i)", "\"True\"")),
                                        EqualsAssertion(listOf("($columnName $tableName$i)", "\"TRUE\"")),
                                        EqualsAssertion(listOf("($columnName $tableName$i)", "\"false\"")),
                                        EqualsAssertion(listOf("($columnName $tableName$i)", "\"False\"")),
                                        EqualsAssertion(listOf("($columnName $tableName$i)", "\"FALSE\""))
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
        for (table in schema.tables) {
            val tableName = table.name.lowercase(Locale.getDefault())
            for (column in table.columns) {
                if (column.type.equals("TIMESTAMP", ignoreCase = true)) {
                    val columnName = column.name.uppercase()
                    val lowerBound = 0 // Example for Unix epoch start
                    val upperBound = 32503680000 // Example for year 3000 in seconds

                    for (i in 1..numberOfRows) {
                        smt.addNode(
                            AssertSMTNode(
                                GreaterThanOrEqualsAssertion(
                                    "($columnName $tableName$i)",
                                    lowerBound.toString()
                                )
                            )
                        )
                        smt.addNode(
                            AssertSMTNode(
                                LessThanOrEqualsAssertion(
                                    "($columnName $tableName$i)",
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
     * @param table The table for which primary key constraints are added.
     */
    private fun appendPrimaryKeyConstraints(smt: SMTLib, table: TableDto) {
        val tableName = table.name.lowercase(Locale.getDefault())
        val primaryKeys = table.columns.filter { it.primaryKey }

        for (primaryKey in primaryKeys) {
            val nodes = assertForDistinctField(primaryKey.name, tableName)
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
                                "(${pkSelector.uppercase(Locale.getDefault())} $tableName$i)",
                                "(${pkSelector.uppercase(Locale.getDefault())} $tableName$j)"
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
     * @param table The table for which foreign key constraints are added.
     */
    private fun appendForeignKeyConstraints(smt: SMTLib, table: TableDto) {
        val sourceTableName = table.name.lowercase(Locale.getDefault())

        for (foreignKey in table.foreignKeys) {
            val referencedTable = findReferencedTable(foreignKey)
            val referencedTableName = referencedTable.name.lowercase(Locale.getDefault())
            val referencedColumnSelector = findReferencedPKSelector(referencedTable, foreignKey)

            for (sourceColumn in foreignKey.sourceColumns) {
                val nodes = assertForEqualsAny(
                    sourceColumn, sourceTableName,
                    referencedColumnSelector, referencedTableName
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
                        "(${sourceColumnSelector.uppercase(Locale.getDefault())} $sourceTableName$i)",
                        "(${referencedColumnSelector.uppercase(Locale.getDefault())} $referencedTableName$j)"
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
    private fun findReferencedPKSelector(referencedTable: TableDto, foreignKey: ForeignKeyDto): String {
        val referencedPrimaryKeys = referencedTable.columns.filter { it.primaryKey }
        if (referencedPrimaryKeys.isEmpty()) {
            throw RuntimeException("Referenced table has no primary key: ${foreignKey.targetTable}")
        }
        // Assuming single-column primary keys
        return referencedPrimaryKeys[0].name
    }

    /**
     * Finds the referenced table based on the foreign key.
     *
     * @param foreignKey The foreign key constraint.
     * @return The referenced table.
     */
    private fun findReferencedTable(foreignKey: ForeignKeyDto): TableDto {
        return schema.tables.firstOrNull { it.name.equals(foreignKey.targetTable, ignoreCase = true) }
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

        if (sqlQuery is Select) { // TODO: Handle other queries
            val plainSelect = sqlQuery.selectBody as PlainSelect
            val where = plainSelect.where

            if (where != null) {
                val condition = parser.parse(where.toString(), toDBType(schema.databaseType))
                val tableFromQuery = TablesNamesFinder().getTables(sqlQuery as Statement).first()
                for (i in 1..numberOfRows) {
                    val constraint = parseQueryCondition(tableAliases, tableFromQuery, condition, i)
                    smt.addNode(constraint)
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
                    val onExpression = join.onExpression
                    if (onExpression != null) {
                        val condition = parser.parse(onExpression.toString(), toDBType(schema.databaseType))
                        val tableFromQuery = TablesNamesFinder().getTables(sqlQuery as Statement).first()
                        for (i in 1..numberOfRows) {
                            val constraint = parseQueryCondition(tableAliases, tableFromQuery, condition, i)
                            smt.addNode(constraint)
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
        val visitor = SMTConditionVisitor(defaultTableName, tableAliases, schema.tables, index)
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
        for (tableName in tablesFinder.getTables(sqlQuery)){
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
        for (table in schema.tables) {
            val tableNameLower = table.name.lowercase(Locale.getDefault())
            if (tablesMentioned.contains(tableNameLower)) {
                for (i in 1..numberOfRows) {
                    smt.addNode(GetValueSMTNode("$tableNameLower$i"))
                }
            }
        }
    }

    /**
     * Gets the constructors for a table's columns to be used in SMT-LIB.
     *
     * @param table The table for which constructors are generated.
     * @return A list of SMT nodes for column constructors.
     */
    private fun getConstructors(table: TableDto): List<DeclareConstSMTNode> {
        return table.columns.map { c ->
            val smtType = TYPE_MAP[c.type.uppercase(Locale.getDefault())]
                ?: throw RuntimeException("Unsupported column type: ${c.type}")
            DeclareConstSMTNode(c.name, smtType)
        }
    }

    companion object {
        // Maps database column types to SMT-LIB types
        private val TYPE_MAP = mapOf(
            "BIGINT" to "Int",
            "INTEGER" to "Int",
            "TIMESTAMP" to "Int",
            "FLOAT" to "Real",
            "DOUBLE" to "Real",
            "DECIMAL" to "Real",
            "REAL" to "Real",
            "CHARACTER VARYING" to "String",
            "CHAR" to "String",
            "VARCHAR" to "String",
            "CHARACTER LARGE OBJECT" to "String",
            "BOOLEAN" to "String", // TODO: Check this
        )
    }
}
