package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator;

import org.evomaster.client.java.distance.heuristics.Truthness;

import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.convertToBoolean;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.trueIfConditionElseFalse;

public class WhereCalculatorStackItem {

    private Object expression;
    private Boolean booleanValue;
    private Truthness truthness;

    private WhereCalculatorStackItem(Object expression, Boolean booleanValue, Truthness truthness) {
        this.expression = expression;
        this.booleanValue = booleanValue;
        this.truthness = truthness;
    }

    public static WhereCalculatorStackItem createStackItem(Truthness truthness) {
        return new WhereCalculatorStackItem(null, null, truthness);
    }

    public static WhereCalculatorStackItem createStackItem(Object expression) {
        return new WhereCalculatorStackItem(expression, null, FALSE);
    }

    private static WhereCalculatorStackItem createStackItem(Object expression, Boolean booleanValue) {
        return new WhereCalculatorStackItem(expression, booleanValue, trueIfConditionElseFalse(booleanValue));
    }

    public static WhereCalculatorStackItem createStackItem(Boolean booleanValue) {
        return createStackItem(booleanValue, booleanValue);
    }

    public static WhereCalculatorStackItem createStackItem(Number number) {
        return createStackItem(number, convertToBoolean(number));
    }

    public Object getExpression() {
        return expression;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public Truthness getTruthness() {
        return truthness;
    }    
}
