package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.controller.db.DataRow;
import org.evomaster.clientJava.instrumentation.testability.StringTransformer;

public class HeuristicsCalculator {


    public static double computeExpression(Expression exp, DataRow data) {

        //TODO all cases

        if (exp instanceof EqualsTo) {
            return computeEqualsTo((EqualsTo) exp, data);
        } else {
            return cannotHandle(exp);
        }
    }

    private static double cannotHandle(Expression exp) {
        SimpleLogger.warn("Cannot handle SQL expression type: " + exp.toString());
        return Double.MAX_VALUE;
    }

    private static double computeEqualsTo(EqualsTo exp, DataRow data) {

        Object left = getValue(exp.getLeftExpression(), data);
        Object right = getValue(exp.getRightExpression(), data);

        //TODO all cases

        if (left instanceof Number && right instanceof Number) {
            double x = ((Number) left).doubleValue();
            double y = ((Number) right).doubleValue();

            return Math.abs(x - y);
        }
        if (left instanceof String && right instanceof String) {
            return StringTransformer.getLeftAlignmentDistance(left.toString(), right.toString());
        } else {
            return cannotHandle(exp);
        }
    }

    private static Object getValue(Expression exp, DataRow data) {

        //TODO all cases

        if (exp instanceof Column) {
            String name = ((Column) exp).getName(true);
            return data.getValueByName(name);
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
