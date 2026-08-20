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
 */
class SMTConditionVisitor(
    private val defaultTableName: String,
    private val tableAliases: Map<String, String>,
    private val tables: List<TableDto>,
    private val rowIndex: Int
) : SqlConditionVisitor<SMTNode, Void> {

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
                /*
                    An unquoted `true` or `false` in a WHERE clause arrives here as a column, not as a
                    SqlBooleanLiteralValue: the constraint parser only produces that type for CHECK
                    expressions, and to the grammar a boolean literal and an identifier are
                    indistinguishable in this position. Left as a column, the name is emitted as a
                    field selector over the row constant, and Z3 rejects the entire formula with
                    "unknown constant TRUE" — a full round-trip spent to learn nothing.

                    Booleans are encoded as SMT strings, so the literal takes the same spelling the
                    constraint path uses, which is the one SMTLibZ3DbConstraintSolver.toBoolean reads
                    back. Only unqualified names are considered, since a qualified one is necessarily
                    a column reference, and only when no table in the schema declares a column with
                    that name.
                 */
                val name = sqlCondition.columnName
                if (sqlCondition.tableName == null && isBooleanLiteral(name) && !isAColumn(name)) {
                    return if (name.equals("true", ignoreCase = true)) "\"True\"" else "\"False\""
                }

                val tableName = sqlCondition.tableName?.let {
                    tableAliases[it] ?: it
                } ?: defaultTableName

                /*
                    A qualifier that resolves to no schema table means the column belongs to a derived
                    table — a sub-select in FROM or JOIN, which has no declared SMT constant to refer
                    to. Emitting a reference anyway produced a formula Z3 rejects outright
                    ("unknown constant"), costing a full round-trip to learn nothing. Throwing instead
                    lets the caller drop the clause and record it as a partial translation, leaving
                    the schema-level constraints of the query intact.

                    Only qualified columns are checked. An unqualified one resolves to the default
                    table, which is a schema table by construction, and the name it arrives under is
                    the ASCII-folded one — comparing that against the schema's own spelling would
                    reject a valid table whose name is not ASCII, quietly dropping its conditions.
                 */
                if (sqlCondition.tableName != null &&
                    tables.none { it.id.name.equals(tableName, ignoreCase = true) }) {
                    throw RuntimeException(
                        "Column '${sqlCondition.columnName}' is qualified by '$tableName', which is not" +
                            " a table in the schema (most likely a derived table)"
                    )
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
    private fun isBooleanLiteral(operand: String): Boolean =
        operand.equals("true", ignoreCase = true) || operand.equals("false", ignoreCase = true)

    /**
     * Whether any table in the schema declares a column with this name.
     *
     * Deliberately broader than the resolution rules for an unqualified column, which would look only
     * at the tables in scope. The two possible mistakes are not symmetric: treating a real column as a
     * boolean literal yields data that is silently wrong, while declining to treat a genuine literal as
     * one yields a formula Z3 rejects — visible, counted, and no worse than the behaviour before the
     * literal was recognised at all. The broad check errs towards the second.
     *
     * It also avoids comparing a folded table name against the schema's own spelling, which is how the
     * scoped version would have to identify the tables in scope.
     */
    private fun isAColumn(operand: String): Boolean {
        return tables.any { table ->
            table.columns.any { column -> column.name.equals(operand, ignoreCase = true) }
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
