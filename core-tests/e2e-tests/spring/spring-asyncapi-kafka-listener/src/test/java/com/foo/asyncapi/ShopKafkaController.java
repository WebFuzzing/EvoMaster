package com.foo.asyncapi;

import com.foo.asyncapi.shop.ShopKafkaApplication;
import com.foo.asyncapi.shop.ShopTopics;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.kafka.KafkaAsyncApiDriverHelper;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    /**
     * The document describing every operation of the shop but restocking.
     */
    public static final String DOCUMENT = "/asyncapi/shop-kafka.yaml";

    /**
     * The document describing restocking, whose handler calls the supplier over HTTP.
     */
    public static final String RESTOCK_DOCUMENT = "/asyncapi/shop-restock-kafka.yaml";

    private static final String BOOTSTRAP_PROPERTY = "spring.kafka.bootstrap-servers";

    private static final String DELETE_STOCK = "DELETE FROM stock";

    /**
     * The bean name of the executor the service renders invoices on.
     */
    private static final String INVOICE_EXECUTOR = "invoiceExecutor";

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

    private final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

    private ConfigurableApplicationContext ctx;

    private KafkaAsyncApiDriverHelper helper;

    /**
     * Which of the shop's documents the search is given, as a resource path.
     */
    private final String document;

    public ShopKafkaController() {
        this(DOCUMENT);
    }

    /**
     * @param document which of {@link #DOCUMENT} and {@link #RESTOCK_DOCUMENT} to give the search.
     *                 The service is the same either way, with every listener running.
     */
    public ShopKafkaController(String document) {
        this.document = Objects.requireNonNull(document);
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

        helper = new KafkaAsyncApiDriverHelper(bootstrap);
        helper.createTopics(ShopTopics.ALL);

        ctx = new SpringApplicationBuilder(ShopKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .properties(BOOTSTRAP_PROPERTY + "=" + bootstrap)
                .run();

        helper.awaitConsumerGroupsAssigned(consumerGroups(), ASSIGNMENT_TIMEOUT_MS);

        //this service has no URL: what there is to know is where its broker listens
        return bootstrap;
    }

    /** The group identifiers configured on the running shop listeners. */
    private List<String> consumerGroups() {
        return listeners().stream().map(MessageListenerContainer::getGroupId).collect(Collectors.toList());
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
        if (helper != null) {
            quietly(helper::close);
            helper = null;
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
        if (!helper.awaitConsumerGroupsCaughtUp(consumerGroups(), CONSUMED_TIMEOUT_MS)) {
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

        helper.executeAsyncApiAction(dto, reply);
    }
}
