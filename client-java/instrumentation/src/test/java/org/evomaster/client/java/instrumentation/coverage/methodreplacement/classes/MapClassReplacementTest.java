package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 19-Sep-19.
 */
class MapClassReplacementTest {

    private final String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testContainsKey(){

        HashMap<String, Integer> map = new HashMap<>();
        map.put("foo", 42);

        assertTrue(map.containsKey("foo"));
        assertFalse(map.containsKey("bar"));

        assertTrue(MapClassReplacement.containsKey(map, "foo", null));
        assertFalse(MapClassReplacement.containsKey(map, "bar", null));
    }

    @Test
    public void testIdentityHashMap(){

        IdentityHashMap<String, Integer> map = new IdentityHashMap<>();
        String a = new String();
        String b = new String();

        assertTrue(a != b);
        assertTrue(a.equals(b));

        map.put(a, 42);
        assertEquals(42, map.get(a));
        assertNull(map.get(b));

        assertTrue(map.containsKey(a));
        assertFalse(map.containsKey(b));

        assertTrue(MapClassReplacement.containsKey(map,a,null));
        assertFalse(MapClassReplacement.containsKey(map,b,null));
    }

    @Test
    public void testContainsValue(){

        Map<Integer, String> data = new HashMap<>();
        data.put(1, "a");
        data.put(2, "g");

        assertFalse(MapClassReplacement.containsValue(data,"x", idTemplate));

        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > DistanceHelper.H_NOT_EMPTY);

        assertFalse(MapClassReplacement.containsValue(data,"c", idTemplate));
        double h1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h1 > h0);

        assertFalse(MapClassReplacement.containsValue(data,"f", idTemplate));
        double h2 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h2 > h1);

        assertTrue(MapClassReplacement.containsValue(data,"a", idTemplate));
        double h3 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h3 > h2);
        assertEquals(1d, h3, 0.0001);
    }


    @Test
    public void testRemoveTaint(){

        Map<String, String> data = new HashMap<>();
        data.put("abc", "foo");
        data.put("xyz", "bar");

        String taintedKey = TaintInputName.getTaintName(0);
        String taintedValue = TaintInputName.getTaintName(1);


        assertFalse(MapClassReplacement.remove(data,taintedKey, "x", idTemplate));
        Map<String, Set<StringSpecializationInfo>> specializations = ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView();
        assertEquals(1, specializations.size());
        Set<StringSpecializationInfo> s = specializations.get(taintedKey);
        assertEquals(2, s.size());
        assertTrue(s.stream().anyMatch(t -> t.getValue().equals("abc")));
        assertTrue(s.stream().anyMatch(t -> t.getValue().equals("xyz")));


        assertFalse(MapClassReplacement.remove(data,"abc", taintedValue, idTemplate));
        assertEquals(2, specializations.size());
        s = specializations.get(taintedValue);
        assertEquals(1, s.size());
        assertTrue(s.stream().anyMatch(t -> t.getValue().equals("foo")));

        assertTrue(MapClassReplacement.remove(data,"abc", "foo", null));
    }

    @Test
    public void testRemoveHeuristics() {

        Map<String, String> data = new HashMap<>();
        data.put("abc", "foo");
        data.put("xyz", "bar");

        assertFalse(MapClassReplacement.remove(data,"a", "foo", idTemplate));
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(idTemplate);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h0 > DistanceHelper.H_NOT_EMPTY);

        assertFalse(MapClassReplacement.remove(data,"ab", "1", idTemplate));
        double h1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h1 > h0);

        assertFalse(MapClassReplacement.remove(data,"abc", "1", idTemplate));
        double h2 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h2 > h1);

        assertFalse(MapClassReplacement.remove(data,"abc", "f", idTemplate));
        double h3 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h3 > h2);

        assertFalse(MapClassReplacement.remove(data,"xyz", "ba", idTemplate));
        double h4 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h4 > h3);

        assertTrue(MapClassReplacement.remove(data,"abc", "foo", idTemplate));
        double h5 = ExecutionTracer.getValue(objectiveId);
        assertTrue(h5 > h4);
        assertEquals(1d, h5, 0.0001);
    }

    @Test
    public void testReplace(){

        Map<String, String> data = new HashMap<>();
        data.put("abc", "foo");
        data.put("xyz", "bar");

        boolean replaced = MapClassReplacement.replace(data, "foo", "bar", "HELLO", idTemplate);
        assertFalse(replaced);
        assertTrue(data.size() == 2);
        assertTrue(data.containsValue("foo"));
        assertTrue(data.containsValue("bar"));

        replaced = MapClassReplacement.replace(data, "abc", "bar", "HELLO", idTemplate);
        assertFalse(replaced);
        assertTrue(data.size() == 2);
        assertTrue(data.containsValue("foo"));
        assertTrue(data.containsValue("bar"));

        replaced = MapClassReplacement.replace(data, "xyz", "bar", "HELLO", idTemplate);
        assertTrue(replaced);
        assertTrue(data.size() == 2);
        assertTrue(data.containsValue("foo"));
        assertTrue(data.containsValue("HELLO"));
    }
}