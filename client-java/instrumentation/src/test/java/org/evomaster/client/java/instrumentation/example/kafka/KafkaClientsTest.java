package org.evomaster.client.java.instrumentation.example.kafka;

import com.foo.somedifferentpackage.examples.kafka.KafkaClientsImpl;
import org.evomaster.client.java.instrumentation.EmittedMessage;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.AsyncMessageTracker;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Kafka replacements applied by the instrumentation, to SUT code declaring the classes or the
 * interfaces, and to library code calling replaced methods (KafkaProducer calling itself,
 * Spring's CloseSafeProducer calling the producer it wraps), where a record is recorded once.
 */
public class KafkaClientsTest {

    private static final String TOPIC = "replies";

    private static final String KEY = "id-1";

    private static final String PAYLOAD = "{\"result\":42}";

    private static String defaultReplacement;

    @BeforeAll
    public static void initClass() {
        defaultReplacement = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,KAFKA");
    }

    @AfterAll
    public static void tearDown() {
        if (defaultReplacement == null) {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement);
        }
    }

    @BeforeEach
    public void initTest() {
        UnitsInfoRecorder.reset();
        ObjectiveRecorder.reset(true);
        ExecutionTracer.reset();
    }

    private static KafkaClients getInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");
        return (KafkaClients) cl.loadClass(KafkaClientsImpl.class.getName()).newInstance();
    }

    private static List<EmittedMessage> emitted() {
        return ExecutionTracer.exposeAdditionalInfoList().get(0).getEmittedMessagesView();
    }

    private static void handlingARecord() {
        AsyncMessageTracker.received(Collections.singletonList(TOPIC));
        assertEquals(1, AsyncMessageTracker.getHandlingState(TOPIC).getUnhandled());
    }

    private static void assertBackInPoll() {
        assertTrue(AsyncMessageTracker.getHandlingState(TOPIC).isHandled(),
                "the poll was not replaced, as the thread's previous batch is still unhandled");
    }

    @Test
    public void testAPollThroughTheConsumerInterfaceIsTracked() throws Exception {

        int polled = getInstance().pollAConsumer(TOPIC, 2);

        assertEquals(2, polled);
        assertEquals(2, AsyncMessageTracker.getHandlingState(TOPIC).getUnhandled());
    }

    @Test
    public void testAPollOnAKafkaConsumerIsTracked() throws Exception {

        KafkaClients sut = getInstance();
        handlingARecord();

        assertThrows(IllegalStateException.class, sut::pollAnUnsubscribedKafkaConsumer);

        assertBackInPoll();
    }

    @Test
    public void testALegacyPollOnAKafkaConsumerIsTracked() throws Exception {

        KafkaClients sut = getInstance();
        handlingARecord();

        assertThrows(IllegalStateException.class, sut::pollAnUnsubscribedKafkaConsumerTheLegacyWay);

        assertBackInPoll();
    }

    @Test
    public void testASendWithAKafkaProducerIsRecordedOnce() throws Exception {

        boolean done = getInstance().sendWithAKafkaProducer(TOPIC, KEY, PAYLOAD);

        assertTrue(done, "with no broker to wait for, the send should be over at once");
        List<EmittedMessage> emitted = emitted();
        assertEquals(1, emitted.size(), "KafkaProducer calling itself recorded the record again: " + emitted);
        assertEquals(TOPIC, emitted.get(0).getAddress());
        assertEquals(PAYLOAD, emitted.get(0).getPayload());
    }

    @Test
    public void testASendWithSpringsKafkaTemplateIsRecordedOnce() throws Exception {

        int sent = getInstance().sendWithAKafkaTemplate(TOPIC, KEY, PAYLOAD);

        assertEquals(1, sent);
        List<EmittedMessage> emitted = emitted();
        assertEquals(1, emitted.size(), "the producer wrapping the actual one recorded the record again: " + emitted);
        assertEquals(PAYLOAD, emitted.get(0).getPayload());
    }

    @Test
    public void testWithoutTheKafkaCategoryNothingIsTracked() throws Exception {

        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0");
        try {
            getInstance().sendWithAKafkaProducer(TOPIC, KEY, PAYLOAD);
            getInstance().pollAConsumer(TOPIC, 1);
        } finally {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,KAFKA");
        }

        assertTrue(emitted().isEmpty());
        assertEquals(0, AsyncMessageTracker.getHandlingState(TOPIC).getReceived());
    }
}
