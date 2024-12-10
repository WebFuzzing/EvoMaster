package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectsClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testEqualsInteger() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals(Integer.valueOf(10), Integer.valueOf(11), prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsLong() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals(Long.valueOf(10), Long.valueOf(11), prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsString() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals("Hello", "He___", prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsFloat() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals(1.0f, 1.1f, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsDouble() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals(1.0d, 1.1d, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsChar() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals('a', 'b', prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsDate() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        String date1 = "07/15/2016";
        String time1 = "11:00 AM";
        String time2 = "11:15 AM";
        String time3 = "11:30 AM";

        String format = "MM/dd/yyyy hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);

        Date dateObject1 = sdf.parse(date1 + " " + time1);
        Date dateObject2 = sdf.parse(date1 + " " + time2);

        boolean equals0 = ObjectsClassReplacement.equals(dateObject1, dateObject2, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }


    @Test
    public void testEqualsLocalDate() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        LocalDate a = LocalDate.of(1978, 7, 31);
        LocalDate b = LocalDate.of(1978, 8, 1);

        boolean equals0 = ObjectsClassReplacement.equals(a, b, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }


    @Test
    public void testEqualsLocalTime() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        LocalTime a = LocalTime.of(10, 0, 0);
        LocalTime b = LocalTime.of(15, 0, 0);

        boolean equals0 = ObjectsClassReplacement.equals(a, b, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    LocalDateTime a = LocalDate.of(1978, 7, 31).atStartOfDay();

    @Test
    public void testEqualsLocalDateTime() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        LocalDateTime a = LocalDate.of(1978, 7, 31).atStartOfDay();
        LocalDateTime b = LocalDate.of(1978, 8, 1).atStartOfDay();

        boolean equals0 = ObjectsClassReplacement.equals(a, b, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsNull() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals(null, null, prefix);
        assertTrue(equals0);
    }

    @Test
    public void testEqualsMismatchTypes() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        LocalDateTime a = LocalDate.of(1978, 7, 31).atStartOfDay();
        String b = "Hello World";

        boolean equals0 = ObjectsClassReplacement.equals(a, b, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > 0);
        assertTrue(h0 < 1);
    }

    @Test
    public void testEqualsNullAndNonNull() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals(null, "Hello World", prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsNonNullAndNull() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = ObjectsClassReplacement.equals("Hello World", null, prefix);
        assertFalse(equals0);

        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(prefix).size());
        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }
}
