package org.evomaster.core.solver

import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.dbconstraint.ast.*
import org.evomaster.solver.smtlib.AssertSMTNode
import org.evomaster.solver.smtlib.EmptySMTNode
import org.evomaster.solver.smtlib.SMTNode
import org.evomaster.solver.smtlib.assertion.*
import java.util.*

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
     * @param tableName The name of the table.
     * @param columnName The name of the column.
     * @return The SMT-LIB column reference string.
     */
    private fun getColumnReference(tableName: String, columnName: String): String {
        return "(${columnName.uppercase(Locale.getDefault())} ${tableName.lowercase(Locale.getDefault())}$rowIndex)"
    }

    /**
     * Handles SQL AND conditions by converting them into an SMT node with an AND assertion.
     *
     * @param condition The SQL AND condition.
     * @param parameter Additional parameters (not used).
     * @return The corresponding SMT node.
     */
    override fun visit(condition: SqlAndCondition, parameter: Void?): SMTNode {
        val left = condition.leftExpr.accept(this, parameter) as AssertSMTNode
        val right = condition.rightExpr.accept(this, parameter) as AssertSMTNode
        return AssertSMTNode(AndAssertion(listOf(left.assertion, right.assertion)))
    }

    /**
     * Handles SQL OR conditions by converting them into an SMT node with an OR assertion.
     *
     * @param condition The SQL OR condition.
     * @param parameter Additional parameters (not used).
     * @return The corresponding SMT node.
     */
    override fun visit(condition: SqlOrCondition, parameter: Void?): SMTNode {
        val conditions = condition.orConditions.map { it.accept(this, parameter) as AssertSMTNode }
        return AssertSMTNode(OrAssertion(conditions.map { it.assertion }))
    }

    /**
     * Handles SQL comparison conditions by converting them into an appropriate SMT assertion node.
     *
     * @param condition The SQL comparison condition.
     * @param parameter Additional parameters (not used).
     * @return The corresponding SMT node.
     */
    override fun visit(condition: SqlComparisonCondition, parameter: Void?): SMTNode {
        val left = getVariableAndLiteral(condition.leftOperand.toString())
        val right = getVariableAndLiteral(condition.rightOperand.toString())

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
     * @param operand The SQL operand as a string.
     * @return The SMT-LIB representation of the operand.
     */
    private fun getVariableAndLiteral(operand: String): String {
        return when {
            operand.contains(".") -> { // Handle column references with aliases
                val parts = operand.split(".")
                if (tableAliases.containsKey(parts[0])) {
                    val tableName = tableAliases[parts[0]] ?: defaultTableName
                    val columnName = parts[parts.lastIndex]
                    getColumnReference(tableName, columnName)
                } else {
                    operand
                }
            }
            isAColumn(operand) -> { // Handle direct column references
                getColumnReference(defaultTableName, operand)
            }
            operand.startsWith("'") && operand.endsWith("'") -> { // Handle string literals
                operand.replace("'", "\"")
            }
            operand.equals("TRUE", ignoreCase = true) -> "\"True\""
            operand.equals("FALSE", ignoreCase = true) -> "\"False\""
            else -> operand // Return as is for other cases
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
            it.name.equals(defaultTableName, ignoreCase = true) &&
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
        val b = condition.sqlColumn.toString();
        val left = getVariableAndLiteral(b)
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
