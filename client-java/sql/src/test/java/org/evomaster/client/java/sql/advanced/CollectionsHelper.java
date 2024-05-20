package org.evomaster.client.java.sql.advanced;

import java.util.*;

public class CollectionsHelper {

    public static <T> List<T> createList(T element1, T element2) {
        List<T> list = new LinkedList<>();
        list.add(element1);
        list.add(element2);
        return list;
    }

    public static <T> Map<String, T> createMap(String key1, T value1) {
        Map<String, T> map  = new HashMap<>();
        map.put(key1, value1);
        return map;
    }

    public static <T> Map<String, T> createMap(String key1, T value1, String key2, T value2) {
        Map<String, T> map  = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static <T> Map<String, T> createMap(String key1, T value1, String key2, T value2, String key3, T value3) {
        Map<String, T> map  = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }
}
