package com.foo.somedifferentpackage.examples.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.instrumentation.example.jackson.FooBaz;
import org.evomaster.client.java.instrumentation.example.jackson.ReadBytesWithJackson;
import org.springframework.kafka.support.serializer.JsonDeserializer;

public class ReadBytesWithJacksonImpl implements ReadBytesWithJackson {

    private static final String TOPIC = "foo";

    @Override
    public Object readWithObjectReader(byte[] json) throws Exception {
        return new ObjectMapper().readerFor(FooBaz.class).readValue(json);
    }

    @Override
    public Object readWithObjectMapper(byte[] json) throws Exception {
        return new ObjectMapper().readValue(json, FooBaz.class);
    }

    @Override
    public Object readWithSpringKafkaJsonDeserializer(byte[] json) throws Exception {
        // no type headers: the target type is the one given here, as with a typed @KafkaListener
        try (JsonDeserializer<FooBaz> deserializer = new JsonDeserializer<>(FooBaz.class, false)) {
            return deserializer.deserialize(TOPIC, json);
        }
    }
}
