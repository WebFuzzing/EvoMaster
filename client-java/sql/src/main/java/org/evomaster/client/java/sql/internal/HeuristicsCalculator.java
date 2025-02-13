package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Timestamp;
import java.time.*;
import java.util.Arrays;
import java.util.Objects;

import static org.evomaster.client.java.sql.internal.SqlParserUtils.getWhere;

public class HeuristicsCalculator {

    private static final String QUOTE = "'";

    private final SqlNameContext context;

    private final TaintHandler taintHandler;

    private final boolean advancedHeuristics;

    protected HeuristicsCalculator(SqlNameContext context, TaintHandler handler, boolean advancedHeuristics) {
        this.context = Objects.requireNonNull(context);
        this.taintHandler = handler;
        this.advancedHeuristics = advancedHeuristics;
    }

    //only for tests
    protected static double computeDistance(String statement, QueryResult... data) {
        return computeDistance(statement, null, null, false, data).sqlDistance;
    }

    public static SqlDistanceWithMetrics computeDistance(
            String sqlCommand,
            DbInfoDto schema,
            TaintHandler taintHandler,
            /**
             * Enable more advance techniques since first SQL support
             */
            boolean advancedHeuristics,
            QueryResult... data
    ) {

        /**
         * This is not an ideal solution, but it will remain this way until config.heuristicsForSQLAdvanced remains
         * experimental and it does not replace the "Plain" SqlHeuristicsCalculator
         */
        if (advancedHeuristics) {
            return SqlHeuristicsCalculator.computeDistance(sqlCommand,schema,taintHandler,data);
        }

        if (data.length == 0 || Arrays.stream(data).allMatch(QueryResult::isEmpty)){
            //if no data, we have no info whatsoever
            return new SqlDistanceWithMetrics(Double.MAX_VALUE,0, false);
        }

        Statement stmt = SqlParserUtils.parseSqlCommand(sqlCommand);
        Expression where = getWhere(stmt);
        if (where == null) {
            //no constraint and at least one data point
            return new SqlDistanceWithMetrics(0.0,0, false);
        }

        SqlNameContext context = new SqlNameContext(stmt);
        if (schema != null) {
            context.setSchema(schema);
        }
        HeuristicsCalculator calculator = new HeuristicsCalculator(context, taintHandler, advancedHeuristics);

        double minSqlDistance = Double.MAX_VALUE;
        int rowCount = 0;
        for(QueryResult qr: data){
            for (DataRow row : qr.seeRows()) {
                rowCount++;
                try {
                    double dist = calculator.computeExpression(where, row);
                    if (dist == 0.0) {
                        return new SqlDistanceWithMetrics(0.0, rowCount, false);
                    } else if (dist < minSqlDistance) {
                        minSqlDistance = dist;
                    }
                } catch (Exception ex) {
                    SimpleLogger.uniqueWarn("Failed to compute where expression: " + where + " with data " + row);
                    return new SqlDistanceWithMetrics(Double.MAX_VALUE, rowCount, true);
                }
            }
        }

        return new SqlDistanceWithMetrics(minSqlDistance,rowCount, false);
    }

    /**
     * Compute a "branch" distance heuristics.
     *
     * @param exp  the WHERE clause which we want to resolve as true
     * @param data current data raw in the database, based on the columns/tables involved in the WHERE
     * @return a branch distance, where 0 means that the data would make the WHERE resolves to true
     */
    private double computeExpression(Expression exp, DataRow data) {

        //TODO all cases

        //------ net.sf.jsqlparser.expression.operators.*  ---------
        if (exp instanceof Parenthesis) {
            return computeExpression(((Parenthesis) exp).getExpression(), data);
        }


        //------ net.sf.jsqlparser.expression.operators.conditional.*  ---------
        if (exp instanceof AndExpression) {
            return computeAnd((AndExpression) exp, data);
        }
        if (exp instanceof OrExpression) {
            return computeOr((OrExpression) exp, data);
        }


        //------ net.sf.jsqlparser.expression.operators.relational.*  ---------
        if (exp instanceof Between) {
            return computeBetween((Between) exp, data);
        }
        if (exp instanceof ComparisonOperator) {
            //   this deals with 6 subclasses:
            return computeComparisonOperator((ComparisonOperator) exp, data);
        }

        if (exp instanceof InExpression) {
            return computeInExpression((InExpression) exp, data);
        }
        if (exp instanceof IsNullExpression) {
            return computeIsNull((IsNullExpression) exp, data);
        }

        if (exp instanceof ExistsExpression) {
            //TODO
        }
        if (exp instanceof ExpressionList) {
            //TODO
        }
        if (exp instanceof JsonOperator) {
            //TODO
        }
        if (exp instanceof LikeExpression) {
            //TODO  too complex for branch distance, but could have taint analysis
        }
        if (exp instanceof Matches) {
            //TODO
        }
        if (exp instanceof ExpressionList) {
            //TODO
        }
        if (exp instanceof NamedExpressionList) {
            //TODO
        }
        if (exp instanceof RegExpMatchOperator) {
            //TODO  too complex for branch distance, but could have taint analysis
        }
        return cannotHandle(exp);
    }

    private double computeBetween(Between between, DataRow data) {

        Instant start = getAsInstant(getValue(between.getBetweenExpressionStart(), data));
        Instant end = getAsInstant(getValue(between.getBetweenExpressionEnd(), data));

        Instant x = getAsInstant(getValue(between.getLeftExpression(), data));

        double after = computeComparison(x, start, new GreaterThanEquals());
        double before = computeComparison(x, end, new MinorThanEquals());

        return DistanceHelper.addDistances(after, before);
    }

