package org.evomaster.client.java.instrumentation;

import java.io.Serializable;

/**
 * A message the SUT handed to a broker client to publish during an action, eg a record given to
 * a Kafka producer.
 *
 * Only the address and the payload are kept, the payload only when it is text and up to
 * {@link #MAX_TEXT_LENGTH} characters, as this is recorded for every message and sent to the
 * core. Keys and headers are not kept, as the core does not read them. Serializable for
 * external mode.
 */
public class EmittedMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * How many characters of the payload are kept, see {@link #isPayloadTruncated()}.
     */
    public static final int MAX_TEXT_LENGTH = 4096;

    /**
     * Where the message was published to, eg a Kafka topic.
     */
    private final String address;

    /**
     * The payload as text, or null when there was none or it has no faithful text form, eg an
     * object the client's serializer turns into bytes.
     */
    private final String payload;

    /**
     * Whether {@link #payload} was cut at {@link #MAX_TEXT_LENGTH} characters.
     */
    private final boolean payloadTruncated;

    /**
     * The name of the class of the payload the SUT gave, or null if it gave none.
     */
    private final String payloadType;

    /**
     * @param address     where the message was published to, eg a Kafka topic
     * @param payload     the payload as text, null if none or not text. Cut here if longer than
     *                    {@link #MAX_TEXT_LENGTH}
     * @param payloadType the class name of the payload object, null if there was none
     */
    public EmittedMessage(String address, String payload, String payloadType) {
        this.address = address;
        this.payload = cut(payload);
        this.payloadTruncated = payload != null && payload.length() > MAX_TEXT_LENGTH;
        this.payloadType = payloadType;
    }

    /**
     * Cut the text at {@link #MAX_TEXT_LENGTH} characters, one less if the cut would leave half
     * of a surrogate pair at the end.
     */
    private static String cut(String text) {
        if (text == null || text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        int end = MAX_TEXT_LENGTH;
        if (Character.isHighSurrogate(text.charAt(end - 1)) && Character.isLowSurrogate(text.charAt(end))) {
            end--;
        }
        return text.substring(0, end);
    }

    public String getAddress() {
        return address;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isPayloadTruncated() {
        return payloadTruncated;
    }

    public String getPayloadType() {
        return payloadType;
    }

    @Override
    public String toString() {
        return "EmittedMessage{address='" + address + "', payloadType='" + payloadType + "'}";
    }
}
