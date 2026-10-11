package com.foo.asyncapi;

import com.foo.asyncapi.shop.ShopKafkaApplication;
import com.foo.asyncapi.shop.ShopTopics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.controller.problem.AsyncApiProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.utils.SimpleLogger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.testcontainers.kafka.KafkaContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * The driver for the shop service, whose operations are all fire-and-forget: it owns the broker,
 * publishes what the core asks it to, and returns as soon as the broker has the message. Nothing
 * ever comes back, so what the service did with it is only known from its coverage.
 *
 * Outside {@code com.foo.asyncapi.shop}, the package whose code is covered, so that the driver's
 * own lines are neither targets of the search nor activity that a wait for the service's
 * coverage to settle would mistake for the service's.
 */
public class ShopKafkaController extends EmbeddedSutController {

    private static final String KAFKA_IMAGE = "apache/kafka:3.8.0";

    private static final String DOCUMENT = "/asyncapi/shop-kafka.yaml";

    private static final String BOOTSTRAP_PROPERTY = "spring.kafka.bootstrap-servers";

    private static final String DELETE_STOCK = "DELETE FROM stock";

    /**
     * The bean name of the executor the service renders invoices on.
     */
    private static final String INVOICE_EXECUTOR = "invoiceExecutor";

    private static final long PUBLISH_TIMEOUT_SECONDS = 10;

    /**
     * How long to wait, at start-up, for every listener to be given its topic.
     */
    private static final long ASSIGNMENT_TIMEOUT_MS = 60_000;

    /**
     * How long to wait, between two tests, for the service to have consumed everything published
     * so far, and then for the invoices it was asked for to be rendered. Far more than it ever
     * needs; reaching it is reported, not fatal.
     */
    private static final long CONSUMED_TIMEOUT_MS = 10_000;

