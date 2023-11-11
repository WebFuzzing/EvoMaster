package org.evomaster.client.java.sql.distance.advanced.query_distance.where_distance;

import org.evomaster.client.java.sql.distance.advanced.query_distance.Distance;

import java.util.Stack;

import static org.evomaster.client.java.sql.distance.advanced.query_distance.where_distance.WhereDistanceStackItem.createStackItem;

/**
 * Stack used by {@link WhereDistanceCalculator}.
 */
public class WhereDistanceStack {

    private Stack<WhereDistanceStackItem> stack;

    public WhereDistanceStack(){
        stack = new Stack<>();
    }

    public void pushGenericExpression(Object expression) {
        stack.push(createStackItem(expression));
    }

    public Object popGenericExpression() {
        WhereDistanceStackItem item = stack.pop();
        return item.getExpression();
    }

    public void pushNumber(Number number) {
        stack.push(createStackItem(number));
    }

    public Number popNumber() {
        return (Number) popGenericExpression();
    }

    public void pushBoolean(Boolean value) {
        stack.push(createStackItem(value));
    }

    public Boolean popBoolean() {
        WhereDistanceStackItem item = stack.pop();
        return item.getBooleanValue();
    }

    public void pushDistance(Distance distance) {
        stack.push(createStackItem(distance));
    }

    public Distance popDistance() {
        WhereDistanceStackItem item = stack.pop();
        return item.getDistance();
    }
}
