package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class MatcherClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testFind() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        Matcher matcher = Pattern.compile("x").matcher("y");
        boolean find0 = MatcherClassReplacement.find(matcher, prefix);
        assertFalse(find0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0>0);
        assertTrue(h0<1);

    }

    @Test
    public void testMatches() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        Matcher matcher = Pattern.compile("x").matcher("y");
        boolean matches0 = MatcherClassReplacement.matches(matcher, prefix);
        assertFalse(matches0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0>0);
        assertTrue(h0<1);
    }
}
