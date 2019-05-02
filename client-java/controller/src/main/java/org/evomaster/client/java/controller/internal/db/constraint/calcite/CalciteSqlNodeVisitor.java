package org.evomaster.client.java.controller.internal.db.constraint.calcite;

//import org.apache.calcite.sql.*;
//import org.apache.calcite.sql.util.SqlVisitor;

public class CalciteSqlNodeVisitor /*implements SqlVisitor<SqlCondition>*/ {

//
//    @Override
//    public SqlCondition visit(SqlLiteral sqlLiteral) {
//        switch (sqlLiteral.getTypeName()) {
//            case NULL: {
//                return new SqlNullLiteralValue();
//            }
//            case DECIMAL: {
//                BigDecimal bigDecimalValue = sqlLiteral.bigDecimalValue();
//                if (bigDecimalValue.toPlainString().matches("\\d+(\\.0*)?")) {
//                    return new SqlBigIntegerLiteralValue(bigDecimalValue.toBigInteger());
//                } else {
//                    return new SqlBigDecimalLiteralValue(bigDecimalValue);
//                }
//            }
//            case BOOLEAN: {
//                return new SqlBooleanLiteralValue(sqlLiteral.booleanValue());
//            }
//            case CHAR: {
//                return new SqlStringLiteralValue(sqlLiteral.toValue());
//            }
//            case BINARY: {
//                return new SqlBinaryDataLiteralValue(sqlLiteral.toString());
//            }
//            default: {
//                throw new IllegalArgumentException("Unsupported literal type " + sqlLiteral.getTypeName());
//            }
//        }
//
//    }
//
//
//    @Override
//    public SqlCondition visit(SqlCall sqlCall) {
//        if (sqlCall.getOperandList().size() == 1) {
//            return visitUnary(sqlCall);
//        } else if (sqlCall.getOperandList().size() == 2) {
//            return visitBinary(sqlCall);
//        } else {
//            throw new IllegalArgumentException("Cannot handle sql call with " + sqlCall.getOperandList().size() + " operands");
//        }
//    }
//
//    private SqlCondition visitUnary(SqlCall sqlCall) {
//        if (sqlCall.getOperandList().size() != 1) {
//            throw new IllegalArgumentException("Cannot invoke unary sql call with " + sqlCall.getOperandList().size() + " operands");
//        }
//        SqlOperator operator = sqlCall.getOperator();
//        SqlCondition operand = sqlCall.getOperandList().get(0).accept(this);
//
//        switch (operator.getKind()) {
//            case IS_NOT_NULL: {
//                SqlColumnName sqlColumnName = (SqlColumnName) operand;
//                return new SqlIsNotNullCondition(sqlColumnName);
//            }
//            case IS_NULL: {
//                SqlColumnName sqlColumnName = (SqlColumnName) operand;
//                return new SqlIsNullCondition(sqlColumnName);
//            }
//            default: {
//                throw new IllegalArgumentException("Unsupported unary operator " + operator.getKind());
//            }
//        }
//    }
//
//    private SqlCondition visitBinary(SqlCall sqlCall) {
//        if (sqlCall.getOperandList().size() != 2) {
//            throw new IllegalArgumentException("Cannot invoke binary sql call with " + sqlCall.getOperandList().size() + " operands");
//        }
//        List<SqlNode> operands = sqlCall.getOperandList();
//        SqlOperator operator = sqlCall.getOperator();
//
//        SqlCondition leftOperand = operands.get(0).accept(this);
//        SqlCondition rightOperand = operands.get(1).accept(this);
//
//        switch (operator.getKind()) {
//            case LESS_THAN: {
//                return new SqlComparisonCondition(leftOperand, SqlComparisonOperator.LESS_THAN, rightOperand);
//            }
//            case LESS_THAN_OR_EQUAL: {
//                return new SqlComparisonCondition(leftOperand, SqlComparisonOperator.LESS_THAN_OR_EQUAL, rightOperand);
//            }
//            case EQUALS: {
//                return new SqlComparisonCondition(leftOperand, SqlComparisonOperator.EQUALS_TO, rightOperand);
//            }
//            case GREATER_THAN: {
//                return new SqlComparisonCondition(leftOperand, SqlComparisonOperator.GREATER_THAN, rightOperand);
//            }
//            case GREATER_THAN_OR_EQUAL: {
//                return new SqlComparisonCondition(leftOperand, SqlComparisonOperator.GREATER_THAN_OR_EQUAL, rightOperand);
//            }
//            case AND: {
//                return new SqlAndCondition(leftOperand, rightOperand);
//            }
//            case IN: {
//                SqlColumnName sqlColumnName = (SqlColumnName) leftOperand;
//                SqlConditionList literalList = (SqlConditionList) rightOperand;
//                return new SqlInCondition(sqlColumnName, literalList);
//            }
//            case SIMILAR: {
//                SqlColumnName sqlColumnName = (SqlColumnName) leftOperand;
//                SqlStringLiteralValue pattern = (SqlStringLiteralValue) rightOperand;
//                return new SqlSimilarToCondition(sqlColumnName, pattern);
//            }
//            case LIKE: {
//                SqlColumnName sqlColumnName = (SqlColumnName) leftOperand;
//                SqlStringLiteralValue pattern = (SqlStringLiteralValue) rightOperand;
//                return new SqlLikeCondition(sqlColumnName, pattern);
//            }
//            case OR: {
//                return new SqlOrCondition(leftOperand, rightOperand);
//            }
//            default: {
//                throw new IllegalArgumentException("Unsupported binary operator " + operator.getKind());
//            }
//        }
//
//    }
//
//    @Override
//    public SqlCondition visit(SqlIdentifier sqlIdentifier) {
//        if (!sqlIdentifier.isSimple() && sqlIdentifier.names.size() != 2) {
//            throw new IllegalArgumentException("Cannot handle compelx identifiers " + sqlIdentifier);
//        }
//
//        if (sqlIdentifier.isSimple()) {
//            String simpleName = sqlIdentifier.getSimple();
//            return new SqlColumnName(simpleName);
//        } else {
//            String tableName = sqlIdentifier.names.get(0);
//            String columnName = sqlIdentifier.names.get(1);
//            return new SqlColumnName(tableName, columnName);
//        }
//    }
//
//    @Override
//    public SqlCondition visit(SqlNodeList sqlNodeList) {
//        List<SqlCondition> sqlConditionList = new LinkedList<>();
//        for (SqlNode sqlNode : sqlNodeList.getList()) {
//            SqlCondition sqlCondition = sqlNode.accept(this);
//            sqlConditionList.add(sqlCondition);
//        }
//        return new SqlConditionList(sqlConditionList);
//    }
//
//
//    @Override
//    public SqlCondition visit(SqlDataTypeSpec sqlDataTypeSpec) {
//        throw new IllegalArgumentException("not yet implemented ");
//    }
//
//    @Override
//    public SqlCondition visit(SqlDynamicParam sqlDynamicParam) {
//        throw new IllegalArgumentException("not yet implemented ");
//    }
//
//    @Override
//    public SqlCondition visit(SqlIntervalQualifier sqlIntervalQualifier) {
//        throw new IllegalArgumentException("not yet implemented ");
//    }
}
