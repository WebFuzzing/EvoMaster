package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.db.DataRow;
import org.evomaster.client.java.controller.db.QueryResult;
import org.evomaster.client.java.instrumentation.testabilityboolean.StringTransformer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import static org.evomaster.client.java.controller.internal.db.ParserUtils.getWhere;

public class HeuristicsCalculator {


    private final SqlNameContext context;

    public HeuristicsCalculator(SqlNameContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public static double computeDistance(String statement, QueryResult data) {

        if (data.isEmpty()) {
            //if no data, we have no info whatsoever
            return Double.MAX_VALUE;
        }

        Statement stmt = ParserUtils.asStatement(statement);

        Expression where = getWhere(stmt);
        if (where == null) {
            //no constraint, and at least one data point
            return 0;
        }


        SqlNameContext context = new SqlNameContext(stmt);
        HeuristicsCalculator calculator = new HeuristicsCalculator(context);

        double min = Double.MAX_VALUE;
        for (DataRow row : data.seeRows()) {
            double dist = calculator.computeExpression(where, row);
            if (dist == 0) {
                return 0;
            }
            if (dist < min) {
                min = dist;
            }
        }

        return min;
    }


    /**
     * Compute a "branch" distance heuristics.
     *
     * @param exp the WHERE clause which we want to resolve as true
     * @param data current data raw in the database, based on the columns/tables involved in the WHERE
     * @return a branch distance, where 0 means that the data would make the WHERE resolves to true
     */
    public double computeExpression(Expression exp, DataRow data) {

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
        if(exp instanceof Between){
            return computeBetween((Between)exp, data);
        }
        if (exp instanceof ComparisonOperator) {
             //   this deals with 6 subclasses:
            return computeComparisonOperator((ComparisonOperator) exp, data);
        }
        if(exp instanceof ExistsExpression){
            //TODO
        }
        if(exp instanceof ExpressionList){
            //TODO
        }
        if (exp instanceof InExpression) {
            return computeInExpression((InExpression) exp, data);
        }
        if (exp instanceof IsNullExpression) {
            return computeIsNull((IsNullExpression) exp, data);
        }
        if(exp instanceof JsonOperator){
            //TODO
        }
        if(exp instanceof LikeExpression){
            //TODO
        }
        if(exp instanceof Matches){
            //TODO
        }
        if(exp instanceof MultiExpressionList){
            //TODO
        }
        if(exp instanceof NamedExpressionList){
            //TODO
        }
        if(exp instanceof RegExpMatchOperator){
            //TODO
        }

        return cannotHandle(exp);
    }

    private double computeBetween(Between between, DataRow data) {

        Instant start = getAsInstant(getValue(between.getBetweenExpressionStart(), data));
        Instant end = getAsInstant(getValue(between.getBetweenExpressionEnd(), data));

        Instant x = getAsInstant(getValue(between.getLeftExpression(), data));

        double after = computeComparison(x, start, new GreaterThanEquals());
        double before = computeComparison(x, end, new MinorThanEquals());

        return after + before;
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

    private Instant getAsInstant(Object obj){

        if(obj == null){
            /*
                TODO this shouldn't really happen if we have full SQL support, like sub-selects
             */
            return null;
        }

        if(obj instanceof Timestamp){
            Timestamp timestamp = (Timestamp) obj;
            return timestamp.toInstant();
        }

        if(obj instanceof String){
            try {
                return ZonedDateTime.parse(obj.toString()).toInstant();
            } catch (DateTimeParseException e){
                /*
                    maybe it is in some weird format like 28-Feb-17...
                    this shouldn't really happen, but looks like Hibernate generate SQL from
                    JPQL with Date handled like this :(
                 */
                return LocalDate.parse(obj.toString(), DateTimeFormatter.ofPattern("dd-MMM-yy"))
                        .atStartOfDay().toInstant(ZoneOffset.UTC);
            }
        }

        SimpleLogger.warn("Cannot handle time value for class: " + obj.getClass());

        return null;
    }

    private double computeComparisonOperator(ComparisonOperator exp, DataRow data) {

        Object left = getValue(exp.getLeftExpression(), data);
        Object right = getValue(exp.getRightExpression(), data);

        if(left instanceof Timestamp || right instanceof Timestamp){

            Instant a = getAsInstant(left);
            Instant b = getAsInstant(right);

            if(a==null || b==null){
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
        double dif = - Duration.between(a,b).toMillis();
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


    private double computerComparison(double dif, ComparisonOperator exp) {

        if (exp instanceof EqualsTo) {
            return Math.abs(dif);
        } else if (exp instanceof GreaterThanEquals) {
            return dif >= 0 ? 0d : -dif;
        } else if (exp instanceof GreaterThan) {
            return dif > 0 ? 0d : 1d - dif;
        } else if (exp instanceof MinorThanEquals) {
            return dif <=0  ? 0d : dif;
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
            Column column = (Column) exp;

            String name = column.getColumnName();
            String table = context.getTableName(column);

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
