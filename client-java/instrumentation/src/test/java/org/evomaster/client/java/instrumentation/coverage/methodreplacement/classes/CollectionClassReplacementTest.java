package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
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

    private final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testIsEmpty() {

        List<Object> emptyList = Collections.emptyList();
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, idTemplate);
        assertTrue(isEmptyValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_NOT_NULL, value);
    }

    @Test
    public void testIsNotEmpty() {
        List<Object> emptyList = Collections.singletonList("Hello World");
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, idTemplate);
        assertFalse(isEmptyValue);
    }

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class,
                () -> {
                    CollectionClassReplacement.isEmpty(null, idTemplate);
                });
    }

    @Test
    public void testContainsOnEmptyCollection() {
        List<Object> emptyList = Collections.emptyList();
        boolean containsValue = CollectionClassReplacement.contains(emptyList, "Hello World", idTemplate);
        assertFalse(containsValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, value);
        assertEquals(DistanceHelper.H_REACHED_BUT_EMPTY, value);
    }

    @Test
    public void testContainsOnNonEmptyStringCollection() {
        List<Object> singletonList = Collections.singletonList("");
        boolean containsValue = CollectionClassReplacement.contains(singletonList, "Hello World", idTemplate);
        assertFalse(containsValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, value);
        assertTrue(value > DistanceHelper.H_NOT_EMPTY);
        assertTrue(value > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsOnNonEmptyMoreThanOne() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList("Hello W____"), "Hello World", idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_NOT_EMPTY);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);


        boolean containsValue1 = CollectionClassReplacement.contains(Arrays.asList("Hello W____", "Hello Worl_"), "Hello World", idTemplate);
        assertFalse(containsValue1);
        final double heuristicValue1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(heuristicValue1 > heuristicValue0);

    }

    @Test
    public void testContainsOnNonEmptyIntegerCollection() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList(1010), 1000, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_NOT_EMPTY);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);

        boolean containsValue1 = CollectionClassReplacement.contains(Arrays.asList(1010, 1001), 1000, idTemplate);
        double heuristicValue1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(heuristicValue1 > heuristicValue0);
    }

    @Test
    public void testContainsNoMatch() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList("Hello W____"), 1000, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertEquals(DistanceHelper.H_NOT_EMPTY, heuristicValue0);
    }

    @Test
    public void testContainsByte() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((byte) 0), (byte) 1, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }


    @Test
    public void testContainsShort() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((short) 0), (short) 1, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsCharacter() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((char) 0), (char) 1, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsLong() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((long) 0), (long) 1, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsFloat() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((float) 0.0), (float) 0.1, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsDouble() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList((double) 0.0), (double) 0.1, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsDate() throws ParseException {
        String date1 = "07/15/2016";
        String time1 = "11:00 AM";
        String time2 = "11:15 AM";

        String format = "MM/dd/yyyy hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);

        Date dateTime1 = sdf.parse(date1 + " " + time1);
        Date dateTime2 = sdf.parse(date1 + " " + time2);

        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList(dateTime1), dateTime2, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertNotEquals(0, heuristicValue0);
        assertTrue(heuristicValue0 > DistanceHelper.H_REACHED_BUT_EMPTY);
    }

    @Test
    public void testContainsString() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList("Hello World"), "Hello World", idTemplate);
        assertTrue(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_NOT_NULL, heuristicValue0);
    }

    @Test
    public void testContainsNotSupported() {
        boolean containsValue0 = CollectionClassReplacement.contains(Collections.singletonList(true), false, idTemplate);
        assertFalse(containsValue0);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        final String objectiveId = nonCoveredObjectives.iterator().next();
        final double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_NOT_EMPTY, heuristicValue0);
    }

    @Test
    public void testRemove(){

        List<String> data = new ArrayList<>();
        data.add("aaa");
        data.add("bbb");

        assertFalse(data.remove("x"));
        assertFalse(CollectionClassReplacement.remove(data,"x",idTemplate));

        Map<String, Set<StringSpecializationInfo>> specializations = ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView();
        assertEquals(0, specializations.size());

        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > DistanceHelper.H_NOT_EMPTY);

        assertFalse(CollectionClassReplacement.remove(data,"xx",idTemplate));
        double h1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h1 > h0);

        assertFalse(CollectionClassReplacement.remove(data,"xxx",idTemplate));
        double h2 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h2 > h1);


        String taint = TaintInputName.getTaintName(0);
        CollectionClassReplacement.remove(data,taint,idTemplate);
        specializations = ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView();
        assertEquals(1, specializations.size());
        Set<StringSpecializationInfo> s = specializations.get(taint);
        assertEquals(2, s.size());
        assertTrue(s.stream().anyMatch(t -> t.getValue().equals("aaa")));
        assertTrue(s.stream().anyMatch(t -> t.getValue().equals("bbb")));

        assertEquals(2, data.size());
        boolean result = CollectionClassReplacement.remove(data,"aaa",idTemplate);
        assertTrue(result);
        assertEquals(1, data.size());

        nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(0, nonCoveredObjectives.size());
        double h3 = ExecutionTracer.getValue(objectiveId);
        assertEquals(1d, h3);

    }
}
