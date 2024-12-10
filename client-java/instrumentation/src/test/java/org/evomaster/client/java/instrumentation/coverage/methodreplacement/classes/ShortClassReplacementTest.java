package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class ShortClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testParseClassReplacement() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        String input = Integer.valueOf(Integer.MAX_VALUE).toString();
        assertThrows(NumberFormatException.class, () -> {
            ShortClassReplacement.parseShort(input, prefix);
        });
    }

    @Test
    public void testEqualsNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = ShortClassReplacement.equals((short) 1, null, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsNotNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = ShortClassReplacement.equals((short) 1, (short) 2, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertTrue(h0 > DistanceHelper.H_NOT_NULL);
    }

    @Test
    public void testParseWithNullIdTemplate() {
        String input = Integer.valueOf(Integer.MAX_VALUE).toString();
        assertThrows(NumberFormatException.class, () -> {
            ShortClassReplacement.parseShort(input, null);
        });
    }

    @Test
    public void testParseShortValue() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        short shortValue = ShortClassReplacement.parseShort(String.valueOf(Short.MAX_VALUE), prefix);
        assertEquals(Short.MAX_VALUE, shortValue);
    }

    @Test
    public void testEqualsWithIdNull() {
        boolean equals = ShortClassReplacement.equals((short) 1, (short) 2, null);
        assertFalse(equals);
    }

    @Test
    public void testEquals() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = ShortClassReplacement.equals((short) 2, (short) 2, prefix);
        assertTrue(equals);
    }

    @Test
    public void testValueOf() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        short shortValue = ShortClassReplacement.valueOf(String.valueOf(Short.MAX_VALUE), prefix);
        assertEquals(Short.MAX_VALUE, shortValue);
    }
}