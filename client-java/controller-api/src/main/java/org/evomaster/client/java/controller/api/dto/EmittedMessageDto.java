package org.evomaster.client.java.controller.api.dto;

/**
 * A message the SUT handed to a broker client to publish during an action, eg a record given to
 * a Kafka producer. Only the address and the payload, the latter only when it is text, truncated.
 */
public class EmittedMessageDto {

    /**
     * Where the message was published to, eg a Kafka topic. Null if the client did not say.
     */
    public String address;

    /**
     * The payload as text: a String, or bytes read as UTF-8. Null for no payload or any other
     * object, whose wire form depends on the client's serializer; see {@link #payloadType}.
     */
    public String payload;

    /**
     * Whether {@link #payload} was cut because it was too long. Null if not known.
     */
    public Boolean payloadTruncated;

    /**
     * The name of the class of the payload the SUT gave. Null if it gave none.
     */
    public String payloadType;
}
