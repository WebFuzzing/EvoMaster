package org.evomaster.client.java.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

public class KafkaAsyncApiDriverHelperTest {

    private static final String REQUEST = "orders.request";

    private static final String REPLY = "orders.reply";

    private static final String OTHER_REPLY = "invoices.reply";

    private static final String GROUP = "orders-service";

    private static final String OTHER_GROUP = "audit-service";

    private static final String ID = "cid-42";

    private static final String PAYLOAD = "{\"item\":\"book\",\"quantity\":2}";

    private static final String REPLY_PAYLOAD = "{\"accepted\":true}";

    /**
     * Where the reply topics end when a consumer is created on them: anything below was there
     * before the action, and must never be taken for its reply.
     */
    private static final long REPLY_END = 5;

    /**
     * Short, as the mock consumers never block: a reply that is not there is known at once,
     * and this only bounds how long the helper keeps asking.
     */
    private static final long SHORT_TIMEOUT_MS = 50;

    private static final TopicPartition REQUEST_0 = new TopicPartition(REQUEST, 0);

    private static final TopicPartition REQUEST_1 = new TopicPartition(REQUEST, 1);

    /**
     * What happened across the producer and the consumers, in order.
     */
    private final List<String> events = new ArrayList<>();

    /**
     * Every consumer the helper asked for, in order.
     */
    private final List<RecordingConsumer> consumers = new ArrayList<>();

    private MockProducer<String, String> producer;

    private Admin admin;

    private int adminsCreated;

    private KafkaAsyncApiDriverHelper helper;

    /**
     * Whether the consumers the helper asks for find {@link #REPLY} on the broker.
     */
    private boolean replyTopicExists;

    /**
     * What asking a new consumer for the partitions of a topic throws, when not null.
     */
    private RuntimeException partitionsForFailure;

    @BeforeEach
    public void init() {
        events.clear();
        consumers.clear();
        adminsCreated = 0;
        replyTopicExists = true;
        partitionsForFailure = null;
        producer = new RecordingProducer();
        admin = mock(Admin.class);
        helper = new KafkaAsyncApiDriverHelper(producer, this::newConsumer, () -> {
            adminsCreated++;
            return admin;
        });
    }

    private RecordingConsumer newConsumer() {
        RecordingConsumer consumer = new RecordingConsumer();
        for (String topic : Arrays.asList(REPLY, OTHER_REPLY)) {
            if (topic.equals(REPLY) && !replyTopicExists) {
                continue;
            }
            consumer.updatePartitions(topic, Collections.singletonList(
                    new PartitionInfo(topic, 0, Node.noNode(), new Node[0], new Node[0])));
            consumer.updateEndOffsets(Collections.singletonMap(new TopicPartition(topic, 0), REPLY_END));
        }
        consumers.add(consumer);
        events.add("consumer created");
        return consumer;
    }

    private class RecordingProducer extends MockProducer<String, String> {

        RecordingProducer() {
            super(true, new StringSerializer(), new StringSerializer());
        }

