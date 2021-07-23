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

    @Test
    public void testJUnitIssue(){

        String input = "Unable to create injector, see the following errors:\n" +
                "\n" +
                "1) [Guice/MissingImplementation]: No implementation for org.evomaster.core.output.service.TestCaseWriter was bound.\n" +
                "\n" +
                "Requested by:\n" +
                "1  : org.evomaster.core.output.service.TestSuiteWriter.testCaseWriter(TestSuiteWriter.kt:25)\n" +
                "      \\_ for field testCaseWriter\n" +
                "     at org.evomaster.core.BaseModule.configure(BaseModule.kt:32)\n" +
                "\n" +
                "Learn more:\n" +
                "  https://github.com/google/guice/wiki/MISSING_IMPLEMENTATION\n" +
                "\n" +
                "1 error";

        String regex = "[\\W](([a-z_0-9]++[.]){2,}+[A-Z][\\w$]*)";
        String anyPositionRegexMatch = String.format("([\\s\\S]*)(%s)([\\s\\S]*)", regex);

        Matcher m = Pattern.compile(regex).matcher(input);

        boolean found = m.find();
        boolean any = Pattern.matches(anyPositionRegexMatch, input);

        assertTrue(found);
        assertEquals(found, any);

        //this should NOT throw an exception
        boolean r = MatcherClassReplacement.find(m, null);
        assertTrue(r);
    }
}
