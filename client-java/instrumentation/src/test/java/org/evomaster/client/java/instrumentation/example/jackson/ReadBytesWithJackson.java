package org.evomaster.client.java.instrumentation.example.jackson;

/**
 * Reading a JSON payload that arrives as bytes, as a message listener does, into {@link FooBaz}.
 */
public interface ReadBytesWithJackson {

    Object readWithObjectReader(byte[] json) throws Exception;

    Object readWithObjectMapper(byte[] json) throws Exception;

    /**
     * The Jackson call happens inside spring-kafka, and not in the code of the SUT
     */
    Object readWithSpringKafkaJsonDeserializer(byte[] json) throws Exception;
}