    /**
     * How often the two waits above check again.
     */
    private static final long CHECK_EVERY_MS = 20;

    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5);

    private final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    private ConfigurableApplicationContext ctx;

    private KafkaProducer<String, String> producer;

    private AdminClient admin;

    public ShopKafkaController() {
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {

        try {
            return start();
        } catch (RuntimeException e) {
            /*
                Nothing else will: isSutRunning() answers on the Spring context, which is not
                assigned until the service is up, so a failure here would leave the broker running
                for the rest of the fork.
             */
            stopSut();
            throw e;
        }
    }

    private String start() {

        kafka.start();
        String bootstrap = kafka.getBootstrapServers();

        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        admin = AdminClient.create(adminProps);

        createTopics();

        ctx = new SpringApplicationBuilder(ShopKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .properties(BOOTSTRAP_PROPERTY + "=" + bootstrap)
                .run();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);

        awaitListenersAssigned();

        //this service has no URL: what there is to know is where its broker listens
        return bootstrap;
    }

    private void createTopics() {

        List<NewTopic> topics = ShopTopics.ALL.stream()
                .map(name -> new NewTopic(name, 1, (short) 1))
                .collect(Collectors.toList());
        try {
            admin.createTopics(topics).all().get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException("Could not create the shop topics", e);
            }
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException("Could not create the shop topics", e);
        }
    }

    /**
     * Return once every listener has been given its topic. No message would be lost before that,
     * as the topics are new and read from the start, but one handled only after its listener
     * joined, which can take seconds, would have its coverage credited to a later message, or to
     * none.
     */
    private void awaitListenersAssigned() {

        boolean assigned = await(ASSIGNMENT_TIMEOUT_MS, () -> listeners().stream().allMatch(c -> {
            Collection<TopicPartition> partitions = c.getAssignedPartitions();
            return partitions != null && !partitions.isEmpty();
        }));

        if (!assigned) {
            throw new IllegalStateException(
                    "The shop listeners were not assigned their topics within " + ASSIGNMENT_TIMEOUT_MS + " ms");
        }
    }

    private Collection<MessageListenerContainer> listeners() {
        return ctx.getBean(KafkaListenerEndpointRegistry.class).getListenerContainers();
    }

    @Override
    public void stopSut() {

        /*
            Every close is given a deadline and is allowed to fail: a Kafka client that will not
            shut down must not keep the whole controller from stopping.
         */
        if (producer != null) {
            quietly(() -> producer.close(CLOSE_TIMEOUT));
            producer = null;
        }
        if (admin != null) {
            quietly(() -> admin.close(CLOSE_TIMEOUT));
            admin = null;
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
        return "com.foo.asyncapi.shop.";
    }

    @Override
    public void resetStateOfSUT() {

        /*
            The stock is the service's only state. Before emptying it, wait for everything
            published to be consumed, or a message of the previous test could write to the stock
            after the reset; then for that test's invoices, or they would be covered in this one.
         */
        if (!await(CONSUMED_TIMEOUT_MS, this::everythingConsumed)) {
            SimpleLogger.warn("The shop service had not consumed everything published within "
                    + CONSUMED_TIMEOUT_MS + " ms");
        }
        awaitInvoicesRendered();

        ctx.getBean(JdbcTemplate.class).update(DELETE_STOCK);
    }

    /**
     * Wait until the invoice executor has run everything handed to it so far. It has one thread
     * and runs tasks in order, so an empty task finishing means every earlier one has.
     */
    private void awaitInvoicesRendered() {

        try {
            Future<?> done = ctx.getBean(INVOICE_EXECUTOR, ExecutorService.class).submit(() -> {
            });
            done.get(CONSUMED_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException | RejectedExecutionException e) {
            SimpleLogger.warn("The shop service had not rendered every invoice within "
                    + CONSUMED_TIMEOUT_MS + " ms: " + e.getMessage());
        }
    }

    /**
     * Whether every listener's group has committed up to the end of the partitions it was
     * assigned, i.e., whether every message published so far has been handled.
     */
    private boolean everythingConsumed() {

        try {
            for (MessageListenerContainer listener : listeners()) {

                Collection<TopicPartition> assigned = listener.getAssignedPartitions();
                if (assigned == null) {
                    //the listener has not joined its group yet, so it has not consumed anything
                    return false;
                }
                Map<TopicPartition, OffsetSpec> latest = new LinkedHashMap<>();
                assigned.forEach(p -> latest.put(p, OffsetSpec.latest()));

                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends =
                        admin.listOffsets(latest).all().get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Map<TopicPartition, OffsetAndMetadata> committed =
                        admin.listConsumerGroupOffsets(listener.getGroupId())
                                .partitionsToOffsetAndMetadata()
                                .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                for (Map.Entry<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> end : ends.entrySet()) {
                    OffsetAndMetadata done = committed.get(end.getKey());
                    long position = done == null ? 0 : done.offset();
                    if (position < end.getValue().offset()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            SimpleLogger.warn("Could not read the shop consumer groups: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check {@code condition} every {@link #CHECK_EVERY_MS} until it holds or {@code timeoutMs}
     * has passed.
     *
     * @return whether it held
     */
    private static boolean await(long timeoutMs, BooleanSupplier condition) {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            if (condition.getAsBoolean()) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(CHECK_EVERY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    /**
     * None, deliberately: {@link #resetStateOfSUT()} empties the stock table, and this E2E is not
     * about SQL heuristics or data insertion.
     */
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

    /**
     * Publish, and return once the broker has the message. The document declares no reply for
     * any operation, so none is waited for; what the service does next is for the controller's
     * settle window to wait on, if the core asks for one.
     */
    @Override
    public void executeAsyncApiAction(AsyncApiActionDto dto, AsyncApiReplyDto reply) {

        if (dto.replyAddress != null) {
            throw new IllegalArgumentException("The shop service declares no replies, yet one was asked for on "
                    + dto.replyAddress);
        }

        //no key: the generated tests publish none
        ProducerRecord<String, String> record = new ProducerRecord<>(dto.address, null, dto.payload);
        if (dto.headers != null) {
            dto.headers.forEach((name, value) -> record.headers().add(name, bytes(value)));
        }

        try {
            producer.send(record).get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Could not publish to " + dto.address + ": " + e.getMessage(), e);
        }

        reply.published = true;
        reply.replyExpected = false;
    }

    private static byte[] bytes(String value) {
        return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
    }
}
