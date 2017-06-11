package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.db.DataRow;
import org.evomaster.clientJava.instrumentation.testability.StringTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HeuristicsCalculator {

    /**
     * Key -> table alias,
     * Value -> table name
     */
    private final Map<String, String> tableAliases;

    public HeuristicsCalculator(Map<String, String> aliases) {

        Map<String, String> map = new HashMap<>();
        if (aliases != null) {
            map.putAll(aliases);
        }

        tableAliases = Collections.unmodifiableMap(map);
    }

    public double computeExpression(Expression exp, DataRow data) {

        //TODO all cases

        if (exp instanceof ComparisonOperator) {
            return computeComparisonOperator((ComparisonOperator) exp, data);
        }
        if (exp instanceof AndExpression) {
            return computeAnd((AndExpression) exp, data);
        }
        if (exp instanceof OrExpression) {
            return computeOr((OrExpression) exp, data);
        }

        return cannotHandle(exp);
    }

    private double cannotHandle(Expression exp) {
        SimpleLogger.warn("WARNING, cannot handle SQL expression type '" + exp.getClass().getSimpleName() +
                "' with value: " + exp.toString());
        return Double.MAX_VALUE;
    }


    private double computeAnd(AndExpression exp, DataRow data) {

        double a = computeExpression(exp.getLeftExpression(), data);
        double b = computeExpression(exp.getRightExpression(), data);

        double sum = a + b;
        if (sum < Math.max(a, b)) {
            //overflow
            return Double.MAX_VALUE;
        } else {
            return sum;
        }
    }

    private double computeOr(OrExpression exp, DataRow data) {

        double a = computeExpression(exp.getLeftExpression(), data);
        double b = computeExpression(exp.getRightExpression(), data);

        return Math.min(a, b);
    }

    private double computeComparisonOperator(ComparisonOperator exp, DataRow data) {

        Object left = getValue(exp.getLeftExpression(), data);
        Object right = getValue(exp.getRightExpression(), data);

        if (left instanceof Number && right instanceof Number) {
            double x = ((Number) left).doubleValue();
            double y = ((Number) right).doubleValue();

            return computerComparison(x, y, exp);
        }
        if (left instanceof String && right instanceof String) {
            return computeComparison(left.toString(), right.toString(), exp);
        } else {
            return cannotHandle(exp);
        }
    }

    private double computerComparison(double x, double y, ComparisonOperator exp) {

        if (exp instanceof EqualsTo) {
            return Math.abs(x - y);
        } else if (exp instanceof GreaterThanEquals) {
            return x >= y ? 0d : y - x;
        } else if (exp instanceof GreaterThan) {
            return x > y ? 0d : 1d + y - x;
        } else if (exp instanceof MinorThanEquals) {
            return x <= y ? 0d : x - y;
        } else if (exp instanceof MinorThan) {
            return x < y ? 0d : 1d + (x - y);
        } else if (exp instanceof NotEqualsTo) {
            return x != y ? 0d : 1d;
        } else {
            return cannotHandle(exp);
        }
    }

    private double computeComparison(String a, String b, ComparisonOperator exp) {
        if (exp instanceof EqualsTo) {
            return StringTransformer.getLeftAlignmentDistance(a, b);
        } else {
            return cannotHandle(exp);
        }
    }


    private Object getValue(Expression exp, DataRow data) {

        //TODO all cases

        if (exp instanceof Column) {
            String name = ((Column) exp).getColumnName();

            String table = ((Column) exp).getTable().getName();
            //might be an alias
            table = tableAliases.getOrDefault(table, table);

            return data.getValueByName(name, table);

        } else if (exp instanceof LongValue) {
            return ((LongValue) exp).getValue();
        } else if (exp instanceof StringValue) {
            return ((StringValue) exp).getNotExcapedValue();
        } else {
            cannotHandle(exp);
            return null;
        }
    }
}
