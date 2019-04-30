package org.evomaster.client.java.controller.internal.db.constraint;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.util.SqlVisitor;
import org.evomaster.client.java.controller.internal.db.constraint.expr.*;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

public class CheckExprExtractor implements SqlVisitor<CheckExpr> {


    @Override
    public CheckExpr visit(SqlLiteral sqlLiteral) {
        switch (sqlLiteral.getTypeName()) {
            case DECIMAL: {
                BigDecimal bigDecimalValue = sqlLiteral.bigDecimalValue();
                if (bigDecimalValue.toPlainString().matches("\\d+(\\.0*)?")) {
                    return new BigIntegerLiteral(bigDecimalValue.toBigInteger());
                } else {
                    return new BigDecimalLiteral(bigDecimalValue);
                }
            }
            case BOOLEAN: {
                return new BooleanLiteral(sqlLiteral.booleanValue());
            }
            case CHAR: {
                return new StringLiteral(sqlLiteral.toValue());
            }

            default: {
                throw new IllegalArgumentException("Unsupported literal type " + sqlLiteral.getTypeName());
            }
        }

    }


    @Override
    public CheckExpr visit(SqlCall sqlCall) {
        if (sqlCall.getOperandList().size() == 1) {
            return visitUnary(sqlCall);
        } else if (sqlCall.getOperandList().size() == 2) {
            return visitBinary(sqlCall);
        } else {
            throw new IllegalArgumentException("Cannot handle sql call with " + sqlCall.getOperandList().size() + " operands");
        }
    }

    private CheckExpr visitUnary(SqlCall sqlCall) {
        if (sqlCall.getOperandList().size() != 1) {
            throw new IllegalArgumentException("Cannot invoke unary sql call with " + sqlCall.getOperandList().size() + " operands");
        }
        SqlOperator operator = sqlCall.getOperator();
        CheckExpr operand = sqlCall.getOperandList().get(0).accept(this);

        switch (operator.getKind()) {
            case IS_NOT_NULL: {
                ColumnName columnName = (ColumnName) operand;
                return new IsNotNullExpr(columnName);
            }
            default: {
                throw new IllegalArgumentException("Unsupported unary operator " + operator.getKind());
            }
        }
    }

    private CheckExpr visitBinary(SqlCall sqlCall) {
        if (sqlCall.getOperandList().size() != 2) {
            throw new IllegalArgumentException("Cannot invoke binary sql call with " + sqlCall.getOperandList().size() + " operands");
        }
        List<SqlNode> operands = sqlCall.getOperandList();
        SqlOperator operator = sqlCall.getOperator();

        CheckExpr leftOperand = operands.get(0).accept(this);
        CheckExpr rightOperand = operands.get(1).accept(this);

        switch (operator.getKind()) {
            case LESS_THAN: {
                return new ComparisonExpr(leftOperand, ComparisonOperator.LESS_THAN, rightOperand);
            }
            case LESS_THAN_OR_EQUAL: {
                return new ComparisonExpr(leftOperand, ComparisonOperator.LESS_THAN_OR_EQUAL, rightOperand);
            }
            case EQUALS: {
                return new ComparisonExpr(leftOperand, ComparisonOperator.EQUALS_TO, rightOperand);
            }
            case GREATER_THAN: {
                return new ComparisonExpr(leftOperand, ComparisonOperator.GREATER_THAN, rightOperand);
            }
            case GREATER_THAN_OR_EQUAL: {
                return new ComparisonExpr(leftOperand, ComparisonOperator.GREATER_THAN_OR_EQUAL, rightOperand);
            }
            case AND: {
                return new AndFormula(leftOperand, rightOperand);
            }
            case IN: {
                ColumnName columnName = (ColumnName) leftOperand;
                CheckExprList literalList = (CheckExprList) rightOperand;
                return new InExpression(columnName, literalList);
            }
            default: {
                throw new IllegalArgumentException("Unsupported binary operator " + operator.getKind());
            }
        }

    }

    @Override
    public CheckExpr visit(SqlIdentifier sqlIdentifier) {
        if (!sqlIdentifier.isSimple() && sqlIdentifier.names.size() != 2) {
            throw new IllegalArgumentException("Cannot handle compelx identifiers " + sqlIdentifier);
        }

        if (sqlIdentifier.isSimple()) {
            String simpleName = sqlIdentifier.getSimple();
            return new ColumnName(simpleName);
        } else {
            String tableName = sqlIdentifier.names.get(0);
            String columnName = sqlIdentifier.names.get(1);
            return new ColumnName(tableName, columnName);
        }
    }

    @Override
    public CheckExpr visit(SqlNodeList sqlNodeList) {
        List<CheckExpr> checkExprList = new LinkedList<>();
        for (SqlNode sqlNode : sqlNodeList.getList()) {
            CheckExpr checkExpr = sqlNode.accept(this);
            checkExprList.add(checkExpr);
        }
        return new CheckExprList(checkExprList);
    }


    @Override
    public CheckExpr visit(SqlDataTypeSpec sqlDataTypeSpec) {
        throw new IllegalArgumentException("not yet implemented ");
    }

    @Override
    public CheckExpr visit(SqlDynamicParam sqlDynamicParam) {
        throw new IllegalArgumentException("not yet implemented ");
    }

    @Override
    public CheckExpr visit(SqlIntervalQualifier sqlIntervalQualifier) {
        throw new IllegalArgumentException("not yet implemented ");
    }
}
