package com.foo.asyncapi.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.controller.problem.AsyncApiProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.kafka.KafkaContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Shared driver for Spring services that speak Kafka, as {@code SpringController} is for REST.
 * The service is given the broker address in the {@value #BOOTSTRAP_PROPERTY} property.
 */
public abstract class KafkaController extends EmbeddedSutController {

    private static final String KAFKA_IMAGE = "apache/kafka:3.8.0";

    protected static final String BOOTSTRAP_PROPERTY = "kafka.bootstrap";

    /**
     * Kafka has no native correlation: the documents of these E2Es declare this header.
     */
    protected static final String CORRELATION_HEADER = "correlationId";

    private static final long DEFAULT_REPLY_TIMEOUT_MS = 5_000;

    private static final long PUBLISH_TIMEOUT_SECONDS = 10;

    private static final long POLL_MS = 100;

    /**
     * How long to wait, at start-up, for the service to answer a probe request.
     */
    private static final long READY_TIMEOUT_MS = 60_000;

    /**
     * How long to wait for one probe to come back before publishing another.
     */
    private static final long READY_POLL_MS = 2_000;

    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5);

    private final Class<?> applicationClass;

    private final String document;

    private final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    /**
     * Keys are reply-topic names. Values are the consumers positioned on them, created on first use.
     */
    private final Map<String, Consumer<String, String>> replyConsumers = new LinkedHashMap<>();

    protected ConfigurableApplicationContext ctx;

    private Producer<String, String> producer;

    /**
     * @param applicationClass the Spring application of the service
     * @param document         the classpath resource of its AsyncAPI document
     */
    protected KafkaController(Class<?> applicationClass, String document) {
        this.applicationClass = applicationClass;
        this.document = document;
        super.setControllerPort(0);
    }

    KafkaController(Class<?> applicationClass, String document, Producer<String, String> producer) {
        this(applicationClass, document);
        this.producer = producer;
    }

    /**
     * Every topic the document names, created up front rather than waiting for auto-creation.
     */
    protected abstract List<String> getTopics();

    /**
     * A request the service answers, published by the default {@link #awaitServiceReady()} until
     * a reply comes back. A service that does not reply overrides that method instead.
     */
    protected ProducerRecord<String, String> getProbeRequest() {
        return null;
    }

    /**
     * The topic the reply to {@link #getProbeRequest()} arrives on.
     */
    protected String getProbeReplyTopic() {
        return null;
    }

    @Override
    public String startSut() {

        try {
            return start();
        } catch (RuntimeException e) {
            //isSutRunning() is false until the end, so nothing else would stop the broker
            stopSut();
            throw e;
        }
    }

    private String start() {

        kafka.start();
        String bootstrap = kafka.getBootstrapServers();

        createTopics(bootstrap);

        ctx = new SpringApplicationBuilder(applicationClass)
                .web(WebApplicationType.NONE)
                .properties(BOOTSTRAP_PROPERTY + "=" + bootstrap)
                .run();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);

        awaitServiceReady();

        //this service has no URL: what there is to know is where its broker listens
        return bootstrap;
    }

    /**
     * Return once the service is consuming: a request sent before then is answered too late, and
     * looks like a missing reply. By default, publish {@link #getProbeRequest()} until it is answered.
     */
    protected void awaitServiceReady() {

        if (getProbeRequest() == null || getProbeReplyTopic() == null) {
            throw new IllegalStateException(
                    "Either override awaitServiceReady(), or provide a probe request and its reply topic");
        }

        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MS;

        try (Consumer<String, String> replies = consumerAt(getProbeReplyTopic())) {

            while (System.currentTimeMillis() < deadline) {

                String probe = "ready-" + System.nanoTime();
                ProducerRecord<String, String> record = getProbeRequest();
                record.headers().add(CORRELATION_HEADER, bytes(probe));
                producer.send(record);

                long roundTrip = System.currentTimeMillis() + READY_POLL_MS;
                while (System.currentTimeMillis() < roundTrip) {
                    for (ConsumerRecord<String, String> reply : replies.poll(Duration.ofMillis(POLL_MS))) {
                        Header correlation = reply.headers().lastHeader(CORRELATION_HEADER);
                        if (correlation != null && correlation.value() != null
                                && probe.equals(new String(correlation.value(), StandardCharsets.UTF_8))) {
                            return;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException(
                "The service did not answer a probe within " + READY_TIMEOUT_MS + " ms");
    }

    private void createTopics(String bootstrap) {

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        try (AdminClient admin = AdminClient.create(props)) {
            List<NewTopic> topics = getTopics().stream()
                    .map(name -> new NewTopic(name, 1, (short) 1))
                    .collect(Collectors.toList());
            admin.createTopics(topics).all().get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException("Could not create the topics", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating the topics", e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Could not create the topics", e);
        }
    }

    @Override
    public void stopSut() {

        //every close has a deadline and may fail, so that a stuck client cannot block the stop
        closeReplyConsumers();

        if (producer != null) {
            quietly(() -> producer.close(CLOSE_TIMEOUT));
            producer = null;
        }
        if (ctx != null) {
            quietly(ctx::close);
            ctx = null;
        }
        quietly(kafka::stop);
    }

    private static void quietly(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            SimpleLogger.warn("Ignored while stopping the SUT: " + e.getMessage());
        }
    }

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public void resetStateOfSUT() {

        //the same seed mints the same correlation ids, so a reply left by a previous test must not be read
        closeReplyConsumers();
    }

    private void closeReplyConsumers() {
        replyConsumers.values().forEach(c -> quietly(() -> c.close(CLOSE_TIMEOUT)));
        replyConsumers.clear();
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    /**
     * The container picks its port at start, so the document's address is never the real one.
     * Null before the container is up.
     */
    @Override
    public String getAsyncApiServerAddress(String serverName) {
        return kafka.isRunning() ? kafka.getBootstrapServers() : null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return AsyncApiProblem.fromSchemaText(readDocument());
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    private String readDocument() {
        try (InputStream in = getClass().getResourceAsStream(document)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + document);
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + document, e);
        }
    }

    @Override
    public void executeAsyncApiAction(AsyncApiActionDto dto, AsyncApiReplyDto reply) {

        correlationHeaderName(dto); //reject unsupported correlation before opening a client

        //positioned before publishing, so that the reply cannot slip past
        Consumer<String, String> replies = dto.replyAddress == null ? null : consumerOn(dto.replyAddress);

        ProducerRecord<String, String> record = new ProducerRecord<>(dto.address, dto.correlationId, dto.payload);
        dto.headers.forEach((name, value) -> record.headers().add(name, bytes(value)));
        if (dto.correlationLocation == AsyncApiActionDto.CorrelationLocation.HEADER || dto.correlationLocation == null) {
            record.headers().add(correlationHeaderName(dto), bytes(dto.correlationId));
        }

        try {
            producer.send(record).get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing to " + dto.address, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException("Could not publish to " + dto.address + ": " + e.getMessage(), e);
        }
        reply.published = true;

        if (replies == null) {
            reply.replyExpected = false;
            return;
        }

        reply.replyExpected = true;
        awaitReply(replies, dto, reply);
    }

    private void awaitReply(Consumer<String, String> replies, AsyncApiActionDto dto, AsyncApiReplyDto reply) {

        long timeout = dto.replyTimeoutMs != null ? dto.replyTimeoutMs : DEFAULT_REPLY_TIMEOUT_MS;
        long start = System.currentTimeMillis();
        long deadline = start + timeout;

        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }

            ConsumerRecords<String, String> polled = replies.poll(Duration.ofMillis(Math.min(POLL_MS, remaining)));

            for (ConsumerRecord<String, String> candidate : polled) {
                String id = correlationOf(candidate, dto);
                if (id != null && !id.equals(dto.correlationId)) {
                    //an answer to some other request
                    continue;
                }
                reply.replyReceived = true;
                reply.replyPayload = candidate.value();
                reply.replyHeaders = headersOf(candidate);
                reply.correlationMatched = id != null;
                reply.waitedMs = System.currentTimeMillis() - start;
                return;
            }
        }

        reply.waitedMs = timeout;
    }

    /** Read the correlation header; absent or null values mean unknown correlation. */
    static String correlationOf(ConsumerRecord<String, String> record, AsyncApiActionDto dto) {
        Header header = record.headers().lastHeader(correlationHeaderName(dto));
        return header == null || header.value() == null
                ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    /** This driver implements only correlation by the {@value #CORRELATION_HEADER} header. */
    static String correlationHeaderName(AsyncApiActionDto dto) {
        if ((dto.correlationLocation != null && dto.correlationLocation != AsyncApiActionDto.CorrelationLocation.HEADER)
                || (dto.correlationPointer != null && !dto.correlationPointer.equals("/" + CORRELATION_HEADER))) {
            throw new IllegalArgumentException("Only the /" + CORRELATION_HEADER + " header location is supported");
        }
        return CORRELATION_HEADER;
    }

    private Consumer<String, String> consumerOn(String address) {

        return replyConsumers.computeIfAbsent(address, this::consumerAt);
    }

    /**
     * A consumer that sees only what is published to {@code address} from now on.
     */
    private Consumer<String, String> consumerAt(String address) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        Consumer<String, String> consumer = new KafkaConsumer<>(props);

        return positionAtEnd(consumer, address);
    }

    /** Owns the consumer on entry and closes it if positioning fails before it can be cached. */
    static Consumer<String, String> positionAtEnd(Consumer<String, String> consumer, String address) {
        try {
            //assigned rather than subscribed: no group, no rebalance to wait for
            List<TopicPartition> partitions = consumer.partitionsFor(address).stream()
                    .map(p -> new TopicPartition(address, p.partition()))
                    .collect(Collectors.toList());
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);
            //seekToEnd is lazy; asking for the position makes it take effect now
            partitions.forEach(consumer::position);
            return consumer;
        } catch (RuntimeException | Error failure) {
            try {
                consumer.close(CLOSE_TIMEOUT);
            } catch (RuntimeException | Error closing) {
                failure.addSuppressed(closing);
            }
            throw failure;
        }
    }

    /**
     * Keys are Kafka header names; values are UTF-8 decoded text, or null for null header values.
     * When a name occurs more than once, the last value wins, matching lastHeader correlation lookup.
     */
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
}
