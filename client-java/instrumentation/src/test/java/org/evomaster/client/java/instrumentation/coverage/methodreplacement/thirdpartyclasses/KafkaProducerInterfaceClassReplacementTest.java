package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.EmittedMessage;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Producer.send, against kafka-clients' own mock producer, so no broker is needed.
 */
public class KafkaProducerInterfaceClassReplacementTest {

    static final String TOPIC = "replies";

    static final String KEY = "id-1";

    static final String PAYLOAD = "{\"result\":42}";

    static final String HEADER = "correlationId";

    static final String HEADER_VALUE = "abc";

    /**
     * Not ASCII, to check that bytes are read as UTF-8.
     */
    private static final String NON_ASCII = "ñandú €";

    /**
     * Turns any value into bytes, so that payloads of any type can be sent.
     */
    private static final Serializer<Object> ANYTHING = (topic, data) -> new byte[0];

    @BeforeEach
    public void init() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void tearDown() {
        ExecutionTracer.setExecutingDriverAction(false);
        ExecutionTracer.reset();
    }

    static MockProducer<String, Object> producer() {
        return new MockProducer<>(true, new StringSerializer(), ANYTHING);
    }

    static ProducerRecord<String, Object> record(Object payload) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, KEY, payload);
        record.headers().add(HEADER, HEADER_VALUE.getBytes(StandardCharsets.UTF_8));
        return record;
    }

    static List<EmittedMessage> emitted() {
        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        return info.getEmittedMessagesView();
    }

    static EmittedMessage onlyEmitted() {
        List<EmittedMessage> emitted = emitted();
        assertEquals(1, emitted.size(), "Emitted: " + emitted);
        return emitted.get(0);
    }

    /**
     * A payload whose text form must never be computed: that would run SUT code from inside the
     * replacement.
     */
    private static class Dto {
        final AtomicBoolean printed = new AtomicBoolean(false);

        @Override
        public String toString() {
            printed.set(true);
            throw new IllegalStateException("the payload was turned into text");
        }
    }

    @Test
    public void testASentRecordIsRecordedAsEmitted() throws Exception {

        MockProducer<String, Object> producer = producer();

        Future<?> sent = KafkaProducerInterfaceClassReplacement.send(producer, record(PAYLOAD));

        assertTrue(sent.isDone(), "the future of the original did not come back");
        assertNotNull(sent.get());
        assertEquals(1, producer.history().size(), "the record did not reach the producer");

        EmittedMessage message = onlyEmitted();
        assertEquals(TOPIC, message.getAddress());
        assertEquals(PAYLOAD, message.getPayload());
        assertFalse(message.isPayloadTruncated());
        assertEquals(String.class.getName(), message.getPayloadType());
    }

    @Test
    public void testWithACallbackTheCallbackStillRuns() {

        AtomicBoolean called = new AtomicBoolean(false);
        Callback callback = (metadata, failure) -> called.set(failure == null && metadata != null);

        KafkaProducerInterfaceClassReplacement.send(producer(), record(PAYLOAD), callback);

        assertTrue(called.get(), "the callback was not given to the original");
        assertEquals(PAYLOAD, onlyEmitted().getPayload());
    }

    @Test
    public void testBytesAreReadAsUtf8() {

        KafkaProducerInterfaceClassReplacement.send(producer(), record(NON_ASCII.getBytes(StandardCharsets.UTF_8)));

        EmittedMessage message = onlyEmitted();
        assertEquals(NON_ASCII, message.getPayload());
        assertEquals(byte[].class.getName(), message.getPayloadType());
    }

    @Test
    public void testABoxedPrimitiveHasNoText() {

        /*
            What goes on the wire for it is up to the serializer: 8 binary bytes with
            LongSerializer, not "42". Only its type is kept.
         */
        KafkaProducerInterfaceClassReplacement.send(producer(), record(42L));

        EmittedMessage message = onlyEmitted();
        assertNull(message.getPayload());
        assertEquals(Long.class.getName(), message.getPayloadType());
    }

    @Test
    public void testOnlyThePrefixOfLongBytesIsKept() {

        // 3 bytes each in UTF-8, so the bytes are well over the decoded limit
        String payload = repeat("\u20ac", 2 * EmittedMessage.MAX_TEXT_LENGTH);

        KafkaProducerInterfaceClassReplacement.send(producer(), record(payload.getBytes(StandardCharsets.UTF_8)));

        EmittedMessage message = onlyEmitted();
        assertEquals(payload.substring(0, EmittedMessage.MAX_TEXT_LENGTH), message.getPayload());
        assertTrue(message.isPayloadTruncated());
    }

    @Test
    public void testLongBytesOfAsciiAreCutAndFlagged() {

        String payload = repeat("x", 10 * EmittedMessage.MAX_TEXT_LENGTH);

        KafkaProducerInterfaceClassReplacement.send(producer(), record(payload.getBytes(StandardCharsets.UTF_8)));

        EmittedMessage message = onlyEmitted();
        assertEquals(EmittedMessage.MAX_TEXT_LENGTH, message.getPayload().length());
        assertTrue(message.isPayloadTruncated());
    }

    @Test
    public void testBytesOfExactlyTheMaximumAreKeptWhole() {

        String payload = repeat("\u20ac", EmittedMessage.MAX_TEXT_LENGTH);

        KafkaProducerInterfaceClassReplacement.send(producer(), record(payload.getBytes(StandardCharsets.UTF_8)));

        EmittedMessage message = onlyEmitted();
        assertEquals(payload, message.getPayload());
        assertFalse(message.isPayloadTruncated());
    }

    private static String repeat(String s, int times) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < times; i++) {
            text.append(s);
        }
        return text.toString();
    }

    @Test
    public void testAnObjectPayloadIsNotTurnedIntoText() {

        Dto dto = new Dto();

        KafkaProducerInterfaceClassReplacement.send(producer(), record(dto));

        EmittedMessage message = onlyEmitted();
        assertNull(message.getPayload());
        assertEquals(Dto.class.getName(), message.getPayloadType());
        assertFalse(dto.printed.get());
    }

    @Test
    public void testNoPayload() {

        KafkaProducerInterfaceClassReplacement.send(producer(), new ProducerRecord<String, Object>(TOPIC, null));

        EmittedMessage message = onlyEmitted();
        assertNull(message.getPayload());
        assertNull(message.getPayloadType());
    }

    @Test
    public void testALongPayloadIsCut() {

        StringBuilder payload = new StringBuilder();
        while (payload.length() <= EmittedMessage.MAX_TEXT_LENGTH) {
            payload.append("x");
        }

        KafkaProducerInterfaceClassReplacement.send(producer(), record(payload.toString()));

        EmittedMessage message = onlyEmitted();
        assertTrue(message.isPayloadTruncated());
        assertEquals(EmittedMessage.MAX_TEXT_LENGTH, message.getPayload().length());
    }

    @Test
    public void testOnlySoManyMessagesAreKeptPerAction() {

        MockProducer<String, Object> producer = producer();
        for (int i = 0; i < AdditionalInfo.MAX_EMITTED_MESSAGES + 1; i++) {
            KafkaProducerInterfaceClassReplacement.send(producer, record(PAYLOAD));
        }

        assertEquals(AdditionalInfo.MAX_EMITTED_MESSAGES + 1, producer.history().size(), "a send was dropped");
        assertEquals(AdditionalInfo.MAX_EMITTED_MESSAGES, emitted().size());
    }

    @Test
    public void testTheDriversSendsAreNotRecorded() {

        MockProducer<String, Object> producer = producer();
        ExecutionTracer.setExecutingDriverAction(true);

        KafkaProducerInterfaceClassReplacement.send(producer, record(PAYLOAD));

        assertEquals(1, producer.history().size(), "the driver's record was not sent");
        assertTrue(emitted().isEmpty(), "the driver's record was recorded as the SUT's");
    }

    @Test
    public void testASendThatThrowsIsNotRecorded() {

        MockProducer<String, Object> producer = producer();
        producer.close();

        assertThrows(IllegalStateException.class,
                () -> KafkaProducerInterfaceClassReplacement.send(producer, record(PAYLOAD)));

        assertTrue(emitted().isEmpty());
    }

    @Test
    public void testARecordSentThroughAWrappingProducerIsRecordedOnce() {

        MockProducer<String, Object> delegate = producer();

        /*
            What Spring's CloseSafeProducer looks like once instrumented: a Producer whose send
            calls send on the Producer it wraps, through the replacement.
         */
        Producer<?, ?> wrapper = (Producer<?, ?>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Producer.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("send")) {
                        return KafkaProducerInterfaceClassReplacement.send(delegate, args[0], args[1]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        KafkaProducerInterfaceClassReplacement.send(wrapper, record(PAYLOAD), (Callback) (metadata, failure) -> {});

        assertEquals(1, delegate.history().size());
        assertEquals(PAYLOAD, onlyEmitted().getPayload());

        //and the guard is released afterwards
        KafkaProducerInterfaceClassReplacement.send(delegate, record(PAYLOAD));
        assertEquals(2, emitted().size());
    }
}
