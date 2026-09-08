package org.evomaster.client.java.controller.mongo.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.OptionalLong;

public abstract class MongoUtils {


    /**
     * Converts the provided {@code Number} into a long value if it represents an integral number
     * within the range of a {@code long}. Supports various {@code Number} types including
     * {@code Byte}, {@code Short}, {@code Integer}, {@code Long}, {@code BigInteger}, and
     * {@code BigDecimal}.
     *
     * If the provided number is of type {@code BigInteger} or {@code BigDecimal} and falls outside
     * the range of {@code long}, or if it cannot be expressed as an exact integral value,
     * the method will return an empty {@code OptionalLong}.
     *
     * @param value the {@code Number} to convert. Must not be {@code null}.
     * @return an {@code OptionalLong} containing the long value if the input represents a valid integral
     *         value within the range of {@code long}, or an empty {@code OptionalLong} otherwise.
     * @throws NullPointerException if the provided {@code value} is {@code null}.
     */
    public static OptionalLong getIntegralLongValue(Number value) {
        Objects.requireNonNull(value);

        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return OptionalLong.of(value.longValue());
        }

        if (value instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) value;
            if (bigInteger.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0
                    || bigInteger.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(bigInteger.longValue());
        }

        if (value instanceof BigDecimal) {
            try {
                return OptionalLong.of(((BigDecimal) value).longValueExact());
            } catch (ArithmeticException e) {
                return OptionalLong.empty();
            }
        }

        double doubleValue = value.doubleValue();
        if (!Double.isFinite(doubleValue)
                || doubleValue < Long.MIN_VALUE
                || doubleValue > Long.MAX_VALUE
                || doubleValue != Math.rint(doubleValue)) {
            return OptionalLong.empty();
        }

        return OptionalLong.of((long) doubleValue);
    }
}
