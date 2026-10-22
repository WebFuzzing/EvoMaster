package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.evomaster.client.java.instrumentation.MessageHandlingState;
import org.evomaster.client.java.instrumentation.staticstate.AsyncMessageTracker;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consumer.poll(Duration), against kafka-clients' own mock consumer, so no broker is needed.
 */
public class KafkaConsumerInterfaceClassReplacementTest {

    static final String TOPIC = "requests";

    static final Duration WAIT = Duration.ofMillis(10);

    private static final TopicPartition PARTITION = new TopicPartition(TOPIC, 0);

    @BeforeEach
    public void init() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void tearDown() {
        ExecutionTracer.setExecutingDriverAction(false);
        ExecutionTracer.reset();
    }

    /**
     * A consumer assigned to {@link #TOPIC}, with {@code records} records waiting from offset 0.
     */
    static MockConsumer<String, String> consumerWith(int records) {
        MockConsumer<String, String> consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        consumer.assign(Collections.singletonList(PARTITION));
        consumer.updateBeginningOffsets(Collections.singletonMap(PARTITION, 0L));
        for (int i = 0; i < records; i++) {
            consumer.addRecord(new ConsumerRecord<>(TOPIC, 0, i, "key" + i, "value" + i));
        }
        return consumer;
    }

    static MessageHandlingState state() {
        return AsyncMessageTracker.getHandlingState(TOPIC);
    }

    @Test
    public void testWhatThePollReturnsIsRecordedAsConsumed() {

        ConsumerRecords<?, ?> records = (ConsumerRecords<?, ?>)
                KafkaConsumerInterfaceClassReplacement.poll(consumerWith(2), WAIT);

        assertEquals(2, records.count(), "the records did not reach the caller");

        MessageHandlingState state = state();
        assertEquals(2, state.getReceived());
        assertEquals(2, state.getUnhandled());
    }

    @Test
    public void testTheNextWaitingPollHandlesTheBatch() {

        MockConsumer<String, String> consumer = consumerWith(1);

        KafkaConsumerInterfaceClassReplacement.poll(consumer, WAIT);
        KafkaConsumerInterfaceClassReplacement.poll(consumer, WAIT);

        assertTrue(state().isHandled(), "back in poll, yet the batch is not handled: " + state());
    }

    @Test
    public void testAPollThatDoesNotWaitLeavesTheBatchUnhandled() {

        MockConsumer<String, String> consumer = consumerWith(1);

        KafkaConsumerInterfaceClassReplacement.poll(consumer, WAIT);
        KafkaConsumerInterfaceClassReplacement.poll(consumer, Duration.ZERO);

        assertEquals(1, state().getUnhandled());
    }

    @Test
    public void testAnEmptyPollRecordsNothing() {

        ConsumerRecords<?, ?> records = (ConsumerRecords<?, ?>)
                KafkaConsumerInterfaceClassReplacement.poll(consumerWith(0), WAIT);

        assertTrue(records.isEmpty());
        assertEquals(0, state().getReceived());
    }

    @Test
    public void testTheDriversPollsAreNotRecorded() {

        ExecutionTracer.setExecutingDriverAction(true);

        ConsumerRecords<?, ?> records = (ConsumerRecords<?, ?>)
                KafkaConsumerInterfaceClassReplacement.poll(consumerWith(1), WAIT);

        assertEquals(1, records.count(), "the driver did not get its records");
        assertEquals(0, state().getReceived(), "the driver's records were recorded as the SUT's");
    }

    @Test
    public void testTheDriverPollingDoesNotHandleTheSutsBatch() {

        KafkaConsumerInterfaceClassReplacement.poll(consumerWith(1), WAIT);

        ExecutionTracer.setExecutingDriverAction(true);
        KafkaConsumerInterfaceClassReplacement.poll(consumerWith(0), WAIT);

        assertEquals(1, state().getUnhandled());
    }

    @Test
    public void testAFailingPollThrowsAsTheOriginalAndStillEndsThePreviousBatch() {

        MockConsumer<String, String> consumer = consumerWith(1);
        KafkaConsumerInterfaceClassReplacement.poll(consumer, WAIT);

        KafkaException failure = new KafkaException("broker gone");
        consumer.setPollException(failure);

        KafkaException thrown = assertThrows(KafkaException.class,
                () -> KafkaConsumerInterfaceClassReplacement.poll(consumer, WAIT));

        assertSame(failure, thrown, "the exception was wrapped");
        assertTrue(state().isHandled(), "the thread came back for more, so it was done with the batch");
    }

    @Test
    public void testARecordPolledAgainIsUnhandledAgain() {

        MockConsumer<String, String> consumer = consumerWith(1);
        KafkaConsumerInterfaceClassReplacement.poll(consumer, WAIT);

        //as an error handler does after a failed record
        consumer.seek(PARTITION, 0);
        consumer.addRecord(new ConsumerRecord<>(TOPIC, 0, 0, "key0", "value0"));
        KafkaConsumerInterfaceClassReplacement.poll(consumer, WAIT);

        assertEquals(2, state().getReceived());
        assertEquals(1, state().getUnhandled(), "the record is being handled again");
    }
}
