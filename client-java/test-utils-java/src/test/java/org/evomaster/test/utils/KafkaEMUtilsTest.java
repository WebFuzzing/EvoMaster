package org.evomaster.test.utils;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the generated tests' publish/reply helper with Kafka's in-memory clients. */
class KafkaEMUtilsTest {

    private static final String INPUT = "requests";
    private static final String REPLY = "replies";
    private static final String CORRELATION = "correlationId";
    private static final TopicPartition FIRST = new TopicPartition(REPLY, 0);
    private static final TopicPartition SECOND = new TopicPartition(REPLY, 1);

    private TrackingConsumer consumer;
    private TrackingProducer producer;
    private boolean consumerCreated;
    private boolean producerCreated;

    @BeforeEach
    void setUp() {
        consumer = new TrackingConsumer();
        producer = new TrackingProducer();
        consumerCreated = false;
        producerCreated = false;
        consumer.updatePartitions(REPLY, Arrays.asList(partition(0), partition(1)));
        consumer.updateBeginningOffsets(offsets(0, 0));
        consumer.updateEndOffsets(offsets(10, 20));
    }

    @Test
    void testFireAndForgetPreservesPayloadAndUtf8Headers() throws Exception {
        assertNull(publish(null, "héllo", null, 1000, "tenant", "á", "tenant", "β"));
        ProducerRecord<String, String> record = producer.history().get(0);
        assertEquals(INPUT, record.topic());
        assertNull(record.key());
        assertEquals("héllo", record.value());
        assertEquals(2, record.headers().toArray().length);
        assertEquals("β", text(record.headers().lastHeader("tenant")));
        assertFalse(consumerCreated);
        assertTrue(producer.closed());
    }

    @Test
    void testNullPayloadIsEmptyInsteadOfATombstone() throws Exception {
        publish(null, null, null, 1000);
        assertEquals("", producer.history().get(0).value());
    }

    @Test
    void testOddHeaderPairsAreRejectedBeforeCreatingClients() {
        assertThrows(IllegalArgumentException.class, () -> publish(REPLY, "body", null, 1000, "missing-value"));
        assertFalse(consumerCreated);
        assertFalse(producerCreated);
        assertThrows(IllegalArgumentException.class,
                () -> KafkaEMUtils.publishAndAwaitReply("unused:9092", INPUT, REPLY, "body", null, 0, "name"));
    }

    @Test
    void testPublicEntryPointRejectsInvalidBrokerConfiguration() {
        for (String reply : Arrays.asList(null, REPLY)) {
            Throwable failure = assertThrows(KafkaException.class,
                    () -> KafkaEMUtils.publishAndAwaitReply("", INPUT, reply, "body", null, 0));
            while (failure.getCause() != null) {
                failure = failure.getCause();
            }
            assertInstanceOf(ConfigException.class, failure);
        }
    }

    @Test
    void testEveryPartitionIsPositionedBeforePublishing() throws Exception {
        producer.onSend = record -> {
            assertEquals(offsets(10, 20), consumer.positions);
            //A reply arrives immediately after publishing: lazy seeks must already be resolved.
            consumer.updateEndOffsets(offsets(11, 21));
        };
        consumer.schedulePollTask(() -> {
            consumer.addRecord(reply(0, 9, "stale", null));
            consumer.addRecord(reply(1, 20, "fresh", null));
        });
        assertEquals("fresh", publish(REPLY, "body", null, 1000));
        assertClosed();
    }

    @Test
    void testUncorrelatedReplyUsesTheFirstMessage() throws Exception {
        consumer.schedulePollTask(() -> {
            consumer.addRecord(reply(0, 10, "first", null));
            consumer.addRecord(reply(0, 11, "second", "another-id"));
        });
        assertEquals("first", publish(REPLY, "body", null, 1000));
        assertClosed();
    }

    @Test
    void testCorrelationSkipsMissingNullAndMismatchedHeaders() throws Exception {
        consumer.schedulePollTask(() -> {
            consumer.addRecord(reply(0, 10, "missing", null));
            ConsumerRecord<String, String> nullHeader = reply(0, 11, "null", null);
            nullHeader.headers().add(CORRELATION, null);
            consumer.addRecord(nullHeader);
            consumer.addRecord(reply(0, 12, "wrong", "another-id"));
            consumer.addRecord(reply(0, 13, "matched", sentCorrelation()));
        });
        assertEquals("matched", publish(REPLY, "body", CORRELATION, 1000));
        assertClosed();
    }

    @Test
    void testLastCorrelationHeaderWins() throws Exception {
        consumer.schedulePollTask(() -> {
            ConsumerRecord<String, String> record = reply(0, 10, "matched", "old-id");
            record.headers().add(CORRELATION, sentCorrelation().getBytes(StandardCharsets.UTF_8));
            consumer.addRecord(record);
        });
        assertEquals("matched", publish(REPLY, "body", CORRELATION, 1000, CORRELATION, "old-id"));
        assertNotEquals("old-id", sentCorrelation());
        assertClosed();
    }

