package org.evomaster.client.java.controller.mongo.utils;

import java.util.List;
import java.util.OptionalLong;

public abstract class BitmaskUtils {

    /**
     * Converts the given value into a bitmask representation if possible.
     * The input value can be of the following types:
     * - A {@code Number}, which is directly converted to its {@code long} value.
     * - A {@code List} of numbers where each number represents a bit position.
     *   These positions are used to generate a bitmask by setting the corresponding bits.
     * - A {@code byte[]} where the bytes represent the binary value of the bitmask.
     *   Only the least significant 64 bits are considered; higher-order bits must be zero.
     *
     * @param value the input value to convert. Must be a {@code Number}, a {@code List} of numbers, or a {@code byte[]}
     *              that satisfies the described conditions.
     * @return an {@code OptionalLong} containing the resulting bitmask if the input value can be converted successfully;
     *         otherwise, an empty {@code OptionalLong}.
     */
    public static OptionalLong toBitMaskValue(Object value) {
        // value can be a number
        if (value instanceof Number) {
            return OptionalLong.of(((Number) value).longValue());
        }
        // value can be a list of positions
        if (value instanceof List<?>) {
            long mask = 0L;
            for (Object p : (List<?>) value) {
                if (p instanceof Number) {
                    long pos = ((Number) p).longValue();
                    if (pos >= Long.SIZE) {
                        return OptionalLong.empty();                 // position 64+ — no fit
                    }
                    mask |= 1L << pos;                               // pos 63 -> Long.MIN_VALUE, fine
                } else {
                    return OptionalLong.empty();                     // not a number
                }
            }
            return OptionalLong.of(mask);
        }
        // value can be a byte array
        if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            for (int i = Long.BYTES; i < bytes.length; i++) {
                if (bytes[i] != 0) {
                    return OptionalLong.empty();                     // significant bits above 63
                }
            }
            long mask = 0L;
            for (int i = 0; i < Math.min(bytes.length, Long.BYTES); i++) {
                mask |= (bytes[i] & 0xFFL) << (8 * i);               // byte 0 holds positions 0-7
            }
            return OptionalLong.of(mask);
        }
        return OptionalLong.empty();
    }
}
