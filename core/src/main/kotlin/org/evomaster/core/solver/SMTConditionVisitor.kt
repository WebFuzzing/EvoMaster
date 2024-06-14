package org.evomaster.core.solver

import org.evomaster.dbconstraint.ast.*
import org.evomaster.solver.smtlib.AssertSMTNode
import org.evomaster.solver.smtlib.SMTNode
import org.evomaster.solver.smtlib.assertion.*

class SMTConditionVisitor(
    private val tableName: String,
    private val rowIndex: Int
) : SqlConditionVisitor<SMTNode, Void> {

    private fun getColumnReference(columnName: String): String {
        return "($columnName $tableName$rowIndex)"
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
        val columnName = condition.leftOperand.toString()
        val variable = getColumnReference(columnName)
        val compare = condition.rightOperand.toString().replace("'", "\"")
        val comparator = getSMTComparator(condition.sqlComparisonOperator.toString())

        return when (comparator) {
            "=" -> AssertSMTNode(EqualsAssertion(listOf(variable, compare)))
            "distinct" -> AssertSMTNode(DistinctAssertion(listOf(variable, compare)))
            ">" -> AssertSMTNode(GreaterThanAssertion(variable, compare))
            ">=" -> AssertSMTNode(GreaterThanOrEqualsAssertion(variable, compare))
            "<" -> AssertSMTNode(LessThanAssertion(variable, compare))
            "<=" -> AssertSMTNode(LessThanOrEqualsAssertion(variable, compare))
            else -> throw IllegalArgumentException("Unsupported SQL comparator: $comparator")
        }
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