    @Test
    void testEachPublishUsesAFreshCorrelationId() throws Exception {
        publish(null, "body", CORRELATION, 0);
        String first = sentCorrelation();
        producer = new TrackingProducer();
        publish(null, "body", CORRELATION, 0);
        assertNotEquals(first, sentCorrelation());
    }

    @Test
    void testReplyCanArriveOnALaterPoll() throws Exception {
        consumer.scheduleNopPollTask();
        consumer.schedulePollTask(() -> consumer.addRecord(reply(0, 10, "late", sentCorrelation())));
        assertEquals("late", publish(REPLY, "body", CORRELATION, 1000));
        assertTrue(consumer.polls >= 2);
        assertClosed();
    }

    @Test
    void testTimeoutReturnsNullAndClosesClients() throws Exception {
        assertNull(publish(REPLY, "body", CORRELATION, 10));
        assertTrue(consumer.polls > 0);
        assertClosed();
    }

    @Test
    void testZeroTimeoutPublishesWithoutPolling() throws Exception {
        assertNull(publish(REPLY, "body", CORRELATION, 0));
        assertEquals(1, producer.history().size());
        assertEquals(0, consumer.polls);
        assertClosed();
    }

    @Test
    void testMissingReplyTopicStillPublishes() throws Exception {
        consumer.updatePartitions(REPLY, Collections.emptyList());
        assertNull(publish(REPLY, "body", CORRELATION, 1000));
        assertEquals(1, producer.history().size());
        assertEquals(0, consumer.polls);
        assertClosed();
    }

    @Test
    void testNullPartitionMetadataStillPublishes() throws Exception {
        consumer = new TrackingConsumer() {
            @Override
            public synchronized List<PartitionInfo> partitionsFor(String topic) {
                return null;
            }
        };
        assertNull(publish(REPLY, "body", CORRELATION, 1000));
        assertEquals(1, producer.history().size());
        assertClosed();
    }