    private double computeInExpression(InExpression exp, DataRow data) {

        //TODO can left be a list???

        Expression rightExpression = exp.getRightExpression();
        if (rightExpression instanceof ExpressionList<?>) {
            ExpressionList<?> expressionList = (ExpressionList<?>) rightExpression;

            if (exp.isNot()) {

                double max = 0;

                for (Expression element : expressionList) {
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

                for (Expression element : expressionList) {
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
                "' with value: " + exp);
        return Double.MAX_VALUE;
    }


    private double computeAnd(AndExpression exp, DataRow data) {

        double a = computeExpression(exp.getLeftExpression(), data);
        double b = computeExpression(exp.getRightExpression(), data);

        /*
            We divide by 2, to avoid overflows when two distances are sum together.
            This is particularly important as in few cases we use Double.MAX_VALUE as a distance value
         */
        return DistanceHelper.addDistances(a/2.0, b/2.0);
    }


    private double computeOr(OrExpression exp, DataRow data) {

        double a = computeExpression(exp.getLeftExpression(), data);
        double b = computeExpression(exp.getRightExpression(), data);

        return Math.min(a, b);
    }

    protected Instant getAsInstant(Object obj) {

        if (obj == null) {
            /*
                TODO this shouldn't really happen if we have full SQL support, like sub-selects
             */
            return null;
        }

        if (obj instanceof Instant)
            return (Instant) obj;

        if (obj instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) obj;
            return timestamp.toInstant();
        }

        if (obj instanceof String) {

            return ColumnTypeParser.getAsInstant((String) obj);
        }

        SimpleLogger.warn("Cannot handle time value for class: " + obj.getClass());
        return null;
    }

    private double computeComparisonOperator(ComparisonOperator exp, DataRow data) {

        Object left = getValue(exp.getLeftExpression(), data);
        Object right = getValue(exp.getRightExpression(), data);


        if (left instanceof Timestamp || right instanceof Timestamp
                || left instanceof Instant || right instanceof Instant) {

            Instant a = getAsInstant(left);
            Instant b = getAsInstant(right);

            if (a == null || b == null) {
                return cannotHandle(exp);
            }

            return computeComparison(a, b, exp);
        }

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

    private double computeComparison(Instant a, Instant b, ComparisonOperator exp) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }

        double dif = -Duration.between(a, b).toMillis();
        return computerComparison(dif, exp);
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

    private double computeNullComparison(Object left, Object right, ComparisonOperator exp) {

        assert left == null || right == null;

        if (!checkEqualOrNotOperator(exp)) {
            return cannotHandle(exp);
        }

        if (exp instanceof EqualsTo && left == right) {
            return 0d;
        }
        if (exp instanceof NotEqualsTo && left != right) {
            return 0d;
        }
        return Double.MAX_VALUE;
    }


    private double computerComparison(double dif, ComparisonOperator exp) {

        if (exp instanceof EqualsTo) {
            return Math.abs(dif);
        } else if (exp instanceof GreaterThanEquals) {
            return dif >= 0 ? 0d : -dif;
        } else if (exp instanceof GreaterThan) {
            return dif > 0 ? 0d : 1d - dif;
        } else if (exp instanceof MinorThanEquals) {
            return dif <= 0 ? 0d : dif;
        } else if (exp instanceof MinorThan) {
            return dif < 0 ? 0d : 1d + dif;
        } else if (exp instanceof NotEqualsTo) {
            return dif != 0 ? 0d : 1d;
        } else {
            return cannotHandle(exp);
        }
    }

    private double computerComparison(double x, double y, ComparisonOperator exp) {
        double dif = x - y;
        return computerComparison(dif, exp);
    }

    private double computeComparison(String a, String b, ComparisonOperator exp) {

        if (exp instanceof EqualsTo) {

            if(taintHandler != null){
                taintHandler.handleTaintForStringEquals(a,b,false);
            }
            return DistanceHelper.getLeftAlignmentDistance(a, b);

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
            Column column = (Column) exp;

            String name = column.getColumnName();
            String table = context.getTableName(column);

            return data.getValueByName(name, table);

        } else if (exp instanceof Parenthesis) {
            return getValue(((Parenthesis) exp).getExpression(), data);
        } else if (exp instanceof LongValue) {
            return ((LongValue) exp).getValue();
        } else if (exp instanceof DoubleValue) {
            return ((DoubleValue) exp).getValue();
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
        } else if (exp instanceof CastExpression) {
            CastExpression castExpression = (CastExpression) exp;
            return getValue(castExpression.getLeftExpression(), data);
        } else if (exp instanceof DateTimeLiteralExpression) {
            DateTimeLiteralExpression dateTimeLiteralExpression = (DateTimeLiteralExpression) exp;
            String str = dateTimeLiteralExpression.getValue();
            assert (str.length() > 2 && startsAndEndsWithQuotes(str));
            str = removeFirstAndLastCharacter(str);
            return str;
        } else {
            cannotHandle(exp);
            return null;
        }
    }

    /**
     * Given a string "Hello World" returns "ello Worl".
     * Requires the string's length to be greater than 2
     *
     * @param str
     * @return
     */
    private String removeFirstAndLastCharacter(String str) {
        if (str.length() < 2) {
            throw new IllegalArgumentException("Cannot remove quotes from " + str);
        }
        return str.substring(1, str.length() - 1);
    }

    private boolean startsAndEndsWithQuotes(String str) {
        return str.startsWith(QUOTE) && str.endsWith(QUOTE);
    }
}
