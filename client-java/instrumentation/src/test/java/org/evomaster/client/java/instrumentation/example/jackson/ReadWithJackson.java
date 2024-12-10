package org.evomaster.client.java.instrumentation.example.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface ReadWithJackson {

    Object readValue(String json) throws JsonProcessingException;
}
