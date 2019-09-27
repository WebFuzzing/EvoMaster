package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;

public class TruthnessUtils {

    public static Truthness getEqualityTruthness(int a, int b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = Truthness.normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

    public static Truthness getEqualityTruthness(long a, long b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = Truthness.normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

    public static Truthness getLessThanTruthness(long a, long b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        return new Truthness(
                a < b ? 1d : 1d / (1.1d + distance),
                a >= b ? 1d : 1d / (1.1d + distance)
        );
    }

    public static Truthness getEqualityTruthness(double a, double b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = Truthness.normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

}
