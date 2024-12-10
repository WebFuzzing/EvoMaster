package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class LocalTimeClassReplacementTest {


    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }


    @Test
    public void testIsAfter() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";

        LocalTime a = LocalTime.of(10, 0, 0);
        LocalTime b = LocalTime.of(15, 0, 0);

        boolean isAfter0 = LocalTimeClassReplacement.isAfter(a, b, idTemplate);
        assertFalse(isAfter0);


        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());
        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0);

        boolean isAfter1 = LocalTimeClassReplacement.isAfter(a, a, idTemplate);
        assertFalse(isAfter1);
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertNotEquals(1, h1);

        boolean isAfter2 = LocalTimeClassReplacement.isAfter(b, a, idTemplate);
        assertTrue(isAfter2);
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);
    }


    @Test
    public void testIsBeforeNull() {
        LocalTime caller = LocalTime.of(10, 30, 30);
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(NullPointerException.class, () -> {
            LocalTimeClassReplacement.isBefore(caller, null, idTemplate);
        });
    }

    @Test
    public void testIsAfterNull() {
        LocalTime caller = LocalTime.of(10, 30, 30);
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(NullPointerException.class, () -> {
            LocalTimeClassReplacement.isAfter(caller, null, idTemplate);
        });
    }

    @Test
    public void testIsBefore() {
        LocalTime a = LocalTime.of(10, 30, 30);
        LocalTime b = LocalTime.of(15, 30, 30);
        LocalTime c = LocalTime.of(20, 30, 30);


        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isBefore0 = LocalTimeClassReplacement.isBefore(c, a, idTemplate);
        assertFalse(isBefore0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0 > 0);
        assertTrue(h0 < 1);

        boolean isBefore1 = LocalTimeClassReplacement.isBefore(c, b, idTemplate);
        assertFalse(isBefore1);

        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        boolean isBefore2 = LocalTimeClassReplacement.isBefore(a, b, idTemplate);
        assertTrue(isBefore2);

        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);
    }

    @Test
    public void testEqualsNull() {
        LocalTime caller = LocalTime.of(10, 30, 30);


        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isBefore0 = LocalTimeClassReplacement.equals(caller, null, idTemplate);
        assertFalse(isBefore0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsNotLocalTime() {
        LocalTime caller = LocalTime.of(10, 30, 30);
        Object other = new Object();

        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean isBefore0 = LocalTimeClassReplacement.equals(caller, other, idTemplate);
        assertFalse(isBefore0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsLocalTime() {
        LocalTime a = LocalTime.of(10, 30, 30);
        LocalTime b = LocalTime.of(15, 30, 30);
        LocalTime c = LocalTime.of(20, 30, 30);


        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals0 = LocalTimeClassReplacement.equals(a, c, idTemplate);
        assertFalse(equals0);
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0 > 0);
        assertTrue(h0 < 1);

        boolean equals1 = LocalTimeClassReplacement.equals(a, b, idTemplate);
        assertFalse(equals1);

        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        boolean equals2 = LocalTimeClassReplacement.equals(a, a, idTemplate);
        assertTrue(equals2);

        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2);
    }

    @Test
    public void testLocalTimeParseThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            LocalTime.parse(null);
        });
        assertThrows(DateTimeParseException.class, () -> {
            LocalTime.parse("");
        });

        assertThrows(DateTimeParseException.class, () -> {
            LocalTime.parse("  11:11 ");
        });

        LocalTime localTime = LocalTime.parse("11:11");
        assertEquals(LocalTime.of(11, 11), localTime);
    }

    @Test
    public void testLocalTimeParse() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:__:__", idTemplate);
        });
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0 > 0);
        assertTrue(h0 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:5_:__", idTemplate);
        });
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:50:__", idTemplate);
        });
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2 > h1);
        assertTrue(h2 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:50:_9", idTemplate);
        });
        double h3 = ExecutionTracer.getValue(targetId);
        assertTrue(h3 > h2);
        assertTrue(h3 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:50:59", idTemplate);
        });
        double h4 = ExecutionTracer.getValue(targetId);
        assertTrue(h4 > h3);
        assertTrue(h4 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("_3:50:59", idTemplate);
        });
        double h5 = ExecutionTracer.getValue(targetId);
        assertTrue(h5 > h4);
        assertTrue(h5 < 1);

        LocalTime localTime = LocalTimeClassReplacement.parse("13:50:59", idTemplate);
        double h6 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h6);

        assertEquals(LocalTime.of(13,50,59),localTime);
    }

    @Test
    public void testLocalTimeParseNoSeconds() {
        final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:__", idTemplate);
        });
        assertEquals(1, ExecutionTracer.getNonCoveredObjectives(idTemplate).size());

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(targetId);

        assertTrue(h0 > 0);
        assertTrue(h0 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:5_", idTemplate);
        });
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("__:50", idTemplate);
        });
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2 > h1);
        assertTrue(h2 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("_5:50", idTemplate);
        });
        double h3 = ExecutionTracer.getValue(targetId);
        assertTrue(h3 > h2);
        assertTrue(h3 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("45:50", idTemplate);
        });
        double h4 = ExecutionTracer.getValue(targetId);
        assertTrue(h4 > h3);
        assertTrue(h4 < 1);

        assertThrows(DateTimeParseException.class, () -> {
            LocalTimeClassReplacement.parse("35:50", idTemplate);
        });
        double h5 = ExecutionTracer.getValue(targetId);
        assertTrue(h5 > h4);
        assertTrue(h5 < 1);

        LocalTime localTime = LocalTimeClassReplacement.parse("13:50", idTemplate);
        double h6 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h6);

        assertEquals(LocalTime.of(13,50),localTime);
    }

}