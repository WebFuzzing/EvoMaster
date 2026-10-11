package com.foo.asyncapi.ncs;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.controller.problem.AsyncApiProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.kafka.KafkaAsyncApiDriverHelper;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.utils.SimpleLogger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.kafka.KafkaContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The driver for NCS over Kafka. It owns the broker and the service; everything it does with
 * Kafka is delegated to {@link KafkaAsyncApiDriverHelper}.
 */
public class NcsKafkaController extends EmbeddedSutController {

    private static final String KAFKA_IMAGE = "apache/kafka:3.8.0";

    private static final String DOCUMENT = "/asyncapi/ncs-kafka.yaml";

    private static final String BOOTSTRAP_PROPERTY = "ncs.kafka.bootstrap";

    private static final String GROUP_PROPERTY = "ncs.kafka.group";

    /**
     * The consumer group the service joins. Named by the driver, so that it can ask how far
     * behind the group is. The broker is a new container at every start, so the same name
     * never resumes old offsets.
     */
    private static final String GROUP = "ncs";

    /**
     * How long to wait, at start-up, for the service to join its group, and then to answer.
     */
    private static final long READY_TIMEOUT_MS = 60_000;

    /**
     * How long resetStateOfSUT waits for the service to take in what the previous test
     * published. NCS answers in milliseconds, so this is only reached when something is wrong.
     */
    private static final long CAUGHT_UP_TIMEOUT_MS = 5_000;

    /**
     * A triangle request that is valid, so the service answers with a result rather than an error.
     */
    private static final String PROBE_REQUEST = "{\"a\":3,\"b\":4,\"c\":5}";

    private static final String CORRELATION_POINTER = "/" + NcsRequestConsumer.CORRELATION_HEADER;

    /**
     * Every topic the document names, created up front so that no first request or reply has
     * to wait for auto-creation.
     */
    private static final List<String> TOPICS = Arrays.asList(
            NcsService.TRIANGLE_REQUEST, NcsService.TRIANGLE_REPLY,
            NcsService.BESSJ_REQUEST, NcsService.BESSJ_REPLY,
            NcsService.EXPINT_REQUEST, NcsService.EXPINT_REPLY,
            NcsService.FISHER_REQUEST, NcsService.FISHER_REPLY,
            NcsService.GAMMQ_REQUEST, NcsService.GAMMQ_REPLY,
            NcsService.REMAINDER_REQUEST, NcsService.REMAINDER_REPLY);

    private static final List<String> GROUPS = Collections.singletonList(GROUP);

    private final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    private ConfigurableApplicationContext ctx;

    private KafkaAsyncApiDriverHelper kafkaHelper;

    public NcsKafkaController() {
        super.setControllerPort(0);
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

        kafkaHelper = new KafkaAsyncApiDriverHelper(bootstrap);
        kafkaHelper.createTopics(TOPICS);

        ctx = new SpringApplicationBuilder(NcsKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .properties(BOOTSTRAP_PROPERTY + "=" + bootstrap, GROUP_PROPERTY + "=" + GROUP)
                .run();

        //started is not listening, see awaitAnswering: wait for the group's partitions, then for an answer
        kafkaHelper.awaitConsumerGroupsAssigned(GROUPS, READY_TIMEOUT_MS);
        kafkaHelper.awaitAnswering(probe(), READY_TIMEOUT_MS);

        //this service has no URL: what there is to know is where its broker listens
        return bootstrap;
    }

    private static AsyncApiActionDto probe() {
        AsyncApiActionDto probe = new AsyncApiActionDto();
        probe.address = NcsService.TRIANGLE_REQUEST;
        probe.replyAddress = NcsService.TRIANGLE_REPLY;
        probe.payload = PROBE_REQUEST;
        probe.correlationLocation = AsyncApiActionDto.CorrelationLocation.HEADER;
        probe.correlationPointer = CORRELATION_POINTER;
        return probe;
    }

    @Override
    public void stopSut() {

        /*
            Every close is given a deadline and is allowed to fail: a Kafka client that will not
            shut down must not keep the whole controller from stopping, which the E2E asserts.
         */
        if (kafkaHelper != null) {
            kafkaHelper.close();
            kafkaHelper = null;
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

            And a request of the previous test may still be in the service's hands, eg one
            whose reply was given up on. Waiting for the group to catch up keeps what it
            covers out of this test.
         */
        kafkaHelper.resetReplyConsumers();
        if (!kafkaHelper.awaitConsumerGroupsCaughtUp(GROUPS, CAUGHT_UP_TIMEOUT_MS)) {
            SimpleLogger.warn("NCS did not catch up with its requests within " + CAUGHT_UP_TIMEOUT_MS + " ms");
        }
    }

    @Override
    public void executeAsyncApiAction(AsyncApiActionDto dto, AsyncApiReplyDto reply) {
        kafkaHelper.executeAsyncApiAction(dto, reply);
    }

    /**
     * NCS commits its offsets once it has answered what it polled, so a group with no lag is
     * a service with nothing left to handle.
     */
    @Override
    public Boolean areAsyncApiConsumersCaughtUp() {
        return kafkaHelper == null ? null : kafkaHelper.consumerGroupsCaughtUp(GROUPS);
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
}
