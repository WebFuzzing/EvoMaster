package com.foo.somedifferentpackage.examples.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonMapImpl {


    public List castToList(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.readValue(json, Map.class);
        List matches = (ArrayList) map.get("matches");
        return matches;
    }

    public Integer assignedToTypedList(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.readValue(json, Map.class);
        List<Integer> matches = (List) map.get("matches");
        Integer value = matches.get(0);
        return value;
    }


    public int castIntFromFunction(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map map = mapper.readValue(json, Map.class);
        Map<String, Object> rule = (Map<String, Object>) map.get("rule");
        int value = (int) getRequired(rule, "offset");
        return value;
    }

    private Object getRequired(Map<String, Object> elem, String propertyName) {
        Object val = elem.get(propertyName);
        if (val != null) {
            return val;
        }
        throw new RuntimeException();
    }
}
