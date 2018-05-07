package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.expression.*;
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
        if (exp instanceof IsNullExpression) {
            return computeIsNull((IsNullExpression) exp, data);
        }
        if (exp instanceof InExpression) {
            return computeInExpression((InExpression) exp, data);
        }
        if (exp instanceof Parenthesis) {
            return computeExpression(((Parenthesis) exp).getExpression(), data);
        }

        return cannotHandle(exp);
    }

    private double computeInExpression(InExpression exp, DataRow data) {

        //TODO can left be a list???

        ItemsList itemsList = exp.getRightItemsList();
        if (itemsList instanceof ExpressionList) {
            ExpressionList list = (ExpressionList) itemsList;

            if (exp.isNot()) {

                double max = 0;

                for (Expression element : list.getExpressions()) {
                    ComparisonOperator op = new NotEqualsTo();
                    op.setLeftExpression(exp.getLeftExpression());
                    op.setRightExpression(element);

                    double dist = computeComparisonOperator(op, data);
                    if (dist > max) {
                        max = dist;
                        break; // no need to look at others, as no gradient
                    }
                }

                return max;

            } else {

                double min = Double.MAX_VALUE;

                for (Expression element : list.getExpressions()) {
                    ComparisonOperator op = new EqualsTo();
                    op.setLeftExpression(exp.getLeftExpression());
                    op.setRightExpression(element);

                    double dist = computeComparisonOperator(op, data);
                    if (dist < min) {
                        min = dist;
                    }
                }

                return min;
            }

        } else {
            return cannotHandle(exp);
        }
    }

    private double computeIsNull(IsNullExpression exp, DataRow data) {

        Object x = getValue(exp.getLeftExpression(), data);

        if (x == null && !exp.isNot()) {
            return 0d;
        }
        if (x != null && exp.isNot()) {
            return 0d;
        }

        return 1;
    }

    private double cannotHandle(Expression exp) {
        SimpleLogger.uniqueWarn("WARNING, cannot handle SQL expression type '" + exp.getClass().getSimpleName() +
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
        }

        if (left instanceof Boolean && right instanceof Boolean) {
            return computeBooleanComparison((Boolean) left, (Boolean) right, exp);
        }

        if (left == null || right == null) {
            return computeNullComparison(left, right, exp);
        }

        return cannotHandle(exp);
    }

    private double computeBooleanComparison(boolean x, boolean y, ComparisonOperator exp) {
        if (!checkEqualOrNotOperator(exp)) {
            return cannotHandle(exp);
        }

        if (exp instanceof EqualsTo && x == y) {
            return 0d;
        }
        if (exp instanceof NotEqualsTo && x != y) {
            return 0d;
        }

        return 1d;
    }

    private boolean checkEqualOrNotOperator(ComparisonOperator exp) {
        return (exp instanceof EqualsTo) || (exp instanceof NotEqualsTo);
    }

    private double computeNullComparison(Object x, Object y, ComparisonOperator exp) {

        assert x == null || y == null;

        if (!checkEqualOrNotOperator(exp)) {
            return cannotHandle(exp);
        }

        if (exp instanceof EqualsTo && x == y) {
            return 0d;
        }
        if (exp instanceof NotEqualsTo && x != y) {
            return 0d;
        }
        return Double.MAX_VALUE;
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
        } else if (exp instanceof NotEqualsTo) {
            if (a.equals(b)) {
                return Double.MAX_VALUE;
            } else {
                return 0d;
            }
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

        } else if (exp instanceof Parenthesis) {
            return getValue(((Parenthesis) exp).getExpression(), data);
        } else if (exp instanceof LongValue) {
            return ((LongValue) exp).getValue();
        } else if (exp instanceof StringValue) {
            return ((StringValue) exp).getNotExcapedValue();
        } else if (exp instanceof NullValue) {
            return null;
        } else if (exp instanceof SignedExpression) {
            SignedExpression signed = (SignedExpression) exp;
            Object base = getValue(signed.getExpression(), data);
            if (signed.getSign() != '-') {
                return base;
            } else {
                if (base instanceof Long) {
                    return -(Long) base;
                } else if (base instanceof Double) {
                    return -(Double) base;
                } else if (base instanceof Float) {
                    return -(Float) base;
                } else if (base instanceof Integer) {
                    return -(Integer) base;
                } else {
                    cannotHandle(exp);
                    return null;
                }
            }
        } else {
            cannotHandle(exp);
            return null;
        }
    }
}
