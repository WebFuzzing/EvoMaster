package org.evomaster.client.java.controller.mongo.utils;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class BitmaskUtilsTest {

    @Test
    void testToBitMaskValueWithNumber() {
        assertAll(
                () -> assertEquals(OptionalLong.of(5L), BitmaskUtils.toBitMaskValue(5)),
                () -> assertEquals(OptionalLong.of(7L), BitmaskUtils.toBitMaskValue(7.9d)),
                () -> assertEquals(OptionalLong.of(9L), BitmaskUtils.toBitMaskValue(9L)),
                () -> assertEquals(OptionalLong.of(11L), BitmaskUtils.toBitMaskValue(new Decimal128(new BigDecimal("11"))))
        );
    }

    @Test
    void testToBitMaskValueWithListPositions() {
        assertAll(
                () -> assertEquals(OptionalLong.of(11L), BitmaskUtils.toBitMaskValue(Arrays.asList(0, 1, 3))),
                () -> assertEquals(OptionalLong.of(0L), BitmaskUtils.toBitMaskValue(Collections.emptyList())),
                () -> assertEquals(OptionalLong.of(2L), BitmaskUtils.toBitMaskValue(Arrays.asList(1, 1))),
                () -> assertEquals(OptionalLong.of(Long.MIN_VALUE), BitmaskUtils.toBitMaskValue(Collections.singletonList(63L)))
        );
    }

    @Test
    void testToBitMaskValueWithInvalidListPositions() {
        assertAll(
                () -> assertEquals(OptionalLong.empty(), BitmaskUtils.toBitMaskValue(Arrays.asList(1, "x"))),
                () -> assertEquals(OptionalLong.empty(), BitmaskUtils.toBitMaskValue(Collections.singletonList(64L)))
        );
    }

    @Test
    void testToBitMaskValueWithByteArray() {
        assertAll(
                () -> assertEquals(OptionalLong.of(5L), BitmaskUtils.toBitMaskValue(new byte[]{0x05})),
                () -> assertEquals(OptionalLong.of(257L), BitmaskUtils.toBitMaskValue(new byte[]{0x01, 0x01})),
                () -> assertEquals(OptionalLong.of(1L), BitmaskUtils.toBitMaskValue(new byte[]{0x01, 0, 0, 0, 0, 0, 0, 0, 0}))
        );
    }

    @Test
    void testToBitMaskValueWithInvalidByteArray() {
        assertEquals(OptionalLong.empty(), BitmaskUtils.toBitMaskValue(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 1}));
    }

    @Test
    void testToBitMaskValueWithUnsupportedType() {
        assertAll(
                () -> assertEquals(OptionalLong.empty(), BitmaskUtils.toBitMaskValue("not-a-bitmask")),
                () -> assertEquals(OptionalLong.empty(), BitmaskUtils.toBitMaskValue(new Object())),
                () -> assertEquals(OptionalLong.empty(), BitmaskUtils.toBitMaskValue(null))
        );
    }

}
