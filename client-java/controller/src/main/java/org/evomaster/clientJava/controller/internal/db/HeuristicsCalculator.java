package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
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

        if (exp instanceof EqualsTo) {
            return computeEqualsTo((EqualsTo) exp, data);
        } else {
            return cannotHandle(exp);
        }
    }

    private double cannotHandle(Expression exp) {
        SimpleLogger.warn("Cannot handle SQL expression type: " + exp.toString());
        return Double.MAX_VALUE;
    }

    private double computeEqualsTo(EqualsTo exp, DataRow data) {

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
