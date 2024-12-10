package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PatternClassReplacementTest {
    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testMatches() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean matches0 = PatternClassReplacement.matches("x","y", prefix);
        assertFalse(matches0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0>0);
        assertTrue(h0<1);
    }

    @Test
    public void testMatchesNullInput() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(
                NullPointerException.class,
                ()-> {
                    PatternClassReplacement.matches("x",null, prefix);
                }
        );
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_REACHED_BUT_NULL,h0);
    }

    @Test
    public void testMatchesNullPattern() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(
                NullPointerException.class,
                ()-> {
                    PatternClassReplacement.matches(null,"y", prefix);
                }
        );
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_REACHED_BUT_NULL,h0);
    }
}
