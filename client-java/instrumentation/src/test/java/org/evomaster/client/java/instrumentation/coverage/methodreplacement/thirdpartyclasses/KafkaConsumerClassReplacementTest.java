package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.evomaster.client.java.instrumentation.staticstate.AsyncMessageTracker;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.KafkaConsumerInterfaceClassReplacementTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * KafkaConsumer.poll(Duration). Without a broker a real KafkaConsumer returns no records, but an
 * unsubscribed one fails its poll straight away: enough to check that the call reaches the
 * original, its exception comes back unchanged, and the thread's previous batch is handled.
 */
public class KafkaConsumerClassReplacementTest {

    /**
     * Never contacted: an unsubscribed consumer fails before connecting.
     */
    static final String NO_BROKER = "localhost:9";

    private static final Duration CLOSE_TIMEOUT = Duration.ZERO;

    @BeforeEach
    public void init() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void tearDown() {
        ExecutionTracer.setExecutingDriverAction(false);
        ExecutionTracer.reset();
    }

    static KafkaConsumer<String, String> unsubscribedConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, NO_BROKER);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    /**
     * As if this thread had received a record from {@link KafkaConsumerInterfaceClassReplacementTest#TOPIC}
     * and was handling it.
     */
    static void handlingARecord() {
        AsyncMessageTracker.received(Collections.singletonList(
                TOPIC));
        assertEquals(1, state().getUnhandled());
    }

    @Test
    public void testThePollReachesTheOriginalAndEndsThePreviousBatch() {

        handlingARecord();

        KafkaConsumer<String, String> consumer = unsubscribedConsumer();
        try {
            assertThrows(IllegalStateException.class, () -> KafkaConsumerClassReplacement.poll(consumer, WAIT));
        } finally {
            consumer.close(CLOSE_TIMEOUT);
        }

        assertTrue(state().isHandled(), "back in poll, yet the batch is not handled: " + state());
    }

    @Test
    public void testTheDriversPollsDoNotEndTheSutsBatch() {

        handlingARecord();
        ExecutionTracer.setExecutingDriverAction(true);

        KafkaConsumer<String, String> consumer = unsubscribedConsumer();
        try {
            assertThrows(IllegalStateException.class, () -> KafkaConsumerClassReplacement.poll(consumer, WAIT));
        } finally {
            consumer.close(CLOSE_TIMEOUT);
        }

        assertEquals(1, state().getUnhandled());
    }
}
