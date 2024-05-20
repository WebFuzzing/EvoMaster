package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator;

import org.evomaster.client.java.distance.heuristics.Truthness;

import java.util.Stack;

import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculatorStackItem.createStackItem;

public class WhereCalculatorStack {

    private Stack<WhereCalculatorStackItem> stack;

    public WhereCalculatorStack(){
        stack = new Stack<>();
    }

    public void pushExpression(Object expression) {
        stack.push(createStackItem(expression));
    }

    public Object popExpression() {
        WhereCalculatorStackItem item = stack.pop();
        return item.getExpression();
    }

    public void pushNumber(Number number) {
        stack.push(createStackItem(number));
    }

    public Number popNumber() {
        return (Number) popExpression();
    }

    public void pushBoolean(Boolean value) {
        stack.push(createStackItem(value));
    }

    public Boolean popBoolean() {
        WhereCalculatorStackItem item = stack.pop();
        return item.getBooleanValue();
    }

    public void pushTruthness(Truthness truthness) {
        stack.push(createStackItem(truthness));
    }

    public Truthness popTruthness() {
        WhereCalculatorStackItem item = stack.pop();
        return item.getTruthness();
    }
}
