package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LongClassReplacementTest {

    @Test
    public void testParseMaximum() {
        BigInteger bigInteger = BigInteger.valueOf(Long.MAX_VALUE);
        String input = bigInteger.toString();
        long longValue = LongClassReplacement.parseLong(input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(Long.MAX_VALUE, longValue);
    }

    @Test
    public void testParseMinimum() {
        BigInteger bigInteger = BigInteger.valueOf(Long.MIN_VALUE);
        String input = bigInteger.toString();
        long longValue = LongClassReplacement.parseLong(input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(Long.MIN_VALUE, longValue);
    }

    @Test
    public void testParseTooLarge() {
        BigInteger bigInteger = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE));
        String input = bigInteger.toString();
        assertThrows(NumberFormatException.class, () -> {
            LongClassReplacement.parseLong(input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }

    @Test
    public void testParseTooSmall() {
        BigInteger bigInteger = BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(Long.MAX_VALUE));
        String input = bigInteger.toString();
        assertThrows(NumberFormatException.class, () -> {
            LongClassReplacement.parseLong(input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }



}
