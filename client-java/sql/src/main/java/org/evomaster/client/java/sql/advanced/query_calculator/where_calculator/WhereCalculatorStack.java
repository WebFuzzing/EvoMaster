package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator;

import org.evomaster.client.java.distance.heuristics.Truthness;

import java.util.List;
import java.util.Stack;

import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.convertToBoolean;
import static org.evomaster.client.java.sql.advanced.helpers.ConversionsHelper.convertToTruthness;

@SuppressWarnings("unchecked")
public class WhereCalculatorStack {

    private Stack<Object> stack;

    public WhereCalculatorStack(){
        stack = new Stack<>();
    }

    public void push(Object expression) {
        stack.push(expression);
    }

    public Object popGeneric() {
        return stack.pop();
    }

    public Object peekGeneric() {
        return stack.peek();
    }

    public List<Object> popValuesList() {
        return (List<Object>) popGeneric();
    }

    public Object popSingleValue() {
        Object expression = popGeneric();
        return expression instanceof List ? ((List<?>) expression).get(0) : expression;
    }

    public Number popNumber() {
        return (Number) popSingleValue();
    }

    public Boolean popBoolean() {
        return convertToBoolean(popSingleValue());
    }

    public Truthness popTruthness() {
        return convertToTruthness(popSingleValue());
    }
}
