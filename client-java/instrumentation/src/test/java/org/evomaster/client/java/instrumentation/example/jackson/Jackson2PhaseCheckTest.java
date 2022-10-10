package org.evomaster.client.java.instrumentation.example.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Jackson2PhaseCheckTest {


    @Test
    public void testArrayWithObjects() throws JsonProcessingException {

        String json = "[{\"x\":4},{\"x\":55}]";

        ObjectMapper mapper = new ObjectMapper();

        List data = mapper.readValue(json, ArrayList.class);
        assertEquals(2, data.size());
        assertTrue(data.get(0) instanceof Map);
        assertTrue(data.get(1) instanceof Map);
    }

    @Test
    public void testArrayWithStrings() throws JsonProcessingException {

        String json = "[\"hello\",\"there\"]";

        ObjectMapper mapper = new ObjectMapper();

        List data = mapper.readValue(json, ArrayList.class);
        assertEquals(2, data.size());
        assertTrue(data.get(0) instanceof String);
        assertTrue(data.get(1) instanceof String);
    }


    private static class Foo{ public int x;}

    @Test
    public void testArrayConvertValue() throws JsonProcessingException {

        String json = "[{\"x\":42},{\"x\":55}]";

        ObjectMapper mapper = new ObjectMapper();

        List data = mapper.readValue(json, ArrayList.class);
        assertEquals(2, data.size());

        Foo foo = mapper.convertValue(data.get(0), Foo.class);
        assertEquals(42, foo.x);
    }

}
