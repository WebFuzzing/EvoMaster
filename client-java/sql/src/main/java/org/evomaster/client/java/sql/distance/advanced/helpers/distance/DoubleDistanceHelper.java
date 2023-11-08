package org.evomaster.client.java.sql.distance.advanced.helpers.distance;

import java.util.function.Function;

public class DoubleDistanceHelper {

    public static Double calculateDistanceForEquals(Double left, Double right) {
        return calculateDistance(left, right, Math::abs);
    }

    public static Double calculateDistanceForNotEquals(Double left, Double right) {
        return calculateDistance(left, right, difference -> difference != 0 ? 0D : 1D);
    }

    public static Double calculateDistanceForGreaterThan(Double left, Double right) {
        return calculateDistance(left, right, difference -> difference > 0 ? 0D : 1D - difference);
    }

    public static Double calculateDistanceForGreaterThanOrEquals(Double left, Double right) {
        return calculateDistance(left, right, difference -> difference >= 0 ? 0D : -difference);
    }

    public static Double calculateDistanceForMinorThan(Double left, Double right) {
        return calculateDistance(left, right, difference -> difference < 0 ? 0D : 1D + difference);
    }

    public static Double calculateDistanceForMinorThanOrEquals(Double left, Double right) {
        return calculateDistance(left, right, difference -> difference <= 0 ? 0D : difference);
    }

    public static Double calculateDistance(Double left, Double right, Function<Double, Double> differenceFunction) {
        double difference = left - right;
        return differenceFunction.apply(difference);
    }
}