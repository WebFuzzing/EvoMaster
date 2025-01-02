package org.evomaster.client.java.distance.heuristics;

import java.util.Arrays;

public class TruthnessUtils {

    /**
     * scales to a positive double value to the [0,1] range
     *
     * @param v a non-negative double value
     * @return
     */
    public static double normalizeValue(double v) {
        if (v < 0) {
            throw new IllegalArgumentException("Negative value: " + v);
        }

        if (Double.isInfinite(v) || v == Double.MAX_VALUE) {
            return 1d;
        }

        //normalization function from old ICST/STVR paper
        double normalized = v / (v + 1d);

        assert normalized >= 0 && normalized <= 1;

        return normalized;
    }


    public static Truthness getEqualityTruthness(int a, int b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

    public static Truthness getEqualityTruthness(long a, long b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }


    public static Truthness getLessThanTruthness(double a, double b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        return new Truthness(
                a < b ? 1d : 1d / (1.1d + distance),
                a >= b ? 1d : 1d / (1.1d + distance)
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
        double normalizedDistance = normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }


    /**
     * @param len a positive value for a length
     * @return
     */
    public static Truthness getTruthnessToEmpty(int len) {
        Truthness t;
        if (len < 0) {
            throw new IllegalArgumentException("lengths should always be non-negative. Invalid length " + len);
        }
        if (len == 0) {
            t = new Truthness(1, DistanceHelper.H_NOT_NULL);
        } else {
            t = new Truthness(1d / (1d + len), 1);
        }
        return t;
    }

    public static Truthness buildAndAggregationTruthness(Truthness... truthnesses) {
        double averageOfTrue = averageOfTrue(truthnesses);
        double falseOrAverageFalse = falseOrAverageFalse(truthnesses);
        return new Truthness(averageOfTrue, falseOrAverageFalse);
    }

    private static double averageOfTrue(Truthness... truthnesses) {
        checkValidTruthnesses(truthnesses);
        double[] getOfTrueValues = Arrays.stream(truthnesses).mapToDouble(Truthness::getOfTrue)
                .toArray();
        return average(getOfTrueValues);
    }

    private static void checkValidTruthnesses(Truthness[] truthnesses) {
        if (truthnesses == null || truthnesses.length == 0 || Arrays.stream(truthnesses).anyMatch(e -> e == null)) {
            throw new IllegalArgumentException("null or empty Truthness instance");
        }
    }

    private static double average(double... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("null or empty values");
        }
        double total = 0.0;
        for (double v : values) {
            total += v;
        }
        return total / values.length;
    }

    private static double averageOfFalse(Truthness... truthnesses) {
        checkValidTruthnesses(truthnesses);
        double[] getOfFalseValues = Arrays.stream(truthnesses).mapToDouble(Truthness::getOfFalse)
                .toArray();
        return average(getOfFalseValues);
    }

    private static double falseOrAverageFalse(Truthness... truthnesses) {
        checkValidTruthnesses(truthnesses);
        if (Arrays.stream(truthnesses).anyMatch(t -> t.isFalse())) {
            return 1.0d;
        } else {
            return averageOfFalse(truthnesses);
        }
    }

    public static Truthness buildScaledTruthness(double base, double ofTrueToScale) {
        final double scaledOfTrue = DistanceHelper.scaleHeuristicWithBase(ofTrueToScale, base);
        final double ofFalse = 1.0d;
        return new Truthness(scaledOfTrue, ofFalse);
    }


}
