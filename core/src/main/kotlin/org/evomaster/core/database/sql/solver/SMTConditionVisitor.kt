package org.evomaster.core.database.sql.solver

import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.core.utils.StringUtils.convertToAscii
import org.evomaster.dbconstraint.ast.*
import org.evomaster.solver.smtlib.AssertSMTNode
import org.evomaster.solver.smtlib.EmptySMTNode
import org.evomaster.solver.smtlib.SMTNode
import org.evomaster.solver.smtlib.assertion.*

/**
 * Converts SQL conditions into SMT nodes for constraint solving.
 *
 * @param defaultTableName The name of the default table used when only one table is involved.
 * @param tableAliases A map of table aliases to their actual table names, used to resolve column references.
 * @param tables A list of table definitions, used to determine if an operand is a column.
 * @param rowIndex The index of the row to be used in SMT-LIB variable declarations.
 * @param columnScope Decides whether a column reference belongs to a derived table. See [ColumnScope].
 */
class SMTConditionVisitor(
    private val defaultTableName: String,
    private val tableAliases: Map<String, String>,
    private val tables: List<TableDto>,
    private val rowIndex: Int,
    private val columnScope: ColumnScope = ColumnScope.UNRESOLVED
) : SqlConditionVisitor<SMTNode, Void> {

    /**
     * Answers, for a column reference appearing in a WHERE or ON clause, whether it belongs to a
     * derived table -- a sub-select in FROM or JOIN, a CTE, a lateral join. Such a column has no
     * declared row constant to select from, so it cannot be expressed in SMT-LIB.
     *
     * This is kept as an interface so that the visitor does not depend on how the answer is
     * obtained. [SmtLibGenerator] supplies an implementation built from the aliases the query's
     * `FROM` and `JOIN` items declare; [UNRESOLVED] is the fallback used where no query context
     * exists, for instance when translating a CHECK constraint, which has no `FROM`.
     */
    fun interface ColumnScope {

        /**
         * @return `true` when the column is known to belong to a derived table, `false` when it is
         *         known to belong to a schema table, and `null` when it could not be resolved --
         *         in which case the caller falls back to matching the qualifier against the schema.
         */
        fun isDerived(qualifier: String?, columnName: String): Boolean?

        companion object {
            /** Resolves nothing; every decision falls back to the qualifier-against-schema check. */
            val UNRESOLVED = ColumnScope { _, _ -> null }
        }
    }

    /**
     * Constructs a column reference string for SMT-LIB from a table name and column name.
     *
     * Both names are converted to ASCII because SMT-LIB unquoted symbols only allow ASCII characters.
     * Table and column names may come directly from SQL query text (e.g., a WHERE clause), which can
     * contain non-ASCII characters if the schema uses them. The conversion must happen here because,
     * unlike schema-derived names that are pre-converted via [SmtTable], query-derived names are
     * parsed at runtime from raw SQL strings.
     *
     * @param tableName The name of the table.
     * @param columnName The name of the column.
     * @return The SMT-LIB column reference string.
     */
    private fun getColumnReference(tableName: String, columnName: String): String {
        val rowConstant = SmtLibGenerator.rowConstantName(convertToAscii(tableName).lowercase(), rowIndex)
        return "(${convertToAscii(columnName).uppercase()} $rowConstant)"
    }

    /**
     * Handles SQL AND conditions by converting them into an SMT node with an AND assertion.
     *
     * @param condition The SQL AND condition.
     * @param parameter Additional parameters (not used).
     * @return The corresponding SMT node.
     */
    override fun visit(condition: SqlAndCondition, parameter: Void?): SMTNode {
        val left = condition.leftExpr.accept(this, parameter)
        val right = condition.rightExpr.accept(this, parameter)
        if (left is EmptySMTNode && right is EmptySMTNode) return EmptySMTNode()
        if (left is EmptySMTNode) return right
        if (right is EmptySMTNode) return left
        return AssertSMTNode(AndAssertion(listOf((left as AssertSMTNode).assertion, (right as AssertSMTNode).assertion)))
    }

    /**
     * Handles SQL OR conditions by converting them into an SMT node with an OR assertion.
     *
     * @param condition The SQL OR condition.
     * @param parameter Additional parameters (not used).
     * @return The corresponding SMT node.
     */
    override fun visit(condition: SqlOrCondition, parameter: Void?): SMTNode {
        val conditions = condition.orConditions.map { it.accept(this, parameter) }
        val nonEmpty = conditions.filterIsInstance<AssertSMTNode>()
        if (nonEmpty.isEmpty()) return EmptySMTNode()
        if (nonEmpty.size == 1) return nonEmpty[0]
        return AssertSMTNode(OrAssertion(nonEmpty.map { it.assertion }))
    }

    /**
     * Handles SQL comparison conditions by converting them into an appropriate SMT assertion node.
     *
     * @param condition The SQL comparison condition.
     * @param parameter Additional parameters (not used).
     * @return The corresponding SMT node.
     */
    override fun visit(condition: SqlComparisonCondition, parameter: Void?): SMTNode {
        if (condition.leftOperand is SqlNullLiteralValue || condition.rightOperand is SqlNullLiteralValue) {
            return EmptySMTNode() // TODO: Change this when we add support for nullable columns in the db schema
        }
        val left = getVariableAndLiteral(condition.leftOperand)
        val right = getVariableAndLiteral(condition.rightOperand)

        return when (val comparator = getSMTComparator(condition.sqlComparisonOperator.toString())) {
            "=" -> AssertSMTNode(EqualsAssertion(listOf(left, right)))
            "distinct" -> AssertSMTNode(DistinctAssertion(listOf(left, right)))
            ">" -> AssertSMTNode(GreaterThanAssertion(left, right))
            ">=" -> AssertSMTNode(GreaterThanOrEqualsAssertion(left, right))
            "<" -> AssertSMTNode(LessThanAssertion(left, right))
            "<=" -> AssertSMTNode(LessThanOrEqualsAssertion(left, right))
            else -> throw IllegalArgumentException("Unsupported SQL comparator: $comparator")
        }
    }

    /**
     * Converts an operand to its corresponding SMT-LIB representation.
     *
     * @param sqlCondition The SqlCondition
     * @return The SMT-LIB representation of the operand.
     */
    private fun getVariableAndLiteral(sqlCondition: SqlCondition): String {
        return when (sqlCondition) {

            is SqlColumn -> {
                val tableName = sqlCondition.tableName?.let {
                    tableAliases[it] ?: it
                } ?: defaultTableName

                /*
                    A column of a derived table — a sub-select in FROM or JOIN, a CTE, a lateral
                    join — has no declared SMT constant to refer to. Emitting a reference anyway
                    produced a formula Z3 rejects outright ("unknown constant"), costing a full
                    round-trip to learn nothing. Throwing instead lets the caller drop the clause and
                    record it as a partial translation, leaving the schema-level constraints of the
                    query intact.

                    Who answers the question matters. [columnScope] resolves the reference against the
                    query itself, so it sees through aliases, unions and CTEs, and it also catches the
                    two shapes the fallback below cannot: an unqualified column that comes from a
                    derived table, and a derived table whose alias happens to spell a real schema
                    table — both of which would otherwise emit a constraint on the wrong rows.
                 */
                when (columnScope.isDerived(sqlCondition.tableName, sqlCondition.columnName)) {
                    true -> throw RuntimeException(
                        "Column '${sqlCondition.columnName}'" +
                            (sqlCondition.tableName?.let { " qualified by '$it'" } ?: "") +
                            " belongs to a derived table, which has no SMT-LIB representation"
                    )

                    false -> { /* a schema table: translatable */ }

                    /*
                        Not resolvable against the query. Fall back to matching the qualifier against
                        the schema, which is what this check did before the resolver was available.

                        Only qualified columns are checked here. An unqualified one resolves to the
                        default table, which is a schema table by construction, and the name it
                        arrives under is the ASCII-folded one — comparing that against the schema's
                        own spelling would reject a valid table whose name is not ASCII, quietly
                        dropping its conditions.
                     */
                    null -> if (sqlCondition.tableName != null &&
                        tables.none { it.id.name.equals(tableName, ignoreCase = true) }) {
                        throw RuntimeException(
                            "Column '${sqlCondition.columnName}' is qualified by '$tableName', which is not" +
                                " a table in the schema (most likely a derived table)"
                        )
                    }
                }

                getColumnReference(tableName, sqlCondition.columnName)
            }

            is SqlStringLiteralValue -> {
                var text = sqlCondition.toSql()
                if (text.startsWith("\'\"")) {
                    text = text.replace("\'\"", "")
                    text = text.replace("\"\'", "")
                } else if (text.startsWith("\'")) {
                    text = text.replace("\'", "")
                }
                "\"${text}\""
            }

            is SqlBigIntegerLiteralValue,
            is SqlBigDecimalLiteralValue -> {
                sqlCondition.toSql()
            }

            is SqlBooleanLiteralValue -> {
                if (sqlCondition.toString().equals("TRUE", ignoreCase = true) ) {
                    "\"True\""
                } else {
                    "\"False\""
                }
            }

            else -> {
                sqlCondition.toString()
            }
        }
    }

    /**
     * Checks if the operand is a column in the default table.
     *
     * @param operand The SQL operand as a string.
     * @return True if the operand is a column, false otherwise.
     */
    private fun isAColumn(operand: String): Boolean {
        return tables.any {
            it.id.name.equals(defaultTableName, ignoreCase = true) &&
                    it.columns.any { column -> column.name.equals(operand, ignoreCase = true) }
        }
    }

    /**
     * Maps SQL comparison operators to SMT-LIB comparators.
     *
     * @param sqlComparator The SQL comparison operator as a string.
     * @return The corresponding SMT-LIB comparator.
     */
    private fun getSMTComparator(sqlComparator: String): String {
        return when (sqlComparator) {
            "=" -> "="
            "<>", "!=" -> "distinct"
            ">" -> ">"
            ">=" -> ">="
            "<" -> "<"
            "<=" -> "<="
            else -> throw IllegalArgumentException("Unsupported SQL comparator: $sqlComparator")
        }
    }

    override fun visit(condition: SqlInCondition, parameter: Void?): SMTNode {
        val left = getVariableAndLiteral(condition.sqlColumn)
        val conditions = condition.literalList.sqlConditionExpressions
            .map {
                AssertSMTNode(EqualsAssertion(listOf(left, asLiteral(it))))
            }
        return if (conditions.size == 1) {
            conditions[0]
        } else {
            AssertSMTNode(OrAssertion(conditions.map { it.assertion }))
        }
    }

    private fun asLiteral(expression: SqlCondition?): String {
        if (expression is SqlStringLiteralValue) {
            return expression.toString().replace("'", "\"")
        } else if (expression is SqlBigDecimalLiteralValue) {
            return expression.toString()
        } else if (expression is SqlBigIntegerLiteralValue) {
            return expression.toString()
        } else if (expression is SqlBooleanLiteralValue) {
            return expression.toString()
        } else if (expression is SqlBinaryDataLiteralValue) {
            return expression.toString()
        } else {
            throw IllegalArgumentException(
                "Unsupported literal type: ${
                    expression?.javaClass?.simpleName
                        ?: "null"
                }"
            )
        }
    }

    override fun visit(condition: SqlIsNotNullCondition, parameter: Void?): SMTNode {
        return EmptySMTNode()
    }

    // Placeholder methods for other SQL conditions; to be implemented as needed
    override fun visit(condition: SqlBigDecimalLiteralValue, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlBigIntegerLiteralValue, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlBooleanLiteralValue, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlColumn, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlNullLiteralValue, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlStringLiteralValue, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlConditionList, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlBinaryDataLiteralValue, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlSimilarToCondition, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlIsNullCondition, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }

    override fun visit(condition: SqlLikeCondition, parameter: Void?): SMTNode {
        return EmptySMTNode() // TODO: implement
    }
}
