import org.evomaster.dbconstraint.ast.*;

import java.util.List;

public class SMTConditionVisitor implements SqlConditionVisitor<Boolean, Integer> {
    private final StringBuilder conditionBuilder;
    private final String tableName;

    public SMTConditionVisitor(StringBuilder conditionBuilder, String tableName) {
        this.conditionBuilder = conditionBuilder;
        this.tableName = tableName;
    }

    @Override
    public Boolean visit(SqlAndCondition condition, Integer index) {
        conditionBuilder.append("(and ");
        condition.getLeftExpr().accept(this, index);
        conditionBuilder.append(" ");
        condition.getRightExpr().accept(this, index);
        conditionBuilder.append(")");
        return true;
    }

    @Override
    public Boolean visit(SqlOrCondition condition, Integer index) {
        conditionBuilder.append("(or ");
        List<SqlCondition> conditions = condition.getOrConditions();
        for (SqlCondition subCondition : conditions) {
            subCondition.accept(this, index);
            conditionBuilder.append(" ");
        }
        conditionBuilder.append(")");
        return true;
    }

    @Override
    public Boolean visit(SqlComparisonCondition condition, Integer index) {
        String columnName = condition.getLeftOperand().toString();
        String variable = "(" + columnName + " " + tableName.toLowerCase() + index + ")";
        String compare = condition.getRightOperand().toString().replace("'", "\"");
        String comparator = getSMTComparator(condition.getSqlComparisonOperator().toString());

        conditionBuilder.append("(").append(comparator).append(" ").append(variable).append(" ").append(compare).append(")");
        return null;
    }

    private String getSMTComparator(String sqlComparator) {
        switch (sqlComparator) {
            case "=":
                return "=";
            case "<>":
            case "!=":
                return "distinct";
            case ">":
                return ">";
            case ">=":
                return ">=";
            case "<":
                return "<";
            case "<=":
                return "<=";
            default:
                throw new IllegalArgumentException("Unsupported SQL comparator: " + sqlComparator);
        }
    }

    @Override
    public Boolean visit(SqlBigDecimalLiteralValue condition, Integer index) {
        throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlBigIntegerLiteralValue condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlBooleanLiteralValue condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlColumn condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlNullLiteralValue condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlStringLiteralValue condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlConditionList condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlInCondition condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlIsNotNullCondition condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlBinaryDataLiteralValue condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlSimilarToCondition condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlIsNullCondition condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

    @Override
    public Boolean visit(SqlLikeCondition condition, Integer index) {
                throw new RuntimeException("The condition is not supported: " + condition.getClass().getSimpleName());
    }

}
