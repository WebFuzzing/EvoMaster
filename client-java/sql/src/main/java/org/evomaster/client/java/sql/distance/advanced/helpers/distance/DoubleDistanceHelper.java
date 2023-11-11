package org.evomaster.client.java.sql.distance.advanced.helpers.distance;

import java.util.function.Function;

/**
 * Class used to calculate the branch distance for doubles. Supports all comparison operators.
 */
public class DoubleDistanceHelper {

    public static double calculateDistanceForEquals(double left, double right) {
        return calculateDistance(left, right, Math::abs);
    }

    public static double calculateDistanceForNotEquals(double left, double right) {
        return calculateDistance(left, right, difference -> difference != 0 ? 0D : 1D);
    }

    public static double calculateDistanceForGreaterThan(double left, double right) {
        return calculateDistance(left, right, difference -> difference > 0 ? 0D : 1D - difference);
    }

    public static double calculateDistanceForGreaterThanOrEquals(double left, double right) {
        return calculateDistance(left, right, difference -> difference >= 0 ? 0D : - difference);
    }

    public static double calculateDistanceForMinorThan(double left, double right) {
        return calculateDistance(left, right, difference -> difference < 0 ? 0D : 1D + difference);
    }

    public static double calculateDistanceForMinorThanOrEquals(double left, double right) {
        return calculateDistance(left, right, difference -> difference <= 0 ? 0D : difference);
    }

    public static double calculateDistance(double left, double right, Function<Double, Double> differenceFunction) {
        double difference = left - right;
        return differenceFunction.apply(difference);
    }
}