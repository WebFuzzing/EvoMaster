package com.foo.somedifferentpackage.examples.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.instrumentation.example.jackson.FooBaz;
import org.evomaster.client.java.instrumentation.example.jackson.ReadWithJackson;

public class ReadWithJacksonImpl implements ReadWithJackson {

    @Override
    public Object readValue(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, objectMapper.constructType(FooBaz.class));
    }
}
