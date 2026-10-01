package org.evomaster.client.java.controller.mongo.utils;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class MongoUtilsTest {

    @Test
    void testGetIntegralLongValueWithPrimitiveIntegralTypes() {
        assertAll(
                () -> assertEquals(OptionalLong.of(1L), MongoUtils.getIntegralLongValue((byte) 1)),
                () -> assertEquals(OptionalLong.of(2L), MongoUtils.getIntegralLongValue((short) 2)),
                () -> assertEquals(OptionalLong.of(3L), MongoUtils.getIntegralLongValue(3)),
                () -> assertEquals(OptionalLong.of(4L), MongoUtils.getIntegralLongValue(4L))
        );
    }

    @Test
    void testGetIntegralLongValueWithBigInteger() {
        assertAll(
                () -> assertEquals(OptionalLong.of(Long.MIN_VALUE), MongoUtils.getIntegralLongValue(BigInteger.valueOf(Long.MIN_VALUE))),
                () -> assertEquals(OptionalLong.of(Long.MAX_VALUE), MongoUtils.getIntegralLongValue(BigInteger.valueOf(Long.MAX_VALUE))),
                () -> assertEquals(OptionalLong.of(42L), MongoUtils.getIntegralLongValue(new BigInteger("42"))),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE)))
        );
    }

    @Test
    void testGetIntegralLongValueWithBigDecimal() {
        assertAll(
                () -> assertEquals(OptionalLong.of(12L), MongoUtils.getIntegralLongValue(new BigDecimal("12"))),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(new BigDecimal("12.5"))),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(new BigDecimal("9223372036854775808")))
        );
    }

    @Test
    void testGetIntegralLongValueWithFloatingPointNumbers() {
        assertAll(
                () -> assertEquals(OptionalLong.of(5L), MongoUtils.getIntegralLongValue(5.0d)),
                () -> assertEquals(OptionalLong.of(6L), MongoUtils.getIntegralLongValue(6.0f)),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(5.5d)),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(Double.NaN)),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(Double.POSITIVE_INFINITY)),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(Double.NEGATIVE_INFINITY))
        );
    }

    @Test
    void testGetIntegralLongValueWithDecimal128() {
        assertAll(
                () -> assertEquals(OptionalLong.of(9L), MongoUtils.getIntegralLongValue(new Decimal128(new BigDecimal("9")))),
                () -> assertEquals(OptionalLong.empty(), MongoUtils.getIntegralLongValue(new Decimal128(new BigDecimal("9.1"))))
        );
    }

    @Test
    void testGetIntegralLongValueRejectsNull() {
        assertThrows(NullPointerException.class, () -> MongoUtils.getIntegralLongValue(null));
    }

}
