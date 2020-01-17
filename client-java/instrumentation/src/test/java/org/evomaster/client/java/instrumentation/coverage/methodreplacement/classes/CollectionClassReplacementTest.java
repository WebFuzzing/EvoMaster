package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by jgaleotti on 29-Ago-19.
 */
public class CollectionClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testIsEmpty() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        List<Object> emptyList = Collections.emptyList();
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, prefix);
        assertTrue(isEmptyValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertEquals(0, value);
    }

    @Test
    public void testIsNotEmpty() {
        List<Object> emptyList = Collections.singletonList("Hello World");
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate");
        assertFalse(isEmptyValue);
    }

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class,
                () -> {
                    CollectionClassReplacement.isEmpty(null, ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate");
                });
    }

    @Test
    public void testContainsOnEmptyCollection() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        List<Object> emptyList = Collections.emptyList();
        boolean containsValue = CollectionClassReplacement.contains(emptyList, "Hello World", prefix);
        assertFalse(containsValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, value);
        assertEquals(DistanceHelper.H_REACHED_BUT_EMPTY, value);
    }

    @Test
    public void testContainsOnNonEmptyStringCollection() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        List<Object> singletonList = Collections.singletonList("");
        boolean containsValue = CollectionClassReplacement.contains(singletonList, "Hello World", prefix);
        assertFalse(containsValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, value);
        assertTrue(value > DistanceHelper.H_NOT_EMPTY);
        assertTrue(value > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsOnNonEmptyMoreThanOne() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList("Hello W____"), "Hello World", prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_NOT_EMPTY);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);


        boolean containsValue1 = CollectionClassReplacement.contains(Arrays.asList("Hello W____", "Hello Worl_"), "Hello World", prefix);
        assertFalse(containsValue1);
        final double heuristicValue1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(heuristicValue1 > heuristicValue0);

    }

    @Test
    public void testContainsOnNonEmptyIntegerCollection() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList(1010), 1000, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_NOT_EMPTY);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);

        boolean containsValue1 = CollectionClassReplacement.contains(Arrays.asList(1010, 1001), 1000, prefix);
        double heuristicValue1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(heuristicValue1 > heuristicValue0);
    }

    @Test
    public void testContainsNoMatch() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList("Hello W____"), 1000, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertEquals(DistanceHelper.H_NOT_EMPTY, heuristicValue0);
    }

    @Test
    public void testContainsByte() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((byte) 0), (byte) 1, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }


    @Test
    public void testContainsShort() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((short) 0), (short) 1, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsCharacter() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((char) 0), (char) 1, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsLong() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((long) 0), (long) 1, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsFloat() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((float) 0.0), (float) 0.1, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsDouble() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((double) 0.0), (double) 0.1, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsDate() throws ParseException {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";

        String date1 = "07/15/2016";
        String time1 = "11:00 AM";
        String time2 = "11:15 AM";

        String format = "MM/dd/yyyy hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        Date dateTime1 = sdf.parse(date1 + " " + time1);
        Date dateTime2 = sdf.parse(date1 + " " + time2);

        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList(dateTime1), dateTime2, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsString() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList("Hello World"), "Hello World", prefix);
        assertTrue(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(0, heuristicValue0);
    }

    @Test
    public void testContainsNotSupported() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList(true), false, prefix);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_NOT_EMPTY, heuristicValue0);
    }

}
