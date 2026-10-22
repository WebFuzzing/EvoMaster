package org.evomaster.client.java.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * What the driver of an AsyncAPI service that speaks Kafka has to do with Kafka, so that the
 * driver itself only has to start the broker and the service.
 *
 * <ul>
 *     <li>{@link #executeAsyncApiAction}: publishes what the core asks, with its headers and
 *     its correlation id, and awaits the reply that answers it. A driver's own
 *     {@code executeAsyncApiAction} can delegate to it as it is.</li>
 *     <li>{@link #resetReplyConsumers}: for {@code resetStateOfSUT}, so that no reply left
 *     over from a test answers a request of the next one.</li>
 *     <li>{@link #createTopics}, {@link #awaitConsumerGroupsAssigned} and
 *     {@link #awaitAnswering}: for {@code startSut}, so that the search does not start before
 *     the service is listening.</li>
 *     <li>{@link #consumerGroupsCaughtUp} and {@link #awaitConsumerGroupsCaughtUp}: whether the
 *     service's consumer groups have taken in everything published so far, for
 *     {@code areAsyncApiConsumersCaughtUp} and {@code resetStateOfSUT}.</li>
 * </ul>
 *
 * The instrumentation skips this package, so its Kafka calls are never recorded as the
 * service's, even when the driver runs in the same JVM.
 *
 * Calls that use the reply consumers are synchronized: the controller makes them one at a
 * time, but not always from the same thread, and a Kafka consumer must not be used by two
 * threads at once. The admin client has a lock of its own, so that asking about the consumer
 * groups does not wait for a reply being awaited.
 */
public class KafkaAsyncApiDriverHelper implements AutoCloseable {

    /**
     * The header a correlation id travels in when the document puts it in a header without
     * naming one, and the one a probe of {@link #awaitAnswering} uses when the probe gives no
     * location. Kafka has no native correlation, so one has to be picked.
     */
    public static final String DEFAULT_CORRELATION_HEADER = "correlationId";

    /**
     * How long to wait for a reply when the action does not say.
     */
    public static final long DEFAULT_REPLY_TIMEOUT_MS = 5_000;

    /**
     * How long to wait for one probe of {@link #awaitAnswering} to be answered before
     * publishing another.
     */
    public static final long DEFAULT_PROBE_TIMEOUT_MS = 2_000;

    /**
     * How long any single request to the broker may take: a publish, or an admin request.
     */
    private static final long BROKER_TIMEOUT_MS = 10_000;

    /**
     * How long one poll for a reply may block.
     */
    private static final long POLL_MS = 100;

    /**
     * How often to ask the broker again while waiting for a consumer group.
     */
    private static final long GROUP_POLL_MS = 50;

    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5);

    private static final String ACKS_ALL = "all";

    private static final String NO_AUTO_COMMIT = "false";

    private static final String OFFSET_RESET_LATEST = "latest";

    private static final String NO_AUTO_CREATE = "false";

    /**
     * Published when the action has no payload, as the generated tests do.
     */
    private static final String EMPTY_PAYLOAD = "";

    private static final String PROBE_ID_PREFIX = "evomaster-probe-";

    private static final String REPLY_FAILURE_PREFIX = "Published, but could not await the reply on ";

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Producer<String, String> producer;

    /**
     * Creates a consumer with no group and no position, to be assigned to a reply address.
     */
    private final Supplier<Consumer<String, String>> consumerFactory;

    /**
     * Creates the admin client the first time one is needed. Not every driver needs one.
     */
    private final Supplier<Admin> adminFactory;

    /**
     * Reply address -&gt; the consumer assigned every partition of it, created the first time a
     * reply is awaited there, and kept until {@link #resetReplyConsumers()}. Only consumers
     * that have partitions are kept: one created while the address did not exist yet is not,
     * so that the next action looks for the address again.
     */
    private final Map<String, Consumer<String, String>> replyConsumers = new LinkedHashMap<>();

    /**
     * Counts the probes of {@link #awaitAnswering}, so that each has an id of its own.
     */
    private long probes = 0;

    /**
     * Guards {@link #admin}.
     */
    private final Object adminLock = new Object();

    private Admin admin;

    /**
     * @param bootstrapServers where the broker listens, eg what a Testcontainers Kafka
     *                         container reports from {@code getBootstrapServers()}
     */
    public KafkaAsyncApiDriverHelper(String bootstrapServers) {
        this(bootstrapServers, new Properties());
    }

    /**
     * @param bootstrapServers where the broker listens
     * @param clientProperties added to the configuration of every client this creates, eg
     *                         security settings. They cannot change how messages are
     *                         serialized: payloads and keys are always strings.
     */
    public KafkaAsyncApiDriverHelper(String bootstrapServers, Properties clientProperties) {
        this(
                new KafkaProducer<>(producerProperties(bootstrapServers, clientProperties)),
                () -> new KafkaConsumer<>(consumerProperties(bootstrapServers, clientProperties)),
                () -> Admin.create(adminProperties(bootstrapServers, clientProperties))
        );
    }

    /**
     * Only for tests, to run on mock clients.
     */
    KafkaAsyncApiDriverHelper(
            Producer<String, String> producer,
            Supplier<Consumer<String, String>> consumerFactory,
            Supplier<Admin> adminFactory
    ) {
        this.producer = Objects.requireNonNull(producer);
        this.consumerFactory = Objects.requireNonNull(consumerFactory);
        this.adminFactory = Objects.requireNonNull(adminFactory);
    }

    private static Properties producerProperties(String bootstrapServers, Properties clientProperties) {
        Properties props = common(bootstrapServers, clientProperties);
        props.put(ProducerConfig.ACKS_CONFIG, ACKS_ALL);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    //package-private for tests
    static Properties consumerProperties(String bootstrapServers, Properties clientProperties) {
        Properties props = common(bootstrapServers, clientProperties);
        //no group: the consumer is assigned its partitions, so there is no rebalance to wait for
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, NO_AUTO_COMMIT);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET_LATEST);
        //looking for a reply topic must not create it
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, NO_AUTO_CREATE);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }

    private static Properties adminProperties(String bootstrapServers, Properties clientProperties) {
        return common(bootstrapServers, clientProperties);
    }

    private static Properties common(String bootstrapServers, Properties clientProperties) {
        Objects.requireNonNull(bootstrapServers);
        Objects.requireNonNull(clientProperties);
        Properties props = new Properties();
        props.putAll(clientProperties);
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return props;
    }

    /**
     * Publish the message the core asks for, and when a reply is expected, wait for the one
     * that answers it. Fills in {@code reply} as {@code SutController.executeAsyncApiAction}
     * expects, reporting what came back without judging it.
     *
     * <ul>
     *     <li>As in the generated tests, the record has no key, so a service that partitions or
     *     behaves on the key sees the same in both, and no payload is published as an empty
     *     one, not a tombstone. The headers are the action's.</li>
     *     <li>The correlation id goes where the document says. In a header, it is named after
     *     the last token of the pointer, or {@link #DEFAULT_CORRELATION_HEADER} when there is
     *     none. In the payload, it is written at the pointer, provided the payload is a JSON
     *     object, see {@link JsonPointers#write}. When the document declares no correlation
     *     id, none is published, and the first reply is taken, as in the generated tests.</li>
     *     <li>The reply consumer is moved to the end of the reply address before every
     *     publish, so an older reply, eg a late answer to an earlier message, is never taken.
     *     A reply address that does not exist yet is looked for while waiting, and once it
     *     appears read from its start, skipping records timestamped before the publish. That
     *     relies on the service's clock agreeing with this one, or on broker timestamps.</li>
     *     <li>A reply that carries another correlation id answers some other request, and is
     *     skipped. One that carries none is taken, with
     *     {@link AsyncApiReplyDto#correlationMatched} false.</li>
     *     <li>Once the message is published, a failure while waiting for the reply is not
     *     thrown: it is reported as no reply, with {@link AsyncApiReplyDto#errorMessage}
     *     saying why.</li>
     * </ul>
     *
     * @throws IllegalStateException when the message could not be published, which the
     * controller reports as a broken setup rather than as a finding about the service
     * @throws org.apache.kafka.common.KafkaException or another runtime exception of the
     * client, when the reply consumer could not be positioned; nothing is published then
     */
    public synchronized void executeAsyncApiAction(AsyncApiActionDto dto, AsyncApiReplyDto reply) {
        Objects.requireNonNull(dto);
        Objects.requireNonNull(reply);

        //positioned before publishing, so that the reply cannot slip past
        Consumer<String, String> replies = dto.replyAddress == null ? null : replyConsumerAtEnd(dto.replyAddress);

        long publishedAt;
        try {
            publishedAt = publish(dto);
        } catch (RuntimeException e) {
            releaseIfNotKept(dto.replyAddress, replies);
            throw e;
        }
        reply.published = true;

        if (replies == null) {
            reply.replyExpected = false;
            return;
        }

        reply.replyExpected = true;
        long start = nowMs();
        try {
            awaitReply(replies, dto, reply, start, publishedAt);
        } catch (RuntimeException e) {
            //the message did go out, so this is no reply rather than a failure to publish
            reply.replyReceived = false;
            reply.replyPayload = null;
            reply.correlationMatched = null;
            reply.waitedMs = nowMs() - start;
            reply.errorMessage = REPLY_FAILURE_PREFIX + dto.replyAddress + ": " + e.getMessage();
            //not trusted with the next action: closed by releaseIfNotKept below
            replyConsumers.remove(dto.replyAddress);
        } finally {
            releaseIfNotKept(dto.replyAddress, replies);
        }
    }

    /**
     * @return the timestamp the broker gave the record, or this clock's time just before
     * sending when it gave none
     */
    private long publish(AsyncApiActionDto dto) {

        //no key: the correlation id travels in a header or in the payload, never in the key
        ProducerRecord<String, String> record = new ProducerRecord<>(dto.address, null, stamped(dto));
        if (dto.headers != null) {
            dto.headers.forEach((name, value) -> record.headers().add(name, bytes(value)));
        }
        if (dto.correlationLocation == AsyncApiActionDto.CorrelationLocation.HEADER && dto.correlationId != null) {
            record.headers().add(correlationHeaderName(dto), bytes(dto.correlationId));
        }

        long sentAt = System.currentTimeMillis();
        try {
            RecordMetadata metadata = producer.send(record).get(BROKER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return metadata != null && metadata.hasTimestamp() ? metadata.timestamp() : sentAt;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to " + dto.address, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Could not publish to " + dto.address + ": " + e.getMessage(), e);
        }
    }

    private void awaitReply(
            Consumer<String, String> replies,
            AsyncApiActionDto dto,
            AsyncApiReplyDto reply,
            long start,
            long publishedAt
    ) {

        long timeout = dto.replyTimeoutMs != null ? dto.replyTimeoutMs : DEFAULT_REPLY_TIMEOUT_MS;
        long deadline = start + timeout;
        //whether the consumer was assigned after the publish, from the start of the address
        boolean fromStart = false;

        while (true) {
            long remaining = deadline - nowMs();
            if (remaining <= 0 || Thread.currentThread().isInterrupted()) {
                break;
            }
            long wait = Math.min(POLL_MS, remaining);

            if (replies.assignment().isEmpty()) {
                //the address did not exist at the publish: read from its start, the reply may be there already
                if (!assignAll(replies, dto.replyAddress, true, Duration.ofMillis(remaining))) {
                    sleep(Math.min(wait, Math.max(0, deadline - nowMs())));
                    continue;
                }
                replyConsumers.put(dto.replyAddress, replies);
                fromStart = true;
            }

            for (ConsumerRecord<String, String> candidate : replies.poll(Duration.ofMillis(wait))) {
                if (fromStart && olderThan(candidate, publishedAt)) {
                    //on the address before the publish, eg an answer to an earlier request
                    continue;
                }
                String id = correlationOf(candidate, dto);
                if (id != null && !id.equals(dto.correlationId)) {
                    //an answer to some other request
                    continue;
                }
                reply.replyReceived = true;
                reply.replyPayload = candidate.value();
                reply.replyHeaders = headersOf(candidate);
                reply.correlationMatched = id != null;
                reply.waitedMs = nowMs() - start;
                return;
            }
        }

        reply.replyReceived = false;
        //an interrupt ends the wait early
        reply.waitedMs = Thread.currentThread().isInterrupted() ? nowMs() - start : timeout;
    }

    private static boolean olderThan(ConsumerRecord<String, String> record, long timestamp) {
        return record.timestamp() != RecordBatch.NO_TIMESTAMP && record.timestamp() < timestamp;
    }

    /**
     * The payload with the correlation id written into it, when that is where the document
     * says it goes; otherwise the payload as it is, and an empty one for none.
     */
    private static String stamped(AsyncApiActionDto dto) {
        if (dto.payload == null) {
            return EMPTY_PAYLOAD;
        }
        if (dto.correlationLocation != AsyncApiActionDto.CorrelationLocation.PAYLOAD || dto.correlationId == null) {
            return dto.payload;
        }
        return JsonPointers.write(dto.payload, dto.correlationPointer, dto.correlationId);
    }

    /**
     * The correlation id a reply carries, read from wherever the request's was written, or null
     * when it carries none, or the document declares none.
     */
    private static String correlationOf(ConsumerRecord<String, String> record, AsyncApiActionDto dto) {
        if (dto.correlationLocation == null) {
            return null;
        }
        if (dto.correlationLocation == AsyncApiActionDto.CorrelationLocation.PAYLOAD) {
            return JsonPointers.read(record.value(), dto.correlationPointer);
        }
        Header header = record.headers().lastHeader(correlationHeaderName(dto));
        return header == null || header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    /**
     * What the core names the correlation header, see {@link JsonPointers#lastToken}.
     */
    private static String correlationHeaderName(AsyncApiActionDto dto) {
        String name = JsonPointers.lastToken(dto.correlationPointer);
        return name == null || name.trim().isEmpty() ? DEFAULT_CORRELATION_HEADER : name;
    }

    /**
     * The consumer kept for {@code address}, moved to the end of every partition of it, so that
     * it sees only what is published from now on. When none is kept, a new one: kept when the
     * address has partitions, and otherwise returned unassigned, for {@link #awaitReply} to
     * assign once the address appears.
     */
    private Consumer<String, String> replyConsumerAtEnd(String address) {

        Consumer<String, String> kept = replyConsumers.get(address);
        if (kept != null) {
            try {
                Set<TopicPartition> partitions = kept.assignment();
                kept.seekToEnd(partitions);
                //seekToEnd is lazy; asking for the positions makes it take effect now, before the publish
                partitions.forEach(kept::position);
                return kept;
            } catch (RuntimeException e) {
                dropReplyConsumer(address);
                throw e;
            }
        }

        Consumer<String, String> consumer = consumerFactory.get();
        try {
            if (assignAll(consumer, address, false, null)) {
                replyConsumers.put(address, consumer);
            }
            return consumer;
        } catch (RuntimeException e) {
            closeQuietly(() -> consumer.close(CLOSE_TIMEOUT));
            throw e;
        }
    }

    /**
     * Assign {@code consumer} every partition of {@code address}, positioned at the start of
     * each when {@code fromStart}, at the end otherwise.
     *
     * @param lookFor how long finding the partitions may take; null for the client's default.
     *                Running out of it counts as no partitions
     * @return false, leaving the consumer as it was, when the address has no partitions, eg as
     * it does not exist yet
     */
    private static boolean assignAll(
            Consumer<String, String> consumer,
            String address,
            boolean fromStart,
            Duration lookFor
    ) {

        List<TopicPartition> partitions = new ArrayList<>();
        List<PartitionInfo> infos;
        if (lookFor == null) {
            infos = consumer.partitionsFor(address);
        } else {
            try {
                infos = consumer.partitionsFor(address, lookFor);
            } catch (org.apache.kafka.common.errors.TimeoutException e) {
                return false;
            }
        }
        if (infos != null) {
            for (PartitionInfo info : infos) {
                partitions.add(new TopicPartition(address, info.partition()));
            }
        }
        if (partitions.isEmpty()) {
            return false;
        }

        consumer.assign(partitions);
        if (fromStart) {
            consumer.seekToBeginning(partitions);
        } else {
            consumer.seekToEnd(partitions);
        }
        //seeking is lazy; asking for the positions makes it take effect now
        partitions.forEach(consumer::position);
        return true;
    }

    /**
     * Close {@code consumer} unless it is the one kept for {@code address}.
     */
    private void releaseIfNotKept(String address, Consumer<String, String> consumer) {
        if (consumer != null && replyConsumers.get(address) != consumer) {
            closeQuietly(() -> consumer.close(CLOSE_TIMEOUT));
        }
    }

    /**
     * Close and forget the consumer kept for {@code address}, if any.
     */
    private void dropReplyConsumer(String address) {
        Consumer<String, String> kept = replyConsumers.remove(address);
        if (kept != null) {
            closeQuietly(() -> kept.close(CLOSE_TIMEOUT));
        }
    }

    /**
     * Close every reply consumer. The next reply awaited on an address gets a new one,
     * positioned at the end of it.
     *
     * Meant for {@code resetStateOfSUT}: a search restarted under the same seed mints the same
     * correlation ids again, so a reply left on a topic by a previous test could otherwise
     * answer a request of this one.
     */
    public synchronized void resetReplyConsumers() {
        for (Consumer<String, String> consumer : replyConsumers.values()) {
            closeQuietly(() -> consumer.close(CLOSE_TIMEOUT));
        }
        replyConsumers.clear();
    }

    /**
     * Create the topics, each with one partition and one replica, so that no first message has
     * to wait for auto-creation, or is lost to a broker that does not auto-create.
     *
     * @see #createTopics(Collection, int, short)
     */
    public void createTopics(Collection<String> topics) {
        createTopics(topics, 1, (short) 1);
    }

    /**
     * Create the topics with the given number of partitions and replicas. A topic that exists
     * already is left as it is.
     *
     * @throws IllegalStateException when any other topic could not be created
     */
    public void createTopics(Collection<String> topics, int partitions, short replicationFactor) {
        Objects.requireNonNull(topics);
        if (topics.isEmpty()) {
            return;
        }

        List<NewTopic> newTopics = new ArrayList<>();
        for (String topic : new LinkedHashSet<>(topics)) {
            newTopics.add(new NewTopic(Objects.requireNonNull(topic), partitions, replicationFactor));
        }

        CreateTopicsResult result = admin().createTopics(newTopics);
        for (Map.Entry<String, KafkaFuture<Void>> created : result.values().entrySet()) {
            try {
                get(created.getValue());
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof TopicExistsException)) {
                    throw new IllegalStateException("Could not create topic " + created.getKey(), e);
                }
            } catch (TimeoutException e) {
                throw new IllegalStateException("Could not create topic " + created.getKey(), e);
            }
        }
    }

    /**
     * Return once the service answers {@code probe}, publishing it again every
     * {@link #DEFAULT_PROBE_TIMEOUT_MS} until it does.
     *
     * Started is not listening: joining a consumer group takes a moment, and a request published
     * before that is answered after its sender has stopped waiting, which looks like a service
     * that does not reply. Each probe has a correlation id of its own, and only a reply carrying
     * it counts.
     *
     * @param probe     a message the service answers, with a reply address. Its correlation id
     *                  and reply timeout are ignored. With no correlation location, the id goes
     *                  in the {@link #DEFAULT_CORRELATION_HEADER} header
     * @param timeoutMs how long to keep trying
     * @throws IllegalArgumentException when the probe expects no reply
     * @throws IllegalStateException    when the service does not answer in time
     */
    public synchronized void awaitAnswering(AsyncApiActionDto probe, long timeoutMs) {
        Objects.requireNonNull(probe);
        if (probe.replyAddress == null) {
            throw new IllegalArgumentException("A probe needs a reply address, or there is nothing to await");
        }

        long deadline = nowMs() + timeoutMs;
        while (nowMs() < deadline) {
            AsyncApiActionDto attempt = copyOf(probe);
            attempt.correlationId = PROBE_ID_PREFIX + (++probes);
            if (attempt.correlationLocation == null) {
                //a probe needs an id to be matched on
                attempt.correlationLocation = AsyncApiActionDto.CorrelationLocation.HEADER;
            }
            attempt.replyTimeoutMs = Math.max(1, Math.min(DEFAULT_PROBE_TIMEOUT_MS, deadline - nowMs()));

            AsyncApiReplyDto answer = new AsyncApiReplyDto();
            executeAsyncApiAction(attempt, answer);
            if (Boolean.TRUE.equals(answer.replyReceived) && Boolean.TRUE.equals(answer.correlationMatched)) {
                return;
            }
        }

        throw new IllegalStateException("The service did not answer a probe on " + probe.address
                + " within " + timeoutMs + " ms");
    }

    private static AsyncApiActionDto copyOf(AsyncApiActionDto dto) {
        AsyncApiActionDto copy = new AsyncApiActionDto();
        copy.operationId = dto.operationId;
        copy.channelName = dto.channelName;
        copy.address = dto.address;
        copy.messageId = dto.messageId;
        copy.payload = dto.payload;
        copy.contentType = dto.contentType;
        copy.headers = dto.headers == null ? null : new LinkedHashMap<>(dto.headers);
        copy.correlationLocation = dto.correlationLocation;
        copy.correlationPointer = dto.correlationPointer;
        copy.replyAddress = dto.replyAddress;
        return copy;
    }

    /**
     * Return once every one of the consumer groups is stable and has partitions assigned to
     * its members, ie it has joined, and is reading.
     *
     * @throws IllegalStateException when that does not happen in time
     */
    public void awaitConsumerGroupsAssigned(Collection<String> groupIds, long timeoutMs) {
        Objects.requireNonNull(groupIds);

        long deadline = nowMs() + timeoutMs;
        while (true) {
            if (allAssigned(groupIds)) {
                return;
            }
            if (nowMs() >= deadline) {
                throw new IllegalStateException("The consumer groups " + groupIds
                        + " did not get their partitions within " + timeoutMs + " ms");
            }
            sleep(GROUP_POLL_MS);
        }
    }

    private boolean allAssigned(Collection<String> groupIds) {
        Map<String, ConsumerGroupDescription> groups;
        try {
            groups = describe(groupIds);
        } catch (ExecutionException | TimeoutException e) {
            //eg the group coordinator not elected yet: asked again
            return false;
        }
        for (String groupId : groupIds) {
            ConsumerGroupDescription group = groups.get(groupId);
            if (group == null || group.state() != ConsumerGroupState.STABLE || assignedTo(group).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether every one of the consumer groups has committed, on every partition it reads, the
     * offset at the end of that partition, ie has taken in everything published so far.
     *
     * A group reads what its members are assigned plus whatever it has committed on; an assigned
     * partition with no commit counts as read from the start. A group is not caught up while
     * rebalancing, nor while it has no members and has never committed. One with no members that
     * did commit, eg whose consumer stopped once done, is judged on its offsets.
     *
     * Limitations:
     * <ul>
     *     <li>A committed offset says only what the consumer told the broker. One that commits
     *     before it is done, hands off to another thread, or auto-commits on a timer looks caught
     *     up earlier or later than it is. One that commits once it has handled what it polled, as
     *     Spring Kafka does by default, is told about correctly.</li>
     *     <li>A consumer with {@code auto.offset.reset=latest} never reads what was there before it
     *     joined, so a partition it has not committed on yet counts as behind, and every wait on
     *     it lasts its whole window. Read from the earliest offset, on topics created empty, or
     *     commit on joining.</li>
     *     <li>A stable group assigned nothing that has committed nothing is caught up, eg one
     *     subscribed to a topic that does not exist yet, until its consumers see the topic, which
     *     can take minutes. Create the topics before the service starts, see
     *     {@link #createTopics}.</li>
     * </ul>
     *
     * @return true when caught up, false when not yet, null when it cannot be told: a group that
     * does not exist or is dead, or a broker that does not answer
     */
    public Boolean consumerGroupsCaughtUp(Collection<String> groupIds) {
        Objects.requireNonNull(groupIds);

        try {
            Map<String, ConsumerGroupDescription> groups = describe(groupIds);

            //how far behind is decided per group, so the same partition read by two groups is two entries
            List<Map<TopicPartition, Long>> perGroup = new ArrayList<>();

            for (String groupId : groupIds) {
                ConsumerGroupDescription group = groups.get(groupId);
                if (group == null || group.state() == ConsumerGroupState.DEAD) {
                    return null;
                }
                if (isRebalancing(group.state())) {
                    return false;
                }
                //partition -> the offset the group is at there
                Map<TopicPartition, Long> at = new LinkedHashMap<>();
                for (TopicPartition partition : assignedTo(group)) {
                    at.put(partition, 0L);
                }
                Map<TopicPartition, OffsetAndMetadata> committed =
                        get(admin().listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
                for (Map.Entry<TopicPartition, OffsetAndMetadata> offset : committed.entrySet()) {
                    //null for a partition the group has no offset for
                    at.put(offset.getKey(), offset.getValue() == null ? 0L : offset.getValue().offset());
                }
                if (group.state() != ConsumerGroupState.STABLE && !anyCommitted(committed)) {
                    //no members, or a state the client does not know, and nothing read yet
                    return false;
                }
                perGroup.add(at);
            }

            Map<TopicPartition, OffsetSpec> latest = new LinkedHashMap<>();
            for (Map<TopicPartition, Long> group : perGroup) {
                for (TopicPartition partition : group.keySet()) {
                    latest.put(partition, OffsetSpec.latest());
                }
            }
            if (latest.isEmpty()) {
                return true;
            }
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends = get(admin().listOffsets(latest).all());

            return caughtUp(perGroup, ends);

        } catch (ExecutionException | TimeoutException e) {
            return null;
        }
    }

    private static boolean anyCommitted(Map<TopicPartition, OffsetAndMetadata> committed) {
        for (OffsetAndMetadata offset : committed.values()) {
            if (offset != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRebalancing(ConsumerGroupState state) {
        return state == ConsumerGroupState.PREPARING_REBALANCE || state == ConsumerGroupState.COMPLETING_REBALANCE;
    }

    private static boolean caughtUp(
            List<Map<TopicPartition, Long>> perGroup,
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends
    ) {
        for (Map<TopicPartition, Long> group : perGroup) {
            for (Map.Entry<TopicPartition, Long> at : group.entrySet()) {
                ListOffsetsResult.ListOffsetsResultInfo end = ends.get(at.getKey());
                if (end != null && at.getValue() < end.offset()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Wait until {@link #consumerGroupsCaughtUp} says the groups have caught up, at most
     * {@code timeoutMs}. Meant for {@code resetStateOfSUT}, so that a test does not start while
     * the service is still handling what the previous one published.
     *
     * @return whether they caught up in time. False as well when it could not be told
     */
    public boolean awaitConsumerGroupsCaughtUp(Collection<String> groupIds, long timeoutMs) {
        Objects.requireNonNull(groupIds);

        long deadline = nowMs() + timeoutMs;
        while (true) {
            if (Boolean.TRUE.equals(consumerGroupsCaughtUp(groupIds))) {
                return true;
            }
            if (nowMs() >= deadline || Thread.currentThread().isInterrupted()) {
                return false;
            }
            sleep(GROUP_POLL_MS);
        }
    }

    private Map<String, ConsumerGroupDescription> describe(Collection<String> groupIds)
            throws ExecutionException, TimeoutException {
        if (groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return get(admin().describeConsumerGroups(groupIds).all());
    }

    private static Set<TopicPartition> assignedTo(ConsumerGroupDescription group) {
        Set<TopicPartition> partitions = new LinkedHashSet<>();
        for (MemberDescription member : group.members()) {
            if (member.assignment() != null) {
                partitions.addAll(member.assignment().topicPartitions());
            }
        }
        return partitions;
    }

    private Admin admin() {
        synchronized (adminLock) {
            if (admin == null) {
                admin = adminFactory.get();
            }
            return admin;
        }
    }

    /**
     * Close the reply consumers, the producer and the admin client. Every close is given a
     * deadline and may fail without stopping the others: a client that will not shut down
     * must not keep the driver from stopping the service.
     */
    @Override
    public synchronized void close() {
        resetReplyConsumers();
        closeQuietly(() -> producer.close(CLOSE_TIMEOUT));
        synchronized (adminLock) {
            if (admin != null) {
                Admin toClose = admin;
                closeQuietly(() -> toClose.close(CLOSE_TIMEOUT));
                admin = null;
            }
        }
    }

    private static void closeQuietly(Runnable close) {
        try {
            close.run();
        } catch (RuntimeException e) {
            //nothing more to do about a client that cannot be closed, and the others still have to be
        }
    }

    /**
     * Wait for a request to the broker, at most {@link #BROKER_TIMEOUT_MS}. An interrupt is
     * kept on the thread, and reported as a timeout.
     */
    private static <T> T get(KafkaFuture<T> future) throws ExecutionException, TimeoutException {
        try {
            return future.get(BROKER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Interrupted while waiting for the broker");
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Map<String, String> headersOf(ConsumerRecord<String, String> record) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8));
        }
        return headers;
    }

    private static byte[] bytes(String value) {
        return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
    }

    private static long nowMs() {
        return System.nanoTime() / NANOS_PER_MILLI;
    }
}
