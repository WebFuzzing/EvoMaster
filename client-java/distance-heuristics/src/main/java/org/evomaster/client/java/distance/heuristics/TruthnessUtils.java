package org.evomaster.client.java.distance.heuristics;

import java.util.Arrays;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.scaleHeuristicWithBase;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE;
import static org.evomaster.client.java.distance.heuristics.Truthness.TRUE;

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

        if(Double.isInfinite(v) || v == Double.MAX_VALUE){
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

    public static Truthness getLessThanTruthness(double a, double b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        return new Truthness(
                a < b ? 1d : 1d / (1.1d + distance),
                a >= b ? 1d : 1d / (1.1d + distance)
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
            t = TRUE;
        } else {
            t = new Truthness(1d / (1d + len), 1);
        }
        return t;
    }

    public static Truthness getStringEqualityTruthness(String str1, String str2){
        if (str1.equals(str2)) {
            return TRUE;
        } else {
            final double base = Truthness.C;
            double distance = DistanceHelper.getLeftAlignmentDistance(str1, str2);
            double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
            return new Truthness(h, 1d);
        }
    }

    public static Double avgOfTrue(Truthness... items) {
        return Arrays.stream(items).mapToDouble(Truthness::getOfTrue).sum() / items.length;
    }

    public static Double avgOfFalse(Truthness... items) {
        return Arrays.stream(items).mapToDouble(Truthness::getOfFalse).sum() / items.length;
    }

    public static Double trueOrAvgTrue(Truthness... items) {
        return Arrays.stream(items).anyMatch(Truthness::isTrue) ? 1d : avgOfTrue(items);
    }

    public static Double falseOrAvgFalse(Truthness... items) {
        return Arrays.stream(items).anyMatch(Truthness::isFalse) ? 1d : avgOfFalse(items);
    }

    public static Truthness andAggregation(Truthness... items) {
        return new Truthness(avgOfTrue(items), falseOrAvgFalse(items));
    }

    public static Truthness orAggregation(Truthness... items) {
        return new Truthness(trueOrAvgTrue(items), avgOfFalse(items));
    }

    public static Truthness trueIfConditionElseFalse(Boolean condition) {
        return condition ? TRUE : FALSE;
    }

    public static Truthness scaleTrue(Double ofTrue){
        return new Truthness(scaleHeuristicWithBase(ofTrue, Truthness.C), 1d);
    }

    public static Truthness trueOrScaleTrue(Double ofTrue){
        return ofTrue == 1d ? TRUE : scaleTrue(ofTrue);
    }
}
