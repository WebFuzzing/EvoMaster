package com.foo.asyncapi.ncs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
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

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Reply address -&gt; the consumer positioned on it, created the first time a reply is
     * awaited there and kept for the rest of the run.
     */
    private final Map<String, KafkaConsumer<String, String>> replyConsumers = new LinkedHashMap<>();

    private ConfigurableApplicationContext ctx;

    private KafkaProducer<String, String> producer;

    public NcsKafkaController() {
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {

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

        //this service has no URL: what there is to know is where its broker listens
        return bootstrap;
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
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException("Could not create the NCS topics", e);
        }
    }

    @Override
    public void stopSut() {

        replyConsumers.values().forEach(KafkaConsumer::close);
        replyConsumers.clear();

        if (producer != null) {
            producer.close();
            producer = null;
        }
        if (ctx != null) {
            ctx.close();
            ctx = null;
        }
        kafka.stop();
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
        //stateless: every request is answered from its own content
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
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

        //positioned before publishing, so that the reply cannot slip past
        KafkaConsumer<String, String> replies = dto.replyAddress == null ? null : consumerOn(dto.replyAddress);

        ProducerRecord<String, String> record = new ProducerRecord<>(dto.address, dto.correlationId, stamped(dto));
        dto.headers.forEach((name, value) -> record.headers().add(name, bytes(value)));
        if (AsyncApiActionDto.CORRELATION_IN_HEADER.equals(dto.correlationLocation) || dto.correlationLocation == null) {
            record.headers().add(correlationHeaderName(dto), bytes(dto.correlationId));
        }

        try {
            producer.send(record).get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
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

    private void awaitReply(KafkaConsumer<String, String> replies, AsyncApiActionDto dto, AsyncApiReplyDto reply) {

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

    /**
     * The payload with the correlation id written into it, when that is where the document
     * says it goes; otherwise the payload as it is.
     */
    private String stamped(AsyncApiActionDto dto) {

        if (!AsyncApiActionDto.CORRELATION_IN_PAYLOAD.equals(dto.correlationLocation)
                || dto.payload == null || dto.correlationPointer == null) {
            return dto.payload;
        }

        try {
            JsonNode root = mapper.readTree(dto.payload);
            if (!root.isObject()) {
                return dto.payload;
            }
            List<String> segments = segmentsOf(dto.correlationPointer);
            ObjectNode holder = (ObjectNode) root;
            for (int i = 0; i < segments.size() - 1; i++) {
                JsonNode next = holder.get(segments.get(i));
                holder = next != null && next.isObject() ? (ObjectNode) next : holder.putObject(segments.get(i));
            }
            holder.put(segments.get(segments.size() - 1), dto.correlationId);
            return root.toString();
        } catch (IOException e) {
            return dto.payload;
        }
    }

    /**
     * The correlation id a reply carries, read from wherever the request's was written, or null
     * when it carries none.
     */
    private String correlationOf(ConsumerRecord<String, String> record, AsyncApiActionDto dto) {

        if (AsyncApiActionDto.CORRELATION_IN_PAYLOAD.equals(dto.correlationLocation)) {
            try {
                JsonNode node = record.value() == null ? null : mapper.readTree(record.value());
                for (String segment : segmentsOf(dto.correlationPointer)) {
                    node = node == null ? null : node.get(segment);
                }
                return node == null || !node.isValueNode() ? null : node.asText();
            } catch (IOException e) {
                return null;
            }
        }

        Header header = record.headers().lastHeader(correlationHeaderName(dto));
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static String correlationHeaderName(AsyncApiActionDto dto) {
        List<String> segments = segmentsOf(dto.correlationPointer);
        return segments.isEmpty() ? DEFAULT_CORRELATION_HEADER : segments.get(segments.size() - 1);
    }

    private static List<String> segmentsOf(String pointer) {
        if (pointer == null) {
            return Arrays.asList();
        }
        return Arrays.stream(pointer.split("/"))
                .filter(s -> !s.isEmpty())
                .map(s -> s.replace("~1", "/").replace("~0", "~"))
                .collect(Collectors.toList());
    }

    private KafkaConsumer<String, String> consumerOn(String address) {

        return replyConsumers.computeIfAbsent(address, a -> {

            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

            //assigned rather than subscribed: no group, no rebalance to wait for
            List<TopicPartition> partitions = consumer.partitionsFor(a).stream()
                    .map(p -> new TopicPartition(a, p.partition()))
                    .collect(Collectors.toList());
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);
            //seekToEnd is lazy; asking for the position makes it take effect now
            partitions.forEach(consumer::position);

            return consumer;
        });
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
}
