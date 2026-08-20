package org.evomaster.client.java.distance.heuristics;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class TruthnessUtils {

    /**
     * A {@link Truthness} representing a condition that is fully satisfied.
     */
    public static final Truthness TRUE_TRUTHNESS = new Truthness(1, DistanceHelper.H_NOT_NULL);

    /**
     * A {@link Truthness} representing a condition that is not satisfied at all.
     */
    public static final Truthness FALSE_TRUTHNESS = TRUE_TRUTHNESS.invert();

    /**
     * A {@link Truthness} representing a condition that is not satisfied, but that provides a
     * better (ie, higher) base value than {@link #FALSE_TRUTHNESS}.
     */
    public static final Truthness FALSE_TRUTHNESS_BETTER = new Truthness(DistanceHelper.H_NOT_NULL_BETTER, 1);

    /**
     * Scales to a positive double value to the [0,1] range
     *
     * @param v a non-negative double value
     * @return a double value in the [0,1] range
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
     *
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
     * @param left  the first Truthness instance
     * @param right the second Truthness instance
     * @return a new Truthness instance representing the XOR aggregation of the input Truthness instances
     */
    public static Truthness buildXorAggregationTruthness(Truthness left, Truthness right) {
        Truthness leftAndNotRight = buildAndAggregationTruthness(left, right.invert());
        Truthness notLeftAndRight = buildAndAggregationTruthness(left.invert(), right);
        return buildOrAggregationTruthness(leftAndNotRight, notLeftAndRight);
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
     * @return the average of the given values.
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
     * @param truthnesses an array of Truthness instances
     * @return the average of the <code>ofFalse</code> values for the input Truthness instances
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
     * @param truthnesses an array of Truthness instances
     * @return 1.0d if any of the truthnesses is false, otherwise returns the average of the <code>ofFalse</code> values
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
     * @param truthnesses an array of Truthness instances
     * @return 1.0d if any of the truthnesses is true, otherwise returns the average of the <code>ofTrue</code> values
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
     * @param base          the base value used for scaling
     * @param ofTrueToScale the value to be scaled
     * @return a new Truthness instance with the scaled `ofTrue` value and `ofFalse` set to 1.0
     */
    public static Truthness buildScaledTruthness(double base, double ofTrueToScale) {
        final double scaledOfTrue = DistanceHelper.scaleHeuristicWithBase(ofTrueToScale, base);
        final double ofFalse = 1.0d;
        return new Truthness(scaledOfTrue, ofFalse);
    }


    public static Truthness getEqualityTruthness(UUID left, UUID right) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        double distance = DistanceHelper.getDistance(left, right);
        double normalizedDistance = normalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                !left.equals(right) ? 1d : 0d
        );
    }

    /**
     * Returns a Truthness instance for comparing two boolean values for equality.
     * <p>
     * Booleans have no intermediate equality distance, so equal values yield
     * {@link #TRUE_TRUTHNESS} and unequal values yield {@link #FALSE_TRUTHNESS}.
     *
     * @param a a boolean value
     * @param b another boolean value
     * @return a Truthness instance representing the equality comparison of the input booleans
     */
    public static Truthness getEqualityTruthness(boolean a, boolean b) {
        return a == b ? TRUE_TRUTHNESS : FALSE_TRUTHNESS;
    }

    /**
     * Returns a Truthness instance for comparing two byte arrays for content equality.
     * <p>
     * Equal content yields {@link #TRUE_TRUTHNESS}. For differing content, {@code ofTrue} is derived
     * from the number of unequal aligned bytes and unmatched trailing bytes using
     * {@link DistanceHelper#H_NOT_NULL} as its base, while {@code ofFalse} is 1.
     *
     * @param a a byte array, must not be {@code null}
     * @param b another byte array, must not be {@code null}
     * @return a Truthness instance representing content equality of the input arrays
     */
    public static Truthness getEqualityTruthness(byte[] a, byte[] b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        long distance = DistanceHelper.getLeftAlignmentDistance(a, b);
        if (distance == 0L) {
            return TRUE_TRUTHNESS;
        }
        double ofTrue = DistanceHelper.heuristicFromScaledDistanceWithBase(
                DistanceHelper.H_NOT_NULL, (double) distance);
        return new Truthness(ofTrue, 1d);
    }

    /**
     * Converts a non-negative branch distance into a false-oriented Truthness that preserves
     * closeness to the true branch.
     * <p>
     * A zero distance yields {@link #TRUE_TRUTHNESS}. Positive finite distances yield an
     * {@code ofTrue} value scaled above the {@link #FALSE_TRUTHNESS} baseline and
     * {@code ofFalse = 1}. Infinite or {@link Double#MAX_VALUE} distances yield the
     * {@link #FALSE_TRUTHNESS} baseline.
     *
     * @param distance a non-negative branch distance
     * @return a Truthness instance derived from the distance
     * @throws IllegalArgumentException if {@code distance} is negative or {@link Double#NaN}
     */
    public static Truthness getTruthnessFromDistance(double distance) {
        if (Double.isNaN(distance)) {
            throw new IllegalArgumentException("NaN distance");
        }
        if (distance < 0) {
            throw new IllegalArgumentException("Negative distance: " + distance);
        }
        if (distance == 0.0d) {
            return TRUE_TRUTHNESS;
        }
        double ofTrue = DistanceHelper.heuristicFromScaledDistanceWithBase(
                DistanceHelper.H_NOT_NULL, distance);
        return new Truthness(ofTrue, 1d);
    }

    /**
     * Computes the {@link Truthness} of the predicate {@code a.equals(b)}.
     * If the strings are equal, {@code ofTrue} is maximal (1.0). Otherwise, {@code ofTrue} is
     * derived from a left-alignment distance between the two strings (via
     * {@link DistanceHelper#getLeftAlignmentDistance}), so that strings sharing a longer common
     * prefix yield a higher (closer-to-true) heuristic value.
     *
     * @param a the first string, must not be {@code null}
     * @param b the second string, must not be {@code null}
     * @return the Truthness of {@code a} and {@code b} being equal
     */
    public static Truthness getStringEqualityTruthness(String a, String b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        if (a.equals(b)) {
            return TRUE_TRUTHNESS;
        }
        long dist = DistanceHelper.getLeftAlignmentDistance(a, b);
        double ofTrue = DistanceHelper.heuristicFromScaledDistanceWithBase(DistanceHelper.H_NOT_NULL, (double) dist);
        return new Truthness(ofTrue, 1d);
    }

}
