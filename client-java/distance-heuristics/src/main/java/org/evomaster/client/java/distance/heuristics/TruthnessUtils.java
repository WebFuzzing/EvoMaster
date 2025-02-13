package org.evomaster.client.java.distance.heuristics;

import java.util.Arrays;
import java.util.Objects;

public class TruthnessUtils {

    /**
     * Scales to a positive double value to the [0,1] range
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


    /**
     * Returns a Truthness instance for comparing two integer values for equality.
     * <p>
     * This method calculates the distance between the two integer values, and creates a Truthness
     * instance where the `ofTrue` field is 1 minus the normalized distance, and the `ofFalse` field
     * is 1 if the values are not equal, otherwise 0.
     *
     * @param a an integer value
     * @param b another integer value
     * @return a Truthness instance representing the equality comparison of the input integer values
     */
    public static Truthness getEqualityTruthness(int a, int b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

    /**
     * Returns a Truthness instance for comparing two long values for equality.
     * <p>
     * This method calculates the distance between the two long values, and creates a Truthness
     * instance where the `ofTrue` field is 1 minus the normalized distance, and the `ofFalse` field
     * is 1 if the values are not equal, otherwise 0.
     *
     * @param a a long value
     * @param b another long value
     * @return a Truthness instance representing the equality comparison of the input integer values
     */
    public static Truthness getEqualityTruthness(long a, long b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }


    /**
     * Returns a Truthness for comparing if one double value is less than another.
     * <p>
     * This method calculates the branch distance, returning <code>ofTrue</code>
     * of 1.0d if the first value is less than the second, and 1.0d / (1.1d + distance)
     * otherwise.
     * The <code>ofFalse</code> value is the opposite of the <code>ofTrue</code> value.
     *
     * @param a the first double value
     * @param b the second double value
     * @return a Truthness instance representing the less-than comparison of the input long values
     */
    public static Truthness getLessThanTruthness(double a, double b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        return new Truthness(
                a < b ? 1d : 1d / (1.1d + distance),
                a >= b ? 1d : 1d / (1.1d + distance)
        );
    }

    /**
     * Returns a Truthness for comparing if one long value is less than another.
     * <p>
     * This method calculates the branch distance, returning <code>ofTrue</code>
     * of 1.0d if the first value is less than the second, and 1.0d / (1.1d + distance)
     * otherwise.
     * The <code>ofFalse</code> value is the opposite of the <code>ofTrue</code> value.
     *
     * @param a the first long value
     * @param b the second long value
     * @return a Truthness instance representing the less-than comparison of the input long values
     */
    public static Truthness getLessThanTruthness(long a, long b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        return new Truthness(
                a < b ? 1d : 1d / (1.1d + distance),
                a >= b ? 1d : 1d / (1.1d + distance)
        );
    }

    /**
     * Returns a Truthness instance for comparing two double values for equality.
     * <p>
     * This method normalizes the distance between the two double values,
     * and creates a Truthness instance where the `ofTrue` field is 1 minus the normalized distance,
     * and the `ofFalse` field is 1 if the values are not equal, otherwise 0.
     *
     * @param a a double value
     * @param b another double value
     * @return a Truthness instance representing the equality comparison of the input double values
     */
    public static Truthness getEqualityTruthness(double a, double b) {
        double distance = DistanceHelper.getDistanceToEquality(a, b);
        double normalizedDistance = normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

    /**
     * Returns a truthness value for comparing how close a length was to 0.
     * @param len a positive value for a length
     * @return a Truthness instance
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

    /**
     * Aggregates multiple Truthness instances using an AND operation.
     * <p>
     * This method returns a Truthness instance where the <code>ofTrue</code> field is the average of the `ofTrue`
     * values of the input truthnesses, and the <code>ofFalse</code> field is either 1.0d if any of the input Truthness
     * instances is false, or average of the `ofFalse` values from the provided Truthness instances if none of the
     * given truthnesses is false.
     *
     * @param truthnesses an array of Truthness instances to be aggregated
     * @return a new Truthness instance representing the AND aggregation of the input Truthness instances
     * @throws IllegalArgumentException if the input array is null, empty, or contains null elements
     */
    public static Truthness buildAndAggregationTruthness(Truthness... truthnesses) {
        double averageOfTrue = averageOfTrue(truthnesses);
        double falseOrAverageFalse = falseOrAverageFalse(truthnesses);
        return new Truthness(averageOfTrue, falseOrAverageFalse);
    }

    /**
     * Aggregates multiple Truthness instances using an OR operation.
     * <p>
     * This method returns a Truthness instance where the <code>ofTrue</code> field is either 1.0d if any of the input
     * Truthness instances is true, or the average of the `ofTrue` values from the provided Truthness instances if none
     * of the given truthnesses is true. The <code>ofFalse</code> field is the average of the `ofFalse` values of the
     * input truthnesses.
     *
     * @param truthnesses an array of Truthness instances to be aggregated
     * @return a new Truthness instance representing the OR aggregation of the input Truthness instances
     * @throws IllegalArgumentException if the input array is null, empty, or contains null elements
     */
    public static Truthness buildOrAggregationTruthness(Truthness... truthnesses) {
        double trueOrAverageTrue = trueOrAverageTrue(truthnesses);
        double averageOfFalse = averageOfFalse(truthnesses);
        return new Truthness(trueOrAverageTrue, averageOfFalse);
    }


    /**
     * Aggregates two Truthness instances using an XOR operation.
     * <p>
     * This method returns XOR(a,b) as (a AND NOT b) OR (NOT a AND b).
     *
     * @param left the first Truthness instance
     * @param right the second Truthness instance
     * @return a new Truthness instance representing the XOR aggregation of the input Truthness instances
     */
    public static Truthness buildXorAggregationTruthness(Truthness left, Truthness right) {
        Truthness leftAndNotRight = buildAndAggregationTruthness(left,right.invert());
        Truthness notLeftAndRight = buildAndAggregationTruthness(left.invert(),right);
        Truthness orAggregation = buildOrAggregationTruthness(leftAndNotRight, notLeftAndRight);
        return orAggregation;
    }

    /**
     * Returns an average of the <code>ofTrue</code> values for the truthnesses.
     *
     * @param truthnesses an array of Truthness instances
     * @return the average of the <code>ofTrue</code> values for the input Truthness instances
     */
    private static double averageOfTrue(Truthness... truthnesses) {
        checkValidTruthnesses(truthnesses);
        double[] getOfTrueValues = Arrays.stream(truthnesses).mapToDouble(Truthness::getOfTrue)
                .toArray();
        return average(getOfTrueValues);
    }

    /**
     * Checks if the given array of Truthness is non-empty and all instances are all non-null.
     *
     * @param truthnesses an array of Truthness instances
     */
    private static void checkValidTruthnesses(Truthness[] truthnesses) {
        if (truthnesses == null || truthnesses.length == 0 || Arrays.stream(truthnesses).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("null or empty Truthness instance");
        }
    }

    /**
     * Computes an average of the given values.
     * If no values are given, an <code>IllegalArgumentException</code> is thrown.
     *
     * @param values a non-empty list of double values.
     * @return
     */
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

    /**
     * Returns the average of the <code>ofFalse</code> values for the truthnesses.
     *
     * @param truthnesses
     * @return
     */
    private static double averageOfFalse(Truthness... truthnesses) {
        checkValidTruthnesses(truthnesses);
        double[] getOfFalseValues = Arrays.stream(truthnesses).mapToDouble(Truthness::getOfFalse)
                .toArray();
        return average(getOfFalseValues);
    }

    /**
     * Returns 1.0d if any of the truthnesses is false, otherwise returns the average of the <code>ofFalse</code> values
     * for the truthnesses.
     *
     * @param truthnesses
     * @return
     */
    private static double falseOrAverageFalse(Truthness... truthnesses) {
        checkValidTruthnesses(truthnesses);
        if (Arrays.stream(truthnesses).anyMatch(Truthness::isFalse)) {
            return 1.0d;
        } else {
            return averageOfFalse(truthnesses);
        }
    }

    /**
     * Returns 1.0d if any of the truthnesses is true, otherwise returns the average of the <code>ofTrue</code> values
     * for the truthnesses.
     *
     * @param truthnesses
     * @return
     */
    private static double trueOrAverageTrue(Truthness... truthnesses) {
        checkValidTruthnesses(truthnesses);
        if (Arrays.stream(truthnesses).anyMatch(Truthness::isTrue)) {
            return 1.0d;
        } else {
            return averageOfTrue(truthnesses);
        }
    }

    /**
     * Builds a scaled Truthness instance.
     * This method scales the given `ofTrueToScale` value using the provided `base` value
     * and creates a Truthness instance where the `ofTrue` field is the scaled value and
     * the `ofFalse` field is set to 1.0.
     *
     * @param base the base value used for scaling
     * @param ofTrueToScale the value to be scaled
     * @return a new Truthness instance with the scaled `ofTrue` value and `ofFalse` set to 1.0
     */
    public static Truthness buildScaledTruthness(double base, double ofTrueToScale) {
        final double scaledOfTrue = DistanceHelper.scaleHeuristicWithBase(ofTrueToScale, base);
        final double ofFalse = 1.0d;
        return new Truthness(scaledOfTrue, ofFalse);
    }



}
