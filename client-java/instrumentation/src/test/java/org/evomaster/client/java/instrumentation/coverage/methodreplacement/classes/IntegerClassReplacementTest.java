package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_REACHED_BUT_NULL;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.NumberParsingUtils.parseIntHeuristic;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class IntegerClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testParseValid() {
        assertEquals(1d, parseIntHeuristic("1"));
        assertEquals(1d, parseIntHeuristic("10"));
        assertEquals(1d, parseIntHeuristic("123"));
        assertEquals(1d, parseIntHeuristic("-1"));
        assertEquals(1d, parseIntHeuristic("001"));
        assertEquals(1d, parseIntHeuristic("-002"));
    }

    @Test
    public void testParseNull() {
        assertEquals(H_REACHED_BUT_NULL, parseIntHeuristic(null));
    }

    @Test
    public void testParseEmpty() {

        double hnull = parseIntHeuristic(null);
        double hempty = parseIntHeuristic("");
        double hone = parseIntHeuristic("1");

        assertTrue(hempty > hnull);
        assertTrue(hempty < hone);
    }

    @Test
    public void testParseInvalid() {

        double ha = parseIntHeuristic("a");

        assertTrue(ha > 0);
        assertTrue(ha < 1);
    }

    @Test
    public void testParseLongerInvalid() {

        double h0 = parseIntHeuristic("a");
        double h1 = parseIntHeuristic("a1");
        double h2 = parseIntHeuristic("a1a");
        double h3 = parseIntHeuristic("a1a1111");

        assertEquals(h0, h1);
        assertTrue(h1 > h2);
        assertTrue(h1 > h3);
    }

    @Test
    public void testParseTooLong() {

        double h0 = parseIntHeuristic("a");
        double h1 = parseIntHeuristic("a111111111111111111");

        assertTrue(h1 < 1);
        assertTrue(h0 > h1);
    }

    @Test
    public void testParseClassReplacement() {
        String input = Long.valueOf(Long.MAX_VALUE).toString();
        assertThrows(NumberFormatException.class, () -> {
            IntegerClassReplacement.parseInt(input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }

    @Test
    public void testEqualsNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = IntegerClassReplacement.equals(1, null, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsNotNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = IntegerClassReplacement.equals(1, 2, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertTrue(h0 > DistanceHelper.H_NOT_NULL);
    }


    @Test
    public void testValueOf() {
        String input = Long.valueOf(Long.MAX_VALUE).toString();
        assertThrows(NumberFormatException.class, () -> {
            IntegerClassReplacement.valueOf(input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }
}