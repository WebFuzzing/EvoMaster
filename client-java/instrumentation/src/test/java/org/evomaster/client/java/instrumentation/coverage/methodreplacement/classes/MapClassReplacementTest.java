package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.IdentityHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 19-Sep-19.
 */
class MapClassReplacementTest {


    @Test
    public void testBase(){

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
}