package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_NOT_EMPTY;
import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_REACHED_BUT_EMPTY;
import static org.junit.jupiter.api.Assertions.*;

class CollectionsDistanceUtilsTest {

    @Test
    public void testEmpty(){
        List<Integer> list = new ArrayList<>();
        double h = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertEquals(H_REACHED_BUT_EMPTY, h);
    }

    @Test
    public void testSingleNotFound(){
        List<Integer> list = Arrays.asList(7);
        double h = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertTrue(h > H_REACHED_BUT_EMPTY);
        assertTrue(h < 1.0f);
    }

    @Test
    public void testIncrement(){

        List<Integer> list = new ArrayList<>();
        list.add(7);

        double h7 = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertTrue(h7 < 1.0);

        list.add(10_000);
        double h10k = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertEquals(h7, h10k, 0.001);

        list.add(40);
        double h40 = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertTrue(h40 > h7);
        assertTrue(h40 < 1.0);

        list.add(42);
        double h42 = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertTrue(h42 > h40);
        assertEquals(1.0, h42, 0.000001);
    }

    @Test
    public void testOnlyNull(){
        List<Integer> list = new ArrayList<>();
        list.add(null);
        double h = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertEquals(H_NOT_EMPTY, h, 0.0001);
    }

    @Test
    public void testWithNullFound(){
        List<Integer> list = Arrays.asList(null, 5, 2, null, 42, null);
        double h = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertEquals(1, h, 0.0001);
    }

    @Test
    public void testWithNullNotFound(){
        List<Integer> list = Arrays.asList(null, 5, 2, null, null);
        double h = CollectionsDistanceUtils.getHeuristicToContains(list, 42);
        assertTrue(h < 1.0);
        assertTrue(h > H_NOT_EMPTY);
    }

    @Test
    public void testFindNull(){
        List<Integer> list = new ArrayList<>();
        list.add(null);
        double h = CollectionsDistanceUtils.getHeuristicToContains(list, null);
        assertEquals(1, h, 0.0001);
    }

    @Test
    public void testNotFindNull(){
        List<Integer> list = Arrays.asList(42);
        double h = CollectionsDistanceUtils.getHeuristicToContains(list, null);
        assertTrue(h < 1.0);
        assertTrue(h >= H_NOT_EMPTY);
    }


    @Test
    public void testAllInputEmpty(){

        List<Integer> data = Arrays.asList(1,2,3,4);
        List<Integer> other = Arrays.asList();

        assertTrue(data.containsAll(other));

        double h = CollectionsDistanceUtils.getHeuristicToContainsAll(data, other);
        assertEquals(1, h, 0.0001);
    }

    @Test
    public void testAllEmpty(){

        List<Integer> data = Arrays.asList();
        List<Integer> other = Arrays.asList();

        assertTrue(data.containsAll(other));

        double h = CollectionsDistanceUtils.getHeuristicToContainsAll(data, other);
        assertEquals(1, h, 0.0001);
    }


    @Test
    public void testAllThisEmpty() {

        List<Integer> data = Arrays.asList();
        List<Integer> other = Arrays.asList(1,2,3,4);

        assertFalse(data.containsAll(other));

        double h = CollectionsDistanceUtils.getHeuristicToContainsAll(data, other);
        assertEquals(H_REACHED_BUT_EMPTY, h);
    }

    @Test
    public void testAllOneMatch() {

        List<Integer> data = Arrays.asList(1,444,555);
        List<Integer> other = Arrays.asList(1,2,3,4);

        assertFalse(data.containsAll(other));

        double h = CollectionsDistanceUtils.getHeuristicToContainsAll(data, other);
        assertTrue(h < 1.0);
        assertTrue(h > H_NOT_EMPTY);
        assertTrue(h >= 1d/ (double) other.size());
    }

    @Test
    public void testAllMatches() {

        List<Integer> data = Arrays.asList(4,5,67,3,2,1,88);
        List<Integer> other = Arrays.asList(1,2,3,4);

        assertTrue(data.containsAll(other));

        double h = CollectionsDistanceUtils.getHeuristicToContainsAll(data, other);
        assertEquals(1d, h);
    }

    @Test
    public void testAnyEmpty(){

        double h0 = CollectionsDistanceUtils.getHeuristicToContainsAny(Arrays.asList(), Arrays.asList(1));
        double h1 = CollectionsDistanceUtils.getHeuristicToContainsAny(Arrays.asList(0), null);
        double h2 = CollectionsDistanceUtils.getHeuristicToContainsAny(Arrays.asList(1), Arrays.asList());
        double h3 = CollectionsDistanceUtils.getHeuristicToContainsAny(Arrays.asList(1,2,3), Arrays.asList(5));
        double h4 = CollectionsDistanceUtils.getHeuristicToContainsAny(Arrays.asList(1,2,3), Arrays.asList(1));

        assertTrue(h1 > h0);
        assertTrue(h2 > h1);
        assertTrue(h3 > h2);
        assertTrue(h4 > h3);
        assertTrue(h2 <= H_REACHED_BUT_EMPTY);
        assertEquals(1d, h4, 0.0001);
    }
}