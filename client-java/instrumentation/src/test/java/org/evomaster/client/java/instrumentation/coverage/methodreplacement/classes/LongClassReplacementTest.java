package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class LongClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

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

    @Test
    public void testEqualsNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = LongClassReplacement.equals(1l, null, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsNotNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = LongClassReplacement.equals(1l, 2l, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertTrue(h0 > DistanceHelper.H_NOT_NULL);
    }

    @Test
    public void testValueOf() {
        long longValue = LongClassReplacement.valueOf(Long.valueOf(Long.MAX_VALUE).toString(), ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(Long.MAX_VALUE, longValue);
    }

    @Test
    public void testParseClassReplacement() {
        BigInteger bigInteger = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        String input = bigInteger.toString();

        assertThrows(NumberFormatException.class, () -> {
            Long.parseLong(input);
        });

        assertThrows(NumberFormatException.class, () -> {
            LongClassReplacement.parseLong(input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }

}