    @Test
    void testMetadataFailureClosesConsumerWithoutPublishing() {
        KafkaException failure = new KafkaException("metadata unavailable");
        consumer = new TrackingConsumer() {
            @Override
            public synchronized List<PartitionInfo> partitionsFor(String topic) {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 1000)));
        assertTrue(consumer.closed());
        assertFalse(producerCreated);
    }

    @Test
    void testPositioningFailureClosesConsumerWithoutPublishing() {
        KafkaException failure = new KafkaException("offset unavailable");
        consumer.positionFailure = failure;
        assertSame(failure, assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 1000)));
        assertTrue(consumer.closed());
        assertFalse(producerCreated);
    }

    @Test
    void testProducerCreationFailureClosesConsumer() {
        KafkaException failure = new KafkaException("producer unavailable");
        assertSame(failure, assertThrows(KafkaException.class, () -> KafkaEMUtils.publishAndAwaitReply(
                () -> consumer, () -> { throw failure; }, INPUT, REPLY, "body", null, 1000)));
        assertTrue(consumer.closed());
    }

    @Test
    void testConsumerCreationFailureDoesNotCreateProducer() {
        KafkaException failure = new KafkaException("consumer unavailable");
        assertSame(failure, assertThrows(KafkaException.class, () -> KafkaEMUtils.publishAndAwaitReply(
                () -> { throw failure; }, () -> { producerCreated = true; return producer; },
                INPUT, REPLY, "body", null, 1000)));
        assertFalse(producerCreated);
    }

    @Test
    void testSynchronousSendFailureClosesBothClients() {
        KafkaException failure = new KafkaException("send failed");
        producer.sendException = failure;
        assertSame(failure, assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 1000)));
        assertClosed();
    }

    @Test
    void testAsynchronousSendFailureClosesBothClients() {
        KafkaException failure = new KafkaException("broker rejected send");
        producer = new TrackingProducer() {
            @Override
            public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
                future.completeExceptionally(failure);
                return future;
            }
        };
        ExecutionException thrown = assertThrows(ExecutionException.class, () -> publish(REPLY, "body", null, 1000));
        assertSame(failure, thrown.getCause());
        assertClosed();
    }

    @Test
    void testInterruptedSendClosesBothClients() {
        producer = new TrackingProducer() {
            @Override
            public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                return new CompletableFuture<>();
            }
        };
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, () -> publish(REPLY, "body", null, 1000));
            assertClosed();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void testPollFailureClosesBothClients() {
        KafkaException failure = new KafkaException("poll failed");
        consumer.setPollException(failure);
        assertSame(failure, assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 1000)));
        assertClosed();
    }

    @Test
    void testProducerCloseFailureStillClosesConsumer() {
        KafkaException failure = new KafkaException("close failed");
        producer.closeException = failure;
        assertSame(failure, assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 0)));
        assertTrue(consumer.closed());
    }

    @Test
    void testCleanupFailureDoesNotReplaceTheSendFailure() {
        KafkaException sendFailure = new KafkaException("send failed");
        KafkaException closeFailure = new KafkaException("close failed");
        producer.sendException = sendFailure;
        producer.closeException = closeFailure;
        Throwable thrown = assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 0));
        assertSame(sendFailure, thrown);
        assertArrayEquals(new Throwable[]{closeFailure}, thrown.getSuppressed());
        assertTrue(consumer.closed());
    }

    @Test
    void testConsumerCloseFailureIsReportedAfterProducerIsClosed() {
        KafkaException failure = new KafkaException("consumer close failed");
        consumer.closeFailure = failure;
        assertSame(failure, assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 0)));
        assertClosed();
    }

    @Test
    void testBothCleanupFailuresAreSuppressedOnTheOriginalFailure() {
        KafkaException failure = new KafkaException("poll failed");
        KafkaException producerFailure = new KafkaException("producer close failed");
        KafkaException consumerFailure = new KafkaException("consumer close failed");
        consumer.setPollException(failure);
        producer.closeException = producerFailure;
        consumer.closeFailure = consumerFailure;
        Throwable thrown = assertThrows(KafkaException.class, () -> publish(REPLY, "body", null, 1000));
        assertSame(failure, thrown);
        assertArrayEquals(new Throwable[]{producerFailure, consumerFailure}, thrown.getSuppressed());
        assertTrue(consumer.closed());
    }

    @Test
    void testHelperClassNameCanBeLoadedWithoutKafkaDependencies() throws Exception {
        URL classes = KafkaEMUtils.class.getProtectionDomain().getCodeSource().getLocation();
        URL tests = HelperNameProbe.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader isolated = new URLClassLoader(new URL[]{classes, tests}, null)) {
            assertThrows(ClassNotFoundException.class,
                    () -> isolated.loadClass("org.apache.kafka.clients.consumer.KafkaConsumer"));
            Class<?> probe = Class.forName(HelperNameProbe.class.getName(), true, isolated);
            assertEquals(KafkaEMUtils.class.getName(), probe.getMethod("helperName").invoke(null));
        }
    }

    private String publish(String replyTopic, String payload, String correlation, long timeout, String... headers)
            throws Exception {
        return KafkaEMUtils.publishAndAwaitReply(
                () -> { consumerCreated = true; return consumer; },
                () -> { producerCreated = true; return producer; },
                INPUT, replyTopic, payload, correlation, timeout, headers);
    }

    private void assertClosed() {
        assertTrue(producer.closed());
        assertTrue(consumer.closed());
    }

    private String sentCorrelation() {
        return text(producer.history().get(0).headers().lastHeader(CORRELATION));
    }

    private static String text(Header header) {
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static ConsumerRecord<String, String> reply(int partition, long offset, String value, String correlation) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(REPLY, partition, offset, null, value);
        if (correlation != null) {
            record.headers().add(CORRELATION, correlation.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    private static PartitionInfo partition(int id) {
        return new PartitionInfo(REPLY, id, null, null, null);
    }

    /** Reply partitions mapped to their offsets, in partition order. */
    private static Map<TopicPartition, Long> offsets(long first, long second) {
        Map<TopicPartition, Long> offsets = new LinkedHashMap<>();
        offsets.put(FIRST, first);
        offsets.put(SECOND, second);
        return offsets;
    }

    /** Mirrors the JVM class-literal expression used by the Kotlin emitter. */
    public static class HelperNameProbe {
        public static String helperName() {
            return KafkaEMUtils.class.getName();
        }
    }

    /** Records resolved positions so a lazy seek cannot be hidden by an assertion resolving it. */
    private static class TrackingConsumer extends MockConsumer<String, String> {
        /** Reply partition to the last offset resolved by the helper before publishing. */
        final Map<TopicPartition, Long> positions = new LinkedHashMap<>();
        int polls;
        RuntimeException positionFailure;
        RuntimeException closeFailure;

        TrackingConsumer() {
            super(OffsetResetStrategy.LATEST);
        }

        @Override
        public synchronized long position(TopicPartition partition) {
            if (positionFailure != null) {
                throw positionFailure;
            }
            long position = super.position(partition);
            positions.put(partition, position);
            return position;
        }

        @Override
        public void close() {
            super.close();
            if (closeFailure != null) {
                throw closeFailure;
            }
        }

        @Override
        public synchronized ConsumerRecords<String, String> poll(Duration timeout) {
            polls++;
            return super.poll(timeout);
        }
    }

    /** Lets a test observe the instant the helper publishes without changing Kafka's mock behavior. */
    private static class TrackingProducer extends MockProducer<String, String> {
        Consumer<ProducerRecord<String, String>> onSend = record -> {};

        TrackingProducer() {
            super(true, new StringSerializer(), new StringSerializer());
        }

        @Override
        public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
            onSend.accept(record);
            return super.send(record);
        }
    }
}
