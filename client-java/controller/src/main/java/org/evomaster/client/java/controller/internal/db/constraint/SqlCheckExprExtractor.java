package org.evomaster.client.java.controller.internal.db.constraint;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.util.SqlVisitor;
import org.evomaster.client.java.controller.internal.db.constraint.expr.*;

import java.math.BigDecimal;
import java.util.List;

public class SqlCheckExprExtractor implements SqlVisitor<ConstraintExpr> {


    @Override
    public ConstraintExpr visit(SqlLiteral sqlLiteral) {
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
            default: {
                throw new IllegalArgumentException("Unsupported literal type " + sqlLiteral.getTypeName());
            }
        }

    }

    @Override
    public ConstraintExpr visit(SqlCall sqlCall) {
        SqlOperator operator = sqlCall.getOperator();
        List<SqlNode> operands = sqlCall.getOperandList();
        if (operands.size() != 2) {
            throw new IllegalArgumentException("Cannot handle sql call with less than or more than 2 operands " + sqlCall);
        }
        ConstraintExpr leftOperand = operands.get(0).accept(this);
        ConstraintExpr rightOperand = operands.get(1).accept(this);

        switch (operator.getKind()) {
            case LESS_THAN: {
                return new BinaryComparison(leftOperand, ComparisonOperator.LESS_THAN, rightOperand);
            }
            case LESS_THAN_OR_EQUAL: {
                return new BinaryComparison(leftOperand, ComparisonOperator.LESS_THAN_OR_EQUAL, rightOperand);
            }
            case EQUALS: {
                return new BinaryComparison(leftOperand, ComparisonOperator.EQUALS, rightOperand);
            }
            case GREATER_THAN: {
                return new BinaryComparison(leftOperand, ComparisonOperator.GREATER_THAN, rightOperand);
            }
            case GREATER_THAN_OR_EQUAL: {
                return new BinaryComparison(leftOperand, ComparisonOperator.GREATER_THAN_OR_EQUAL, rightOperand);
            }
            case AND: {
                return new AndFormula(leftOperand, rightOperand);
            }
            default: {
                throw new IllegalArgumentException("Unsupported operator for binary comparison " + operator.getKind());
            }
        }
    }

    @Override
    public ConstraintExpr visit(SqlNodeList sqlNodeList) {
        return null;
    }

    @Override
    public ConstraintExpr visit(SqlIdentifier sqlIdentifier) {
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
    public ConstraintExpr visit(SqlDataTypeSpec sqlDataTypeSpec) {
        return null;
    }

    @Override
    public ConstraintExpr visit(SqlDynamicParam sqlDynamicParam) {
        return null;
    }

    @Override
    public ConstraintExpr visit(SqlIntervalQualifier sqlIntervalQualifier) {
        return null;
    }
}
