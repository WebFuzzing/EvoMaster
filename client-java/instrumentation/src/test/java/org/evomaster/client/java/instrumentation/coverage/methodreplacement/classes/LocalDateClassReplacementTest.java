package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.LocalDateClassReplacement.parseHeuristic;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class LocalDateClassReplacementTest {


    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testParseValid() {
        assertEquals(1d, parseHeuristic("0001-01-01"));
        assertEquals(1d, parseHeuristic("1982-11-27"));
        assertEquals(1d, parseHeuristic("1970-01-01"));
        assertEquals(1d, parseHeuristic("9999-12-31"));
    }

    @Test
    public void testParseTooShortLong() {

        double h0 = parseHeuristic("1");
        double h1 = parseHeuristic("1234-11-"); //2 shorter
        double ok = parseHeuristic("1234-11-11"); //ok
        double h3 = parseHeuristic("1234-11-111"); // 1 too long

        assertEquals(1d, ok);
        assertTrue(h0 < h1);
        assertTrue(h0 < ok);
        assertTrue(h0 < h3);
        assertTrue(h1 < ok);
        assertTrue(h3 < ok);
        assertTrue(h1 < h3);
    }

    @Test
    public void testParseNearlyCorrect() {

        /*
            recall ASCII:
            '-' -> 45
            '0' -> 48
            '9' -> 57
            'a' -> 97
         */

        double h0 = parseHeuristic("a234-11-11");
        double h1 = parseHeuristic("1234a11-11");
        double h2 = parseHeuristic("1234-11a11");
        double h3 = parseHeuristic("1234-11-aa");
        double h4 = parseHeuristic("1234a11a11");

        assertTrue(h1 < h0);
        assertTrue(h2 < h0);
        assertEquals(h1, h2);
        assertTrue(h3 < h1);
        assertTrue(h4 < h1);
        assertTrue(h4 < h3);
    }

    @Test
    public void testIsBefore() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";

        LocalDate a = LocalDate.of(2012, 6, 30);
        LocalDate b = LocalDate.of(2012, 7, 1);

        boolean isBefore0 = LocalDateClassReplacement.isBefore(b, a, idTemplate);
        assertFalse(isBefore0);


        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());
        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0);

        boolean isBefore1 = LocalDateClassReplacement.isBefore(a, a, idTemplate);
        assertFalse(isBefore1);
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertNotEquals(1,h1);

        boolean isBefore2= LocalDateClassReplacement.isBefore(a, b, idTemplate);
        assertTrue(isBefore2);
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1,h2);

    }

    @Test
    public void testIsAfter() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";

        LocalDate a = LocalDate.of(2012, 6, 30);
        LocalDate b = LocalDate.of(2012, 7, 1);

        boolean isAfter0 = LocalDateClassReplacement.isAfter(a, b, idTemplate);
        assertFalse(isAfter0);


        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());
        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0);

        boolean isAfter1 = LocalDateClassReplacement.isAfter(a, a, idTemplate);
        assertFalse(isAfter1);
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertNotEquals(1,h1);

        boolean isAfter2= LocalDateClassReplacement.isAfter(b, a, idTemplate);
        assertTrue(isAfter2);
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1,h2);
    }


    @Test
    public void testIsEqual() {
        LocalDate a = LocalDate.of(1978,7,31);
        LocalDate b = LocalDate.of(1988,7,31);
        LocalDate c = LocalDate.of(1998,7,31);


        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isEqual0 = LocalDateClassReplacement.isEqual(a, c, idTemplate);
        assertFalse(isEqual0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0>0);
        assertTrue(h0<1);

        boolean isEqual1 = LocalDateClassReplacement.isEqual(a, b, idTemplate);
        assertFalse(isEqual1);

        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1>h0);
        assertTrue(h1<1);

        boolean isEqual2 = LocalDateClassReplacement.isEqual(a, a, idTemplate);
        assertTrue(isEqual2);

        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1,h2);
    }
}