package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.KafkaConsumerClassReplacementTest.handlingARecord;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.KafkaConsumerClassReplacementTest.unsubscribedConsumer;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.KafkaConsumerInterfaceClassReplacementTest.state;
import static org.junit.jupiter.api.Assertions.*;

/**
 * KafkaConsumer.poll(long), see {@link KafkaConsumerClassReplacementTest} for why an unsubscribed
 * consumer is enough.
 */
public class KafkaConsumerLegacyPollClassReplacementTest {

    private static final long WAIT_MS = 10;

    @BeforeEach
    public void init() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void tearDown() {
        ExecutionTracer.reset();
    }

    @Test
    public void testThePollReachesTheOriginalAndEndsThePreviousBatch() {

        handlingARecord();

        KafkaConsumer<String, String> consumer = unsubscribedConsumer();
        try {
            assertThrows(IllegalStateException.class, () -> KafkaConsumerLegacyPollClassReplacement.poll(consumer, WAIT_MS));
        } finally {
            consumer.close(Duration.ZERO);
        }

        assertTrue(state().isHandled(), "back in poll, yet the batch is not handled: " + state());
    }

    @Test
    public void testAPollWithoutTimeoutLeavesTheBatchUnhandled() {

        handlingARecord();

        KafkaConsumer<String, String> consumer = unsubscribedConsumer();
        try {
            assertThrows(IllegalStateException.class, () -> KafkaConsumerLegacyPollClassReplacement.poll(consumer, 0L));
        } finally {
            consumer.close(Duration.ZERO);
        }

        assertEquals(1, state().getUnhandled());
    }
}
