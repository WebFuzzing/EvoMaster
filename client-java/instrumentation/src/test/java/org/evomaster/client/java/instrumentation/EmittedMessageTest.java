package org.evomaster.client.java.instrumentation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class EmittedMessageTest {

    private static final String TOPIC = "replies";

    /**
     * A character outside the Basic Multilingual Plane, two chars in Java: a surrogate pair.
     */
    private static final String SUPPLEMENTARY = new String(Character.toChars(0x1F600));

    private static String repeat(String s, int times) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < times; i++) {
            text.append(s);
        }
        return text.toString();
    }

    @Test
    public void testALongPayloadIsCut() {

        String tooLong = repeat("y", EmittedMessage.MAX_TEXT_LENGTH + 1);

        EmittedMessage message = new EmittedMessage(TOPIC, tooLong, String.class.getName());

        assertEquals(EmittedMessage.MAX_TEXT_LENGTH, message.getPayload().length());
        assertTrue(message.isPayloadTruncated());
    }

    @Test
    public void testAShortPayloadIsKeptWhole() {

        EmittedMessage message = new EmittedMessage(TOPIC, "{}", String.class.getName());

        assertEquals("{}", message.getPayload());
        assertFalse(message.isPayloadTruncated());
    }

    @Test
    public void testAPayloadOfExactlyTheMaximumIsNotCut() {

        String payload = repeat("y", EmittedMessage.MAX_TEXT_LENGTH);

        EmittedMessage message = new EmittedMessage(TOPIC, payload, String.class.getName());

        assertEquals(payload, message.getPayload());
        assertFalse(message.isPayloadTruncated());
    }

    @Test
    public void testTheCutDoesNotSplitASurrogatePair() {

        //the pair straddles the cut: its high surrogate is the last char kept
        String payload = repeat("y", EmittedMessage.MAX_TEXT_LENGTH - 1) + SUPPLEMENTARY;

        EmittedMessage message = new EmittedMessage(TOPIC, payload, String.class.getName());

        assertEquals(repeat("y", EmittedMessage.MAX_TEXT_LENGTH - 1), message.getPayload());
        assertTrue(message.isPayloadTruncated());
    }

    @Test
    public void testAPairBeforeTheCutIsKept() {

        String kept = repeat("y", EmittedMessage.MAX_TEXT_LENGTH - 2) + SUPPLEMENTARY;

        EmittedMessage message = new EmittedMessage(TOPIC, kept + "y", String.class.getName());

        assertEquals(kept, message.getPayload());
        assertTrue(message.isPayloadTruncated());
    }

    @Test
    public void testItSurvivesTheTripToAnExternalController() throws Exception {

        EmittedMessage message = new EmittedMessage(TOPIC, "{}", "java.lang.String");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(message);
        }
        EmittedMessage copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (EmittedMessage) in.readObject();
        }

        assertEquals(TOPIC, copy.getAddress());
        assertEquals("{}", copy.getPayload());
        assertEquals("java.lang.String", copy.getPayloadType());
    }
}
