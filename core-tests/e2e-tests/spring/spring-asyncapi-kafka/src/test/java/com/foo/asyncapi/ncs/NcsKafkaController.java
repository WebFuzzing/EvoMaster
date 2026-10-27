package com.foo.asyncapi.ncs;

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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * The driver for NCS over Kafka, and the reference implementation of
 * {@link #executeAsyncApiAction}: it owns the broker, publishes what the core asks it to, waits
 * for the reply that answers it, and reports what came back without judging it.
 */
public class NcsKafkaController extends EmbeddedSutController {

    private static final String KAFKA_IMAGE = "apache/kafka:3.8.0";

    private static final String DOCUMENT = "/asyncapi/ncs-kafka.yaml";

    private static final String BOOTSTRAP_PROPERTY = "ncs.kafka.bootstrap";

    /**
     * The header a correlation id travels in when the document names no location. Kafka has
     * no native correlation, so the driver has to pick one.
     */
    private static final String DEFAULT_CORRELATION_HEADER = "correlationId";

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

    /**
     * A triangle request that is valid, so the service answers with a result rather than an error.
     */
    private static final String PROBE_REQUEST = "{\"a\":3,\"b\":4,\"c\":5}";

    /**
     * Every topic the document names, created up front so that no first request or reply has
     * to wait for auto-creation.
     */
    private static final List<String> TOPICS = Arrays.asList(
            "ncs.triangle.request", "ncs.triangle.reply",
            "ncs.bessj.request", "ncs.bessj.reply",
            "ncs.expint.request", "ncs.expint.reply",
            "ncs.fisher.request", "ncs.fisher.reply",
            "ncs.gammq.request", "ncs.gammq.reply",
            "ncs.remainder.request", "ncs.remainder.reply");

    private final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    /**
     * Keys are literal Kafka reply-topic names (for example, ncs.triangle.reply). Values are
     * the consumers positioned on those topics, created the first time a reply is
     * awaited there and kept for the rest of the run.
     */
    private final Map<String, Consumer<String, String>> replyConsumers = new LinkedHashMap<>();

    private ConfigurableApplicationContext ctx;

    private Producer<String, String> producer;

    public NcsKafkaController() {
        super.setControllerPort(0);
    }

    NcsKafkaController(Producer<String, String> producer) {
        this();
        this.producer = producer;
    }

    /** Enable or suppress replies in the fixture, after its readiness probe has completed. */
    public void setRepliesEnabled(boolean enabled) {
        ctx.getBean(NcsRequestConsumer.class).setRepliesEnabled(enabled);
    }

    @Override
    public String startSut() {

        try {
            return start();
        } catch (RuntimeException e) {
            /*
                Nothing else will: isSutRunning() answers on the Spring context, which is not
                assigned until the end, so a failure here would leave the broker running for the
                rest of the fork.
             */
            stopSut();
            throw e;
        }
    }

    private String start() {

        kafka.start();
        String bootstrap = kafka.getBootstrapServers();

        createTopics(bootstrap);

        ctx = new SpringApplicationBuilder(NcsKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .properties(BOOTSTRAP_PROPERTY + "=" + bootstrap)
                .run();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);

        awaitServiceConsuming();

        //this service has no URL: what there is to know is where its broker listens
        return bootstrap;
    }

    /**
     * Return only once the service is answering, rather than as soon as it has been started.
     *
     * Joining a consumer group takes a moment, and a request published before that happens is
     * answered late, after whoever sent it has stopped waiting. For a search, that would look
     * like a service that does not reply, and the first dozen messages would each cost the
     * whole reply timeout.
     */
    private void awaitServiceConsuming() {

        long deadline = System.currentTimeMillis() + READY_TIMEOUT_MS;

        try (Consumer<String, String> replies = consumerAt(NcsService.TRIANGLE_REPLY)) {

            while (System.currentTimeMillis() < deadline) {

                String probe = "ready-" + System.nanoTime();
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(NcsService.TRIANGLE_REQUEST, probe, PROBE_REQUEST);
                record.headers().add(DEFAULT_CORRELATION_HEADER, bytes(probe));
                producer.send(record);

                long roundTrip = System.currentTimeMillis() + READY_POLL_MS;
                while (System.currentTimeMillis() < roundTrip) {
                    for (ConsumerRecord<String, String> reply : replies.poll(Duration.ofMillis(POLL_MS))) {
                        Header correlation = reply.headers().lastHeader(DEFAULT_CORRELATION_HEADER);
                        if (correlation != null && correlation.value() != null
                                && probe.equals(new String(correlation.value(), StandardCharsets.UTF_8))) {
                            return;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException(
                "The NCS service did not answer a probe within " + READY_TIMEOUT_MS + " ms");
    }

    private void createTopics(String bootstrap) {

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        try (AdminClient admin = AdminClient.create(props)) {
            List<NewTopic> topics = TOPICS.stream()
                    .map(name -> new NewTopic(name, 1, (short) 1))
                    .collect(Collectors.toList());
            admin.createTopics(topics).all().get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException("Could not create the NCS topics", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating the NCS topics", e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Could not create the NCS topics", e);
        }
    }

    @Override
    public void stopSut() {

        /*
            Every close is given a deadline and is allowed to fail: a Kafka client that will not
            shut down must not keep the whole controller from stopping, which the E2E asserts.
         */
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
    public String getPackagePrefixesToCover() {
        return "com.foo.asyncapi.ncs.";
    }

    @Override
    public void resetStateOfSUT() {

        /*
            The service itself is stateless: every request is answered from its own content.
            What does carry over is where the reply consumers are positioned, and that matters:
            a search restarted under the same seed mints the same correlation ids again, so a
            reply left on a topic by the previous test would answer a request of this one. They
            are dropped here, and the next one to be needed is created at the end of its topic.
         */
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
     * The container picks a port when it starts, so the address in the document is never where
     * this broker actually is. A generated test asks for this instead of trusting the document.
     *
     * Asking before the container is up is answered with null, as the contract says to, rather
     * than with the exception testcontainers raises for a port that has not been mapped yet.
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
        try (InputStream in = getClass().getResourceAsStream(DOCUMENT)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + DOCUMENT);
            }
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + DOCUMENT, e);
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

    /** Read this fixture's correlation header; absent or null values mean unknown correlation. */
    static String correlationOf(ConsumerRecord<String, String> record, AsyncApiActionDto dto) {
        Header header = record.headers().lastHeader(correlationHeaderName(dto));
        return header == null || header.value() == null
                ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    /** This fixture implements only the correlation header declared in ncs-kafka.yaml. */
    static String correlationHeaderName(AsyncApiActionDto dto) {
        if ((dto.correlationLocation != null && dto.correlationLocation != AsyncApiActionDto.CorrelationLocation.HEADER)
                || (dto.correlationPointer != null && !dto.correlationPointer.equals("/" + DEFAULT_CORRELATION_HEADER))) {
            throw new IllegalArgumentException("NCS supports only the /correlationId header location");
        }
        return DEFAULT_CORRELATION_HEADER;
    }

    private Consumer<String, String> consumerOn(String address) {

        return replyConsumers.computeIfAbsent(address, this::consumerAt);
    }

    /**
     * A consumer positioned at the end of {@code address}, so that it sees only what is published
     * from now on.
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
