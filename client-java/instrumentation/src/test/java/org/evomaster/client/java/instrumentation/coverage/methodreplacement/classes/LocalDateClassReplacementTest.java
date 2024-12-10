package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DateTimeParsingUtils;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
        assertEquals(1d, DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("0001-01-01"));
        assertEquals(1d, DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1982-11-27"));
        assertEquals(1d, DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1970-01-01"));
        assertEquals(1d, DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("9999-12-31"));
    }

    @Test
    public void testParseTooShortLong() {

        double h0 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1");
        double h1 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1234-11-"); //2 shorter
        double ok = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1234-11-11"); //ok
        double h3 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1234-11-111"); // 1 too long

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

        double h0 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("a234-11-11");
        double h1 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1234a11-11");
        double h2 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1234-11a11");
        double h3 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1234-11-aa");
        double h4 = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1234a11a11");

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
        assertNotEquals(1, h1);

        boolean isBefore2 = LocalDateClassReplacement.isBefore(a, b, idTemplate);
        assertTrue(isBefore2);
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);

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
        assertNotEquals(1, h1);

        boolean isAfter2 = LocalDateClassReplacement.isAfter(b, a, idTemplate);
        assertTrue(isAfter2);
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);
    }


    @Test
    public void testIsEqual() {
        LocalDate a = LocalDate.of(1978, 7, 31);
        LocalDate b = LocalDate.of(1988, 7, 31);
        LocalDate c = LocalDate.of(1998, 7, 31);


        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isEqual0 = LocalDateClassReplacement.isEqual(a, c, idTemplate);
        assertFalse(isEqual0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0 > 0);
        assertTrue(h0 < 1);

        boolean isEqual1 = LocalDateClassReplacement.isEqual(a, b, idTemplate);
        assertFalse(isEqual1);

        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        boolean isEqual2 = LocalDateClassReplacement.isEqual(a, a, idTemplate);
        assertTrue(isEqual2);

        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);
    }


    @Test
    public void testNotEquals() {
        LocalDate a = LocalDate.of(1978, 7, 31);
        LocalDate b = LocalDate.of(1988, 7, 31);

        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isEqual0 = LocalDateClassReplacement.equals(a, b, prefix);
        assertFalse(isEqual0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsNull() {
        LocalDate a = LocalDate.of(1978, 7, 31);

        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isEqual0 = LocalDateClassReplacement.equals(a, null, prefix);
        assertFalse(isEqual0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);

    }

    @Test
    public void testIsEqualsNull() {
        LocalDate a = LocalDate.of(1978, 7, 31);

        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(NullPointerException.class,
                () -> {
                    LocalDateClassReplacement.isEqual(a, null, prefix);
                }
        );
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);

    }

    @Test
    public void testParseNull() {
        LocalDate a = LocalDate.of(1978, 7, 31);

        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(NullPointerException.class,
                () -> {
                    LocalDateClassReplacement.parse(null, prefix);
                }
        );
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);

    }

    @Test
    public void testEquals() {
        LocalDate a = LocalDate.of(1978, 7, 31);
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isEqual0 = LocalDateClassReplacement.equals(a, a, prefix);
        assertTrue(isEqual0);
    }

    @Test
    public void testIsBeforeNull() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        LocalDate a = LocalDate.of(2012, 6, 30);
        assertThrows(NullPointerException.class,
                () -> {
                    LocalDateClassReplacement.isBefore(a, null, idTemplate);
                });

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testIsAfterNull() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        LocalDate a = LocalDate.of(2012, 6, 30);
        assertThrows(NullPointerException.class,
                () -> {
                    LocalDateClassReplacement.isAfter(a, null, idTemplate);
                });

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

}