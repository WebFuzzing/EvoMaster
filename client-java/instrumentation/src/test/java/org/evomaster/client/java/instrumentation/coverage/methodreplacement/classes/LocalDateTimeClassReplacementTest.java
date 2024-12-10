package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DateTimeParsingUtils;
import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class LocalDateTimeClassReplacementTest {


    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testParseValid() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        LocalDateTimeClassReplacement.parse("0001-01-01T00:00:00", idTemplate);
        LocalDateTimeClassReplacement.parse("0001-01-01T00:00:00", idTemplate);
        LocalDateTimeClassReplacement.parse("1982-01-27T00:00:00", idTemplate);
        LocalDateTimeClassReplacement.parse("1970-01-01T00:00:00", idTemplate);
        LocalDateTimeClassReplacement.parse("9999-03-23T00:00:00", idTemplate);
    }

    @Test
    public void testParseTooShortLong() {

        double h0 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1");
        double h1 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1234-01-");
        double ok = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1234-01-11T00:00:00");
        double h3 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1234-01-111");

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

        double h0 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("a234-01-11T00:00:00");
        double h1 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1234a01-11T00:00:00");
        double h2 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1234-01a11T00:00:00");
        double h3 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1234-01-a˜˜taT00:00:00");
        double h4 = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1234a01a11T00:00:00");

        assertTrue(h1 < h0);
        assertTrue(h2 < h0);
        assertEquals(h1, h2);
        assertTrue(h3 < h1);
        assertTrue(h4 < h1);
    }

    @Test
    public void testIsBefore() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";

        LocalDateTime a = LocalDate.of(2012, 6, 30).atStartOfDay();
        LocalDateTime b = LocalDate.of(2012, 7, 1).atStartOfDay();

        boolean isBefore0 = LocalDateTimeClassReplacement.isBefore(b, a, idTemplate);
        assertFalse(isBefore0);


        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());
        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0);

        boolean isBefore1 = LocalDateTimeClassReplacement.isBefore(a, a, idTemplate);
        assertFalse(isBefore1);
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertNotEquals(1, h1);

        boolean isBefore2 = LocalDateTimeClassReplacement.isBefore(a, b, idTemplate);
        assertTrue(isBefore2);
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);

    }

    @Test
    public void testIsAfter() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";

        LocalDateTime a = LocalDate.of(2012, 6, 30).atStartOfDay();
        LocalDateTime b = LocalDate.of(2012, 7, 1).atStartOfDay();

        boolean isAfter0 = LocalDateTimeClassReplacement.isAfter(a, b, idTemplate);
        assertFalse(isAfter0);


        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());
        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0);

        boolean isAfter1 = LocalDateTimeClassReplacement.isAfter(a, a, idTemplate);
        assertFalse(isAfter1);
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertNotEquals(1, h1);

        boolean isAfter2 = LocalDateTimeClassReplacement.isAfter(b, a, idTemplate);
        assertTrue(isAfter2);
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);
    }


    @Test
    public void testIsEqual() {
        LocalDateTime a = LocalDate.of(1978, 7, 31).atStartOfDay();
        LocalDateTime b = LocalDate.of(1988, 7, 31).atStartOfDay();
        LocalDateTime c = LocalDate.of(1998, 7, 31).atStartOfDay();


        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isEqual0 = LocalDateTimeClassReplacement.isEqual(a, c, idTemplate);
        assertFalse(isEqual0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0 > 0);
        assertTrue(h0 < 1);

        boolean isEqual1 = LocalDateTimeClassReplacement.isEqual(a, b, idTemplate);
        assertFalse(isEqual1);

        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        boolean isEqual2 = LocalDateTimeClassReplacement.isEqual(a, a, idTemplate);
        assertTrue(isEqual2);

        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);
    }

    @Test
    public void testEquals() {
        LocalDateTime a = LocalDate.of(1978, 7, 31).atStartOfDay();
        LocalDateTime b = LocalDate.of(1988, 7, 31).atStartOfDay();
        LocalDateTime c = LocalDate.of(1998, 7, 31).atStartOfDay();


        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isEqual0 = LocalDateTimeClassReplacement.equals(a, null, prefix);
        assertFalse(isEqual0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);

        boolean isEqual1 = LocalDateTimeClassReplacement.equals(a, b, prefix);
        assertFalse(isEqual1);

        double h1 = ExecutionTracer.getValue(objectiveId);

        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        boolean isEqual2 = LocalDateTimeClassReplacement.equals(a, a, prefix);
        assertTrue(isEqual2);

        double h2 = ExecutionTracer.getValue(objectiveId);
        assertEquals(1, h2);
    }

    @Test
    public void testIsEqualNull() {
        LocalDateTime a = LocalDate.of(1978, 7, 31).atStartOfDay();


        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(
                NullPointerException.class,
                ()-> {
                    LocalDateTimeClassReplacement.isEqual(a, null, idTemplate);
                }
        );

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }


}