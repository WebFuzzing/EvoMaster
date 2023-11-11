package org.evomaster.client.java.sql.distance.advanced.query_distance.where_distance;

import org.evomaster.client.java.sql.distance.advanced.query_distance.Distance;

import static org.evomaster.client.java.sql.distance.advanced.query_distance.Distance.INF_DISTANCE;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.Distance.ZERO_DISTANCE;
import static org.evomaster.client.java.sql.distance.advanced.helpers.ConversionsHelper.convertToBoolean;

/**
 * Item used by {@link WhereDistanceStack}.
 */
public class WhereDistanceStackItem {

    private Object expression;
    private Boolean booleanValue;
    private Distance distance;

    private WhereDistanceStackItem(Object expression, Boolean booleanValue, Distance distance) {
        this.expression = expression;
        this.booleanValue = booleanValue;
        this.distance = distance;
    }

    public static WhereDistanceStackItem createStackItem(Distance distance) {
        return new WhereDistanceStackItem(null, null, distance);
    }

    public static WhereDistanceStackItem createStackItem(Object expression) {
        return new WhereDistanceStackItem(expression, null, null);
    }

    private static WhereDistanceStackItem createStackItem(Object expression, Boolean booleanValue) {
        return new WhereDistanceStackItem(expression, booleanValue, zeroIfConditionElseInf(booleanValue));
    }

    private static Distance zeroIfConditionElseInf(Boolean condition) {
        return condition ? ZERO_DISTANCE : INF_DISTANCE;
    }

    public static WhereDistanceStackItem createStackItem(Boolean booleanValue) {
        return createStackItem(booleanValue, booleanValue);
    }

    public static WhereDistanceStackItem createStackItem(Number number) {
        return createStackItem(number, convertToBoolean(number));
    }

    public Object getExpression() {
        return expression;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public Distance getDistance() {
        return distance;
    }    
}
