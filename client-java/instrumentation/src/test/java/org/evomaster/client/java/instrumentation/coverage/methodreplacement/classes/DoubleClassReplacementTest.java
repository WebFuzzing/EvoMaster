package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DoubleClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testParseSuccessSingleDot() {
        String inputString = "0.0";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseInteger() {
        String inputString = "0";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseSuccessMissingZero() {
        String inputString = "0.";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseSuccessDotZero() {
        String inputString = ".0";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseSuccessOnlyDot() {
        String inputString = ".";
        assertThrows(NumberFormatException.class, () -> {
            DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }

    @Test
    public void testParseExponentialNotation() {
        String inputString = "9.18E+09";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
    }

    @Test
    public void testEqualsNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = DoubleClassReplacement.equals(1d, null, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsNotNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = DoubleClassReplacement.equals(1d, 2d, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertTrue(h0 > DistanceHelper.H_NOT_NULL);
    }

    @Test
    public void testValueOf() {
        String inputString = ".0";
        double parsedDouble = DoubleClassReplacement.valueOf(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseSuccessDoubleDot() {
        String inputString = "0..0";

        assertThrows(NumberFormatException.class, () -> {
            Double.parseDouble(inputString);
        });

        assertThrows(NumberFormatException.class, () -> {
            DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }
}