        @Override
        public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
            events.add("published to " + record.topic());
            return super.send(record);
        }
    }

    private class RecordingConsumer extends MockConsumer<String, String> {

        private final RuntimeException partitionsForFailure = KafkaAsyncApiDriverHelperTest.this.partitionsForFailure;

        RecordingConsumer() {
            super(OffsetResetStrategy.LATEST);
        }

        @Override
        public synchronized List<PartitionInfo> partitionsFor(String topic) {
            if (partitionsForFailure != null) {
                throw partitionsForFailure;
            }
            return super.partitionsFor(topic);
        }

        /**
         * Have {@link #REPLY} appear on the broker, with {@code records} on it from its start.
         */
        @SafeVarargs
        final void createReplyTopicWith(ConsumerRecord<String, String>... records) {
            TopicPartition partition = new TopicPartition(REPLY, 0);
            updatePartitions(REPLY, Collections.singletonList(
                    new PartitionInfo(REPLY, 0, Node.noNode(), new Node[0], new Node[0])));
            updateBeginningOffsets(Collections.singletonMap(partition, 0L));
            updateEndOffsets(Collections.singletonMap(partition, (long) records.length));
            schedulePollTask(() -> Arrays.stream(records).forEach(this::addRecord));
        }

        @Override
        public synchronized long position(TopicPartition partition) {
            long position = super.position(partition);
            events.add("positioned on " + partition.topic() + " at " + position);
            return position;
        }
    }

    private static AsyncApiActionDto fireAndForget() {
        AsyncApiActionDto dto = new AsyncApiActionDto();
        dto.address = REQUEST;
        dto.payload = PAYLOAD;
        dto.correlationId = ID;
        return dto;
    }

    /**
     * Correlated through an explicit pointer to the default header.
     */
    private static AsyncApiActionDto requestReply() {
        AsyncApiActionDto dto = fireAndForget();
        dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.HEADER;
        dto.correlationPointer = "/" + KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER;
        dto.replyAddress = REPLY;
        dto.replyTimeoutMs = SHORT_TIMEOUT_MS;
        return dto;
    }

    private static ConsumerRecord<String, String> replyRecord(long offset, String value, String correlationHeader) {
        return replyRecord(offset, value, correlationHeader, ConsumerRecord.NO_TIMESTAMP);
    }

    private static ConsumerRecord<String, String> replyRecord(
            long offset, String value, String correlationHeader, long timestamp) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(REPLY, 0, offset, timestamp,
                TimestampType.CREATE_TIME, ConsumerRecord.NULL_SIZE, ConsumerRecord.NULL_SIZE, null, value,
                new RecordHeaders(), Optional.empty());
        if (correlationHeader != null) {
            record.headers().add(KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER,
                    correlationHeader.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    /**
     * Have the next poll of the latest consumer find these records on the reply topic, as if the
     * service had answered after the publish.
     */
    @SafeVarargs
    private final void answerWith(ConsumerRecord<String, String>... records) {
        RecordingConsumer consumer = consumers.get(consumers.size() - 1);
        consumer.schedulePollTask(() -> Arrays.stream(records).forEach(consumer::addRecord));
    }

    private static String header(ProducerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private ProducerRecord<String, String> lastPublished() {
        List<ProducerRecord<String, String>> history = producer.history();
        return history.get(history.size() - 1);
    }

    // ---------------------------------------------------------------- publishing

    @Test
    public void testAMessageIsPublishedWithItsHeadersAndNoKey() {

        AsyncApiActionDto dto = fireAndForget();
        dto.headers.put("tenant", "acme");

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        ProducerRecord<String, String> record = lastPublished();
        assertEquals(REQUEST, record.topic());
        assertNull(record.key(), "the tests EvoMaster generates publish with no key, and so must the driver");
        assertEquals(PAYLOAD, record.value());
        assertEquals("acme", header(record, "tenant"));
        assertEquals(1, record.headers().toArray().length,
                "with no correlation declared, the generated tests publish no correlation header: " + record.headers());

        assertTrue(reply.published);
        assertFalse(reply.replyExpected);
        assertNull(reply.replyReceived);
        assertTrue(consumers.isEmpty(), "a consumer was created for a message that expects no reply");
    }

    @Test
    public void testInvalidHeaderPointersAreRejectedBeforePublishing() {
        for (String pointer : new String[]{null, "/", "/meta/id"}) {
            AsyncApiActionDto dto = fireAndForget();
            dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.HEADER;
            dto.correlationPointer = pointer;
            assertThrows(IllegalArgumentException.class,
                    () -> helper.executeAsyncApiAction(dto, new AsyncApiReplyDto()));
        }
    }

    @Test
    public void testAMessageWithNoPayloadIsPublishedEmptyAsInTheGeneratedTests() {

        AsyncApiActionDto dto = fireAndForget();
        dto.payload = null;
        dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.PAYLOAD;
        dto.correlationPointer = "/meta/id";

        helper.executeAsyncApiAction(dto, new AsyncApiReplyDto());

        assertEquals("", lastPublished().value(), "a null value is a tombstone, which the generated tests never publish");
    }

    @Test
    public void testTheCorrelationHeaderIsNamedAfterThePointer() {

        AsyncApiActionDto dto = fireAndForget();
        dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.HEADER;
        dto.correlationPointer = "/x-request-id";

        helper.executeAsyncApiAction(dto, new AsyncApiReplyDto());

        ProducerRecord<String, String> record = lastPublished();
        assertEquals(ID, header(record, "x-request-id"));
        assertNull(header(record, KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER));
    }

    @Test
    public void testEscapedCorrelationHeaderNames() {
        AsyncApiActionDto dto = fireAndForget();
        dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.HEADER;
        dto.correlationPointer = "/x~1request~0id";
        helper.executeAsyncApiAction(dto, new AsyncApiReplyDto());
        assertEquals(ID, header(lastPublished(), "x/request~id"));
    }

    @Test
    public void testAPayloadCorrelationIdIsWrittenIntoThePayloadAndNoHeader() {

        AsyncApiActionDto dto = fireAndForget();
        dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.PAYLOAD;
        dto.correlationPointer = "/meta/id";

        helper.executeAsyncApiAction(dto, new AsyncApiReplyDto());

        ProducerRecord<String, String> record = lastPublished();
        assertEquals("{\"item\":\"book\",\"quantity\":2,\"meta\":{\"id\":\"" + ID + "\"}}", record.value());
        assertFalse(record.headers().iterator().hasNext(), "headers were added: " + record.headers());
    }

    @Test
    public void testNullHeadersAndNoCorrelationIdPublishThePayloadAlone() {

        AsyncApiActionDto dto = fireAndForget();
        dto.headers = null;
        dto.correlationId = null;

        helper.executeAsyncApiAction(dto, new AsyncApiReplyDto());

        ProducerRecord<String, String> record = lastPublished();
        assertNull(record.key());
        assertEquals(PAYLOAD, record.value());
        assertFalse(record.headers().iterator().hasNext(), "headers were added: " + record.headers());
    }

    @Test
    public void testAMessageThatCannotBePublishedIsABrokenSetup() {

        producer = new MockProducer<String, String>(false, new StringSerializer(), new StringSerializer()) {
            @Override
            public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                Future<RecordMetadata> sent = super.send(record);
                errorNext(new IllegalStateException("not enough replicas"));
                return sent;
            }
        };
        helper = new KafkaAsyncApiDriverHelper(producer, this::newConsumer, () -> admin);

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> helper.executeAsyncApiAction(fireAndForget(), reply));

        assertTrue(e.getMessage().contains(REQUEST), e.getMessage());
        assertNull(reply.published, "reported as published although it was not");
    }

    @Test
    public void testInterruptedPublishPreservesInterrupt() {
        producer = new MockProducer<>(false, new StringSerializer(), new StringSerializer());
        helper = new KafkaAsyncApiDriverHelper(producer, this::newConsumer, () -> admin);
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        Thread.currentThread().interrupt();
        try {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> helper.executeAsyncApiAction(fireAndForget(), reply));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            assertNotEquals(Boolean.TRUE, reply.published);
        } finally {
            Thread.interrupted();
        }
    }

    // ---------------------------------------------------------------- replies

    @Test
    public void testTheReplyConsumerIsAtTheEndOfTheTopicBeforeThePublish() {

        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());

        assertEquals(Arrays.asList(
                "consumer created",
                "positioned on " + REPLY + " at " + REPLY_END,
                "published to " + REQUEST), events);
    }

    @Test
    public void testOnlyTheReplyCarryingTheCorrelationIdIsTaken() {

        AsyncApiActionDto dto = requestReply();
        dto.replyTimeoutMs = 10_000L;

        //created up front, so that the answer can be scheduled on it
        helper.executeAsyncApiAction(fireAndForgetTo(REPLY), new AsyncApiReplyDto());
        answerWith(
                //on the topic before the action: never an answer to it, even with the same id
                replyRecord(REPLY_END - 1, "{\"stale\":true}", ID),
                replyRecord(REPLY_END, "{\"other\":true}", "cid-other"),
                replyRecord(REPLY_END + 1, REPLY_PAYLOAD, ID));

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        assertTrue(reply.published);
        assertTrue(reply.replyExpected);
        assertTrue(reply.replyReceived);
        assertEquals(REPLY_PAYLOAD, reply.replyPayload);
        assertTrue(reply.correlationMatched);
        assertEquals(ID, reply.replyHeaders.get(KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER));
        assertNotNull(reply.waitedMs);
    }

    /**
     * A fire-and-forget message whose address happens to be a reply topic, only to have the
     * consumer on it created before the action under test.
     */
    private AsyncApiActionDto fireAndForgetTo(String replyAddress) {
        AsyncApiActionDto dto = requestReply();
        dto.replyAddress = replyAddress;
        dto.replyTimeoutMs = 1L;
        return dto;
    }

    @Test
    public void testAReplyWithoutACorrelationIdIsTakenAsUncorrelated() {

        helper.executeAsyncApiAction(fireAndForgetTo(REPLY), new AsyncApiReplyDto());
        answerWith(replyRecord(REPLY_END, REPLY_PAYLOAD, null));

        AsyncApiActionDto dto = requestReply();
        dto.replyTimeoutMs = 10_000L;
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        assertTrue(reply.replyReceived);
        assertFalse(reply.correlationMatched);
        assertEquals(REPLY_PAYLOAD, reply.replyPayload);
    }

    @Test
    public void testANullCorrelationHeaderIsTakenAsUncorrelated() {
        helper.executeAsyncApiAction(fireAndForgetTo(REPLY), new AsyncApiReplyDto());
        ConsumerRecord<String, String> record = replyRecord(REPLY_END, REPLY_PAYLOAD, null);
        record.headers().add(KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER, null);
        answerWith(record);
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(requestReply(), reply);
        assertTrue(reply.replyReceived);
        assertFalse(reply.correlationMatched);
        assertEquals(REPLY_PAYLOAD, reply.replyPayload);
    }

    @Test
    public void testWithNoCorrelationDeclaredTheFirstReplyIsTaken() {

        helper.executeAsyncApiAction(fireAndForgetTo(REPLY), new AsyncApiReplyDto());
        answerWith(replyRecord(REPLY_END, REPLY_PAYLOAD, "cid-other"));

        AsyncApiActionDto dto = requestReply();
        dto.correlationLocation = null;
        dto.replyTimeoutMs = 10_000L;
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        assertNull(header(lastPublished(), KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER));
        assertTrue(reply.replyReceived, "the generated tests take the first reply, whatever header it carries");
        assertFalse(reply.correlationMatched);
        assertEquals(REPLY_PAYLOAD, reply.replyPayload);
    }

    @Test
    public void testAReplyIsMatchedOnTheCorrelationIdInItsPayload() {

        helper.executeAsyncApiAction(fireAndForgetTo(REPLY), new AsyncApiReplyDto());
        answerWith(
                replyRecord(REPLY_END, "{\"meta\":{\"id\":\"cid-other\"}}", null),
                replyRecord(REPLY_END + 1, "{\"meta\":{\"id\":\"" + ID + "\"},\"ok\":1}", null));

        AsyncApiActionDto dto = requestReply();
        dto.replyTimeoutMs = 10_000L;
        dto.correlationLocation = AsyncApiActionDto.CorrelationLocation.PAYLOAD;
        dto.correlationPointer = "/meta/id";

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        assertTrue(reply.replyReceived);
        assertTrue(reply.correlationMatched);
        assertEquals("{\"meta\":{\"id\":\"" + ID + "\"},\"ok\":1}", reply.replyPayload);
    }

    @Test
    public void testAReplyThatNeverComesIsReportedAsWaitedForTheWholeTimeout() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(requestReply(), reply);

        assertTrue(reply.published);
        assertTrue(reply.replyExpected);
        assertFalse(reply.replyReceived);
        assertNull(reply.replyPayload);
        assertEquals(SHORT_TIMEOUT_MS, reply.waitedMs);
    }

    @Test
    public void testReplyConsumersAreKeptPerAddressUntilReset() {

        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());
        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());
        assertEquals(1, consumers.size(), "a second consumer was created on the same reply address");

        helper.executeAsyncApiAction(fireAndForgetTo(OTHER_REPLY), new AsyncApiReplyDto());
        assertEquals(2, consumers.size());

        helper.resetReplyConsumers();
        assertTrue(consumers.stream().allMatch(MockConsumer::closed), "a reply consumer was left open");

        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());
        assertEquals(3, consumers.size(), "the next reply after a reset needs a new consumer, at the end of its topic");
    }

    @Test
    public void testTheReplyConsumerIsMovedToTheEndBeforeEveryPublish() {

        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());

        //a late answer to the first message, with no correlation id, lands after it gave up
        RecordingConsumer consumer = consumers.get(0);
        consumer.addRecord(replyRecord(REPLY_END, "{\"late\":true}", null));
        consumer.updateEndOffsets(Collections.singletonMap(new TopicPartition(REPLY, 0), REPLY_END + 1));
        events.clear();

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(requestReply(), reply);

        assertEquals(Arrays.asList(
                "positioned on " + REPLY + " at " + (REPLY_END + 1),
                "published to " + REQUEST), events);
        assertFalse(reply.replyReceived, "the late answer to the first message was taken for the second one's");
        assertNull(reply.replyPayload);
    }

    @Test
    public void testAReplyAddressThatAppearsAfterThePublishIsReadFromItsStart() {

        replyTopicExists = false;
        //the service creates the reply topic when it first answers
        producer = new RecordingProducer() {
            @Override
            public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                consumers.get(consumers.size() - 1).createReplyTopicWith(replyRecord(0, REPLY_PAYLOAD, ID));
                return super.send(record);
            }
        };
        helper = new KafkaAsyncApiDriverHelper(producer, this::newConsumer, () -> admin);

        AsyncApiActionDto dto = requestReply();
        dto.replyTimeoutMs = 10_000L;
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        assertTrue(reply.replyReceived, "the reply on a topic created after the publish was missed");
        assertEquals(REPLY_PAYLOAD, reply.replyPayload);
        assertTrue(reply.correlationMatched);

        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());
        assertEquals(1, consumers.size(), "once assigned, the consumer is kept");
        assertFalse(consumers.get(0).closed());
    }

    @Test
    public void testAReplyAddressThatAppearsAfterThePublishSkipsWhatIsOlderThanThePublish() {

        replyTopicExists = false;
        long future = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        producer = new RecordingProducer() {
            @Override
            public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                consumers.get(consumers.size() - 1).createReplyTopicWith(
                        //an answer to some earlier request with the same id, eg from a previous run
                        replyRecord(0, "{\"stale\":true}", ID, 1L),
                        replyRecord(1, REPLY_PAYLOAD, ID, future));
                return super.send(record);
            }
        };
        helper = new KafkaAsyncApiDriverHelper(producer, this::newConsumer, () -> admin);

        AsyncApiActionDto dto = requestReply();
        dto.replyTimeoutMs = 10_000L;
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        assertTrue(reply.replyReceived);
        assertEquals(REPLY_PAYLOAD, reply.replyPayload, "a record older than the publish was taken for its reply");
    }

    @Test
    public void testLookingForTheReplyAddressIsBoundedByTheReplyTimeout() {

        replyTopicExists = false;
        List<Duration> asked = new ArrayList<>();
        helper = new KafkaAsyncApiDriverHelper(producer, () -> {
            RecordingConsumer consumer = new RecordingConsumer() {
                @Override
                public List<PartitionInfo> partitionsFor(String topic, Duration timeout) {
                    asked.add(timeout);
                    throw new TimeoutException("no metadata yet");
                }
            };
            consumers.add(consumer);
            return consumer;
        }, () -> admin);

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(requestReply(), reply);

        assertFalse(reply.replyReceived);
        assertNull(reply.errorMessage, "running out of time looking for the address is not a failure");
        assertFalse(asked.isEmpty());
        assertTrue(asked.stream().allMatch(d -> d.toMillis() <= SHORT_TIMEOUT_MS), "asked for longer than the reply timeout: " + asked);
    }

    @Test
    public void testAnInterruptedWaitReportsTheTimeItActuallyWaited() {

        helper.executeAsyncApiAction(fireAndForgetTo(REPLY), new AsyncApiReplyDto());
        consumers.get(0).schedulePollTask(() -> Thread.currentThread().interrupt());

        AsyncApiActionDto dto = requestReply();
        dto.replyTimeoutMs = 60_000L;
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        try {
            helper.executeAsyncApiAction(dto, reply);
        } finally {
            assertTrue(Thread.interrupted(), "the interrupt was not kept on the thread");
        }

        assertFalse(reply.replyReceived);
        assertTrue(reply.waitedMs < dto.replyTimeoutMs, "reported the whole timeout: " + reply.waitedMs);
    }

    @Test
    public void testLookingForAReplyTopicDoesNotCreateIt() {

        Properties props = KafkaAsyncApiDriverHelper.consumerProperties("localhost:9092", new Properties());

        assertEquals("false", props.get(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG));
    }

    @Test
    public void testAConsumerOnAReplyAddressThatNeverAppearsIsNotKept() {

        replyTopicExists = false;

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(requestReply(), reply);

        assertTrue(reply.published);
        assertFalse(reply.replyReceived);
        assertEquals(SHORT_TIMEOUT_MS, reply.waitedMs);
        assertTrue(consumers.get(0).closed(), "a consumer with no partitions was left open");

        //the next action looks for the address again, rather than polling a consumer with nothing assigned
        replyTopicExists = true;
        AsyncApiReplyDto next = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(requestReply(), next);

        assertEquals(2, consumers.size());
        assertEquals(Collections.singleton(new TopicPartition(REPLY, 0)), consumers.get(1).assignment());
        assertFalse(next.replyReceived);
        assertNull(next.errorMessage);
    }

    @Test
    public void testAFailureWhileAwaitingTheReplyIsReportedAsNoReply() {

        helper.executeAsyncApiAction(fireAndForgetTo(REPLY), new AsyncApiReplyDto());
        consumers.get(0).setPollException(new KafkaException("connection to the broker lost"));

        AsyncApiActionDto dto = requestReply();
        dto.replyTimeoutMs = 10_000L;
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        helper.executeAsyncApiAction(dto, reply);

        assertTrue(reply.published, "the message did go out");
        assertTrue(reply.replyExpected);
        assertFalse(reply.replyReceived);
        assertNotNull(reply.waitedMs);
        assertTrue(reply.errorMessage.contains(REPLY), reply.errorMessage);
        assertTrue(reply.errorMessage.contains("connection to the broker lost"), reply.errorMessage);
        assertTrue(consumers.get(0).closed(), "a consumer that failed was kept");

        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());
        assertEquals(2, consumers.size(), "the next action needs a new consumer");
    }

    @Test
    public void testAConsumerThatCannotBePositionedIsClosedAndNothingPublished() {

        partitionsForFailure = new KafkaException("metadata not available");

        assertThrows(KafkaException.class, () -> helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto()));

        assertTrue(consumers.get(0).closed(), "the consumer was leaked");
        assertTrue(producer.history().isEmpty(), "published although the reply could not be awaited");
    }

    // ---------------------------------------------------------------- start-up

    @Test
    public void testTopicsAreCreatedOnceEachAndOnesThatExistAreLeftAlone() {

        CreateTopicsResult result = mock(CreateTopicsResult.class);
        Map<String, KafkaFuture<Void>> outcomes = new LinkedHashMap<>();
        outcomes.put(REQUEST, KafkaFuture.completedFuture(null));
        outcomes.put(REPLY, failed(new TopicExistsException(REPLY)));
        when(result.values()).thenReturn(outcomes);
        when(admin.createTopics(anyCollection())).thenReturn(result);

        helper.createTopics(Arrays.asList(REQUEST, REPLY, REQUEST));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<NewTopic>> created = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(created.capture());
        assertEquals(Arrays.asList(REQUEST, REPLY),
                created.getValue().stream().map(NewTopic::name).collect(Collectors.toList()));
        assertTrue(created.getValue().stream().allMatch(t -> t.numPartitions() == 1 && t.replicationFactor() == 1));
    }

    @Test
    public void testATopicThatCannotBeCreatedFailsTheStart() {

        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.values()).thenReturn(Collections.singletonMap(REQUEST, failed(new IllegalStateException("denied"))));
        when(admin.createTopics(anyCollection())).thenReturn(result);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> helper.createTopics(Collections.singletonList(REQUEST)));
        assertTrue(e.getMessage().contains(REQUEST), e.getMessage());
    }

    @Test
    public void testNoTopicsNeedNoAdminClient() {

        helper.createTopics(Collections.emptyList());

        assertEquals(0, adminsCreated);
    }

    @Test
    public void testTheServiceIsReadyOnceItAnswersAProbe() {

        AsyncApiActionDto probe = requestReply();
        probe.correlationId = "ignored";

        //the consumer is created by the first attempt, so the answer is scheduled from the producer
        producer = new RecordingProducer() {
            @Override
            public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                answerWith(replyRecord(REPLY_END, REPLY_PAYLOAD,
                        header(record, KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER)));
                return super.send(record);
            }
        };
        helper = new KafkaAsyncApiDriverHelper(producer, this::newConsumer, () -> admin);

        helper.awaitAnswering(probe, 10_000);

        String probeId = header(lastPublished(), KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER);
        assertTrue(probeId.startsWith("evomaster-probe-"), probeId);
        assertEquals("ignored", probe.correlationId, "the probe handed in was changed");
    }

    @Test
    public void testAProbeWithNoCorrelationLocationIsCorrelatedInTheDefaultHeader() {

        AsyncApiActionDto probe = requestReply();
        probe.correlationLocation = null;

        producer = new RecordingProducer() {
            @Override
            public synchronized Future<RecordMetadata> send(ProducerRecord<String, String> record) {
                answerWith(replyRecord(REPLY_END, REPLY_PAYLOAD,
                        header(record, KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER)));
                return super.send(record);
            }
        };
        helper = new KafkaAsyncApiDriverHelper(producer, this::newConsumer, () -> admin);

        helper.awaitAnswering(probe, 10_000);

        assertNull(probe.correlationLocation, "the probe handed in was changed");
    }

    @Test
    public void testAServiceThatNeverAnswersTheProbeFailsTheStart() {

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> helper.awaitAnswering(requestReply(), SHORT_TIMEOUT_MS));

        assertTrue(e.getMessage().contains(REQUEST), e.getMessage());
        assertTrue(producer.history().size() >= 1, "no probe was published");
        //each probe has an id of its own, so that the answer to an earlier one is not taken for a later one
        assertEquals(producer.history().size(), producer.history().stream()
                .map(r -> header(r, KafkaAsyncApiDriverHelper.DEFAULT_CORRELATION_HEADER)).distinct().count());
    }

    @Test
    public void testAProbeNeedsAReplyAddress() {

        assertThrows(IllegalArgumentException.class, () -> helper.awaitAnswering(fireAndForget(), SHORT_TIMEOUT_MS));
    }

    @Test
    public void testAStableGroupWithPartitionsIsAssigned() {

        describing(group(GROUP, ConsumerGroupState.STABLE, REQUEST_0));

        helper.awaitConsumerGroupsAssigned(Collections.singletonList(GROUP), 10_000);
    }

    @Test
    public void testAGroupStillRebalancingIsNotAssigned() {

        describing(group(GROUP, ConsumerGroupState.PREPARING_REBALANCE, REQUEST_0));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> helper.awaitConsumerGroupsAssigned(Collections.singletonList(GROUP), SHORT_TIMEOUT_MS));
        assertTrue(e.getMessage().contains(GROUP), e.getMessage());
    }

    @Test
    public void testAStableGroupWithNoPartitionsIsNotAssigned() {

        describing(group(GROUP, ConsumerGroupState.STABLE));

        assertThrows(IllegalStateException.class,
                () -> helper.awaitConsumerGroupsAssigned(Collections.singletonList(GROUP), SHORT_TIMEOUT_MS));
    }

    // ---------------------------------------------------------------- consumer lag

    private static ConsumerGroupDescription group(String id, ConsumerGroupState state, TopicPartition... assigned) {
        List<MemberDescription> members = assigned.length == 0
                ? Collections.emptyList()
                : Collections.singletonList(new MemberDescription(id + "-member", "client", "host",
                new MemberAssignment(new HashSet<>(Arrays.asList(assigned)))));
        return new ConsumerGroupDescription(id, false, members, "range", state, Node.noNode());
    }

    private void describing(ConsumerGroupDescription... groups) {
        Map<String, KafkaFuture<ConsumerGroupDescription>> described = new LinkedHashMap<>();
        for (ConsumerGroupDescription group : groups) {
            described.put(group.groupId(), KafkaFuture.completedFuture(group));
        }
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(new DescribeConsumerGroupsResult(described));
    }

    private void committing(String groupId, Map<TopicPartition, OffsetAndMetadata> offsets) {
        ListConsumerGroupOffsetsResult result = mock(ListConsumerGroupOffsetsResult.class);
        when(result.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(offsets));
        when(admin.listConsumerGroupOffsets(groupId)).thenReturn(result);
    }

    private static Map<TopicPartition, OffsetAndMetadata> offsets(Object... partitionThenOffset) {
        Map<TopicPartition, OffsetAndMetadata> offsets = new LinkedHashMap<>();
        for (int i = 0; i < partitionThenOffset.length; i += 2) {
            offsets.put((TopicPartition) partitionThenOffset[i], new OffsetAndMetadata((Long) partitionThenOffset[i + 1]));
        }
        return offsets;
    }

    private void ending(Object... partitionThenOffset) {
        Map<TopicPartition, KafkaFuture<ListOffsetsResult.ListOffsetsResultInfo>> ends = new LinkedHashMap<>();
        for (int i = 0; i < partitionThenOffset.length; i += 2) {
            ends.put((TopicPartition) partitionThenOffset[i], KafkaFuture.completedFuture(
                    new ListOffsetsResult.ListOffsetsResultInfo((Long) partitionThenOffset[i + 1], 0L, Optional.empty())));
        }
        when(admin.listOffsets(anyMap())).thenReturn(new ListOffsetsResult(ends));
    }

    private static <T> KafkaFuture<T> failed(Throwable cause) {
        KafkaFutureImpl<T> future = new KafkaFutureImpl<>();
        future.completeExceptionally(cause);
        return future;
    }

    private Boolean caughtUp(String... groupIds) {
        return helper.consumerGroupsCaughtUp(Arrays.asList(groupIds));
    }

    @Test
    public void testAGroupThatCommittedTheEndOfEveryPartitionHasCaughtUp() {

        describing(group(GROUP, ConsumerGroupState.STABLE, REQUEST_0, REQUEST_1));
        committing(GROUP, offsets(REQUEST_0, 7L, REQUEST_1, 3L));
        ending(REQUEST_0, 7L, REQUEST_1, 3L);

        assertEquals(Boolean.TRUE, caughtUp(GROUP));
    }

    @Test
    public void testAGroupBehindOnAnyPartitionHasNotCaughtUp() {

        describing(group(GROUP, ConsumerGroupState.STABLE, REQUEST_0, REQUEST_1));
        committing(GROUP, offsets(REQUEST_0, 7L, REQUEST_1, 2L));
        ending(REQUEST_0, 7L, REQUEST_1, 3L);

        assertEquals(Boolean.FALSE, caughtUp(GROUP));
    }

    @Test
    public void testAnAssignedPartitionNeverCommittedOnCountsAsReadFromTheStart() {

        describing(group(GROUP, ConsumerGroupState.STABLE, REQUEST_0, REQUEST_1));
        committing(GROUP, offsets(REQUEST_0, 7L));

        ending(REQUEST_0, 7L, REQUEST_1, 1L);
        assertEquals(Boolean.FALSE, caughtUp(GROUP), "a message on a partition the group never committed on was missed");

        ending(REQUEST_0, 7L, REQUEST_1, 0L);
        assertEquals(Boolean.TRUE, caughtUp(GROUP), "an empty partition has nothing to catch up with");
    }

    @Test
    public void testACommittedPartitionNoLongerAssignedIsStillWatched() {

        //eg a group whose member left, with the offsets it committed still there
        describing(group(GROUP, ConsumerGroupState.EMPTY));
        committing(GROUP, offsets(REQUEST_0, 4L));
        ending(REQUEST_0, 6L);

        assertEquals(Boolean.FALSE, caughtUp(GROUP));
    }

    @Test
    public void testACommittedGroupWithNoMembersIsJudgedOnItsOffsets() {

        describing(group(GROUP, ConsumerGroupState.EMPTY));
        committing(GROUP, offsets(REQUEST_0, 6L));
        ending(REQUEST_0, 6L);

        assertEquals(Boolean.TRUE, caughtUp(GROUP));
    }

    @Test
    public void testAStableGroupThatReadsNothingHasNothingToCatchUpWith() {

        describing(group(GROUP, ConsumerGroupState.STABLE));
        committing(GROUP, offsets());

        assertEquals(Boolean.TRUE, caughtUp(GROUP));
        verify(admin, never()).listOffsets(anyMap());
    }

    @Test
    public void testAGroupThatHasNotStartedReadingHasNotCaughtUp() {

        //eg the service's consumer has not joined yet: nothing it has not read can be told apart
        describing(group(GROUP, ConsumerGroupState.EMPTY));
        committing(GROUP, offsets());

        assertEquals(Boolean.FALSE, caughtUp(GROUP));

        committing(GROUP, Collections.singletonMap(REQUEST_0, null));
        assertEquals(Boolean.FALSE, caughtUp(GROUP), "a partition with no offset is nothing committed");
    }

    @Test
    public void testAGroupStillRebalancingHasNotCaughtUp() {

        for (ConsumerGroupState rebalancing : Arrays.asList(
                ConsumerGroupState.PREPARING_REBALANCE, ConsumerGroupState.COMPLETING_REBALANCE)) {
            describing(group(GROUP, rebalancing, REQUEST_0));
            committing(GROUP, offsets(REQUEST_0, 3L));
            ending(REQUEST_0, 3L);

            assertEquals(Boolean.FALSE, caughtUp(GROUP), "caught up while " + rebalancing);
        }
    }

    @Test
    public void testEveryGroupHasToHaveCaughtUp() {

        describing(
                group(GROUP, ConsumerGroupState.STABLE, REQUEST_0),
                group(OTHER_GROUP, ConsumerGroupState.STABLE, REQUEST_0));
        committing(GROUP, offsets(REQUEST_0, 9L));
        committing(OTHER_GROUP, offsets(REQUEST_0, 8L));
        ending(REQUEST_0, 9L);

        assertEquals(Boolean.FALSE, caughtUp(GROUP, OTHER_GROUP),
                "one group at the end of a partition hid another one behind on the same partition");
        assertEquals(Boolean.TRUE, caughtUp(GROUP));
    }

    @Test
    public void testAGroupThatDoesNotExistCannotBeToldAbout() {

        /*
            Not "caught up": a mistyped group id would otherwise end every wait at once, and
            nothing would say why.
         */
        describing(group(GROUP, ConsumerGroupState.DEAD));

        assertNull(caughtUp(GROUP));
        assertNull(caughtUp(OTHER_GROUP), "a group the broker did not describe at all");
    }

    @Test
    public void testABrokerThatFailsToAnswerCannotBeToldAbout() {

        when(admin.describeConsumerGroups(anyCollection())).thenReturn(new DescribeConsumerGroupsResult(
                Collections.singletonMap(GROUP, failed(new IllegalStateException("coordinator not available")))));

        assertNull(caughtUp(GROUP));
    }

    @Test
    public void testWaitingForTheGroupsEndsOnceTheyCatchUp() {

        describing(group(GROUP, ConsumerGroupState.STABLE, REQUEST_0));
        ending(REQUEST_0, 3L);

        ListConsumerGroupOffsetsResult behind = mock(ListConsumerGroupOffsetsResult.class);
        when(behind.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(offsets(REQUEST_0, 2L)));
        ListConsumerGroupOffsetsResult atTheEnd = mock(ListConsumerGroupOffsetsResult.class);
        when(atTheEnd.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(offsets(REQUEST_0, 3L)));
        when(admin.listConsumerGroupOffsets(GROUP)).thenReturn(behind, behind, atTheEnd);

        assertTrue(helper.awaitConsumerGroupsCaughtUp(Collections.singletonList(GROUP), 10_000));
        verify(admin, times(3)).listConsumerGroupOffsets(GROUP);
    }

    @Test
    public void testWaitingForGroupsThatNeverCatchUpGivesUp() {

        describing(group(GROUP, ConsumerGroupState.STABLE, REQUEST_0));
        committing(GROUP, offsets(REQUEST_0, 2L));
        ending(REQUEST_0, 3L);

        assertFalse(helper.awaitConsumerGroupsCaughtUp(Collections.singletonList(GROUP), SHORT_TIMEOUT_MS));
    }

    @Test
    public void testAskingAboutTheGroupsDoesNotWaitForAReplyBeingAwaited() throws Exception {

        describing(group(GROUP, ConsumerGroupState.DEAD));
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            //what executeAsyncApiAction holds while it awaits a reply
            synchronized (helper) {
                Future<Boolean> answer = other.submit(() -> caughtUp(GROUP));
                assertNull(answer.get(10, TimeUnit.SECONDS));
            }
        } finally {
            other.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- the rest

    @Test
    public void testClosingClosesEveryClientEvenWhenOneFails() {

        helper.executeAsyncApiAction(requestReply(), new AsyncApiReplyDto());
        doThrow(new IllegalStateException("stuck")).when(admin).close(any());
        //the admin client is only created when first needed
        describing(group(GROUP, ConsumerGroupState.DEAD));
        caughtUp(GROUP);

        helper.close();

        assertTrue(consumers.get(0).closed());
        assertTrue(producer.closed());
        verify(admin).close(any());
    }

    @Test
    public void testNoArgumentCanBeNull() {

        assertThrows(NullPointerException.class, () -> helper.executeAsyncApiAction(null, new AsyncApiReplyDto()));
        assertThrows(NullPointerException.class, () -> helper.executeAsyncApiAction(fireAndForget(), null));
        assertThrows(NullPointerException.class, () -> helper.createTopics(null));
        assertThrows(NullPointerException.class, () -> helper.awaitAnswering(null, 1));
        assertThrows(NullPointerException.class, () -> helper.awaitConsumerGroupsAssigned(null, 1));
        assertThrows(NullPointerException.class, () -> helper.consumerGroupsCaughtUp(null));
        assertThrows(NullPointerException.class, () -> helper.awaitConsumerGroupsCaughtUp(null, 1));
        assertThrows(NullPointerException.class, () -> new KafkaAsyncApiDriverHelper(null));
    }
}
