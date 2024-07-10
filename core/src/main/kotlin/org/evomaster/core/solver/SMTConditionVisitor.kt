package org.evomaster.core.solver

import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.evomaster.dbconstraint.ast.*
import org.evomaster.solver.smtlib.AssertSMTNode
import org.evomaster.solver.smtlib.SMTNode
import org.evomaster.solver.smtlib.assertion.*
import java.util.*

/**
 * Visitor to convert SQL conditions to SMT nodes
 * @param defaultTableName table Name corresponding to the condition (this is used when only one table is involved)
 * @param tableAliases the table aliases used in the query, so then we can know which table is being referred to
 * @param rowIndex the row index to be used when declaring the variables in SMTlib
 * */
class SMTConditionVisitor(
    private val defaultTableName: String,
    private val tableAliases: Map<String, String>,
    private val tables: List<TableDto>,
    private val rowIndex: Int
) : SqlConditionVisitor<SMTNode, Void> {

    private fun getColumnReference(tableName: String, columnName: String): String {
        return "(${columnName.uppercase(Locale.getDefault())} ${tableName.lowercase(Locale.getDefault())}$rowIndex)"
    }

    override fun visit(condition: SqlAndCondition, parameter: Void?): SMTNode {
        val left = condition.leftExpr.accept(this, parameter) as AssertSMTNode
        val right = condition.rightExpr.accept(this, parameter) as AssertSMTNode
        return AssertSMTNode(AndAssertion(listOf(left.assertion, right.assertion)))
    }

    override fun visit(condition: SqlOrCondition, parameter: Void?): SMTNode {
        val conditions = condition.orConditions.map { it.accept(this, parameter) as AssertSMTNode }
        return AssertSMTNode(OrAssertion(conditions.map { it.assertion }))
    }

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

    private fun getVariableAndLiteral(operand: String): String {
        return if (operand.contains(".")) { // It's using an alias to refer to the column, for example "u.age"
            val parts = operand.split(".")
            val tableName = tableAliases[parts[0]] ?: parts[0]
            val columnName = parts[1]
            getColumnReference(tableName, columnName)
        } else if (isAColumn(operand)) {
            getColumnReference(defaultTableName, operand)
        } else if (operand.startsWith("'") && operand.endsWith("'")) {
            operand.replace("'", "\"")
        } else {
            operand
        }
    }

    private fun isAColumn(operand: String): Boolean {
        return tables.any { it.name.lowercase() == defaultTableName.lowercase() && it.columns.any { it.name.lowercase() == operand.lowercase() }}
    }

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

    override fun visit(condition: SqlBigDecimalLiteralValue, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlBigIntegerLiteralValue, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlBooleanLiteralValue, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlColumn, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlNullLiteralValue, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlStringLiteralValue, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlConditionList, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlInCondition, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlIsNotNullCondition, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlBinaryDataLiteralValue, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlSimilarToCondition, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlIsNullCondition, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }

    override fun visit(condition: SqlLikeCondition, parameter: Void?): SMTNode {
        return SMTNode() // TODO: implement
    }
}
