package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.KafkaConsumerInterfaceClassReplacementTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Consumer.poll(long), against kafka-clients' own mock consumer.
 */
public class KafkaConsumerInterfaceLegacyPollClassReplacementTest {

    private static final long WAIT_MS = 10;

    @BeforeEach
    public void init() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void tearDown() {
        ExecutionTracer.setExecutingDriverAction(false);
        ExecutionTracer.reset();
    }

    @Test
    public void testWhatThePollReturnsIsRecordedAndHandledOnTheNextPoll() {

        MockConsumer<String, String> consumer = consumerWith(3);

        ConsumerRecords<?, ?> records = (ConsumerRecords<?, ?>)
                KafkaConsumerInterfaceLegacyPollClassReplacement.poll(consumer, WAIT_MS);

        assertEquals(3, records.count());
        assertEquals(3, state().getUnhandled());

        KafkaConsumerInterfaceLegacyPollClassReplacement.poll(consumer, WAIT_MS);

        assertTrue(state().isHandled());
    }

    @Test
    public void testAPollWithoutTimeoutLeavesTheBatchUnhandled() {

        MockConsumer<String, String> consumer = consumerWith(1);

        KafkaConsumerInterfaceLegacyPollClassReplacement.poll(consumer, WAIT_MS);
        KafkaConsumerInterfaceLegacyPollClassReplacement.poll(consumer, 0L);

        assertEquals(1, state().getUnhandled());
    }

    @Test
    public void testTheDriversPollsAreNotRecorded() {

        ExecutionTracer.setExecutingDriverAction(true);

        ConsumerRecords<?, ?> records = (ConsumerRecords<?, ?>)
                KafkaConsumerInterfaceLegacyPollClassReplacement.poll(consumerWith(1), WAIT_MS);

        assertEquals(1, records.count());
        assertEquals(0, state().getReceived());
    }
}
