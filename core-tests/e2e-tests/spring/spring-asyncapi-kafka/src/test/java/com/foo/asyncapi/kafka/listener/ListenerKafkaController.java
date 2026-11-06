package com.foo.asyncapi.kafka.listener;

import com.foo.asyncapi.kafka.KafkaController;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.evomaster.client.java.utils.SimpleLogger;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * The driver for {@link ListenerKafkaApplication}. Nothing is ever answered, so it waits for the
 * listener to be assigned its topic at start-up, and between tests for the service to have
 * handled everything published.
 */
public class ListenerKafkaController extends KafkaController {

    /**
     * How long to wait, at start-up, for the listener to be given its topic.
     */
    private static final long ASSIGNMENT_TIMEOUT_MS = 60_000;

    /**
     * How long to wait, between two tests, for the service to be done. Far more than it needs;
     * reaching it is reported, not fatal: at worst, some coverage is credited to the next test.
     */
    private static final long DONE_TIMEOUT_MS = 10_000;

    /**
     * How often the waits above check again.
     */
    private static final long CHECK_EVERY_MS = 20;

    /**
     * How long one request to the broker may take, within those waits.
     */
    private static final long ADMIN_CALL_MS = 1_000;

    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(5);

    /**
     * To read the listener's offsets between tests, while the service is up.
     */
    private AdminClient admin;

    public ListenerKafkaController() {
        super(ListenerKafkaApplication.class, "/asyncapi/listener-kafka.yaml");
    }

    @Override
    protected List<String> getTopics() {
        return Collections.singletonList(OrderListener.ORDER_PLACED);
    }

    @Override
    public String startSut() {
        String bootstrap = super.startSut();
        admin = createAdminClient();
        return bootstrap;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.asyncapi.kafka.listener.";
    }

    /**
     * A message handled before the listener joined its group would have its coverage credited to
     * a later message, or to none.
     */
    @Override
    protected void awaitServiceReady() {
        if (!await(ASSIGNMENT_TIMEOUT_MS, () -> !assignedPartitions().isEmpty())) {
            throw new IllegalStateException("The listener was not assigned its topic within " + ASSIGNMENT_TIMEOUT_MS + " ms");
        }
    }

    /**
     * Wait until every message published so far has been handled, and then until the dispatches
     * it led to have run, so that none of them is covered in the next test.
     */
    @Override
    public void resetStateOfSUT() {
        super.resetStateOfSUT();

        if (!await(DONE_TIMEOUT_MS, this::everythingConsumed)) {
            SimpleLogger.warn("The service had not consumed everything published within " + DONE_TIMEOUT_MS + " ms");
        }

        //one thread, running tasks in order: an empty task done means every earlier one is
        try {
            ctx.getBean(ExecutorService.class).submit(() -> { }).get(DONE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            SimpleLogger.warn("The service had not dispatched everything within " + DONE_TIMEOUT_MS + " ms");
        }
    }

    @Override
    public void stopSut() {
        if (admin != null) {
            try {
                admin.close(CLOSE_TIMEOUT);
            } catch (RuntimeException e) {
                SimpleLogger.warn("Ignored while stopping the SUT: " + e.getMessage());
            }
            admin = null;
        }
        super.stopSut();
    }

    private MessageListenerContainer listener() {
        return ctx.getBean(KafkaListenerEndpointRegistry.class).getListenerContainer(OrderListener.LISTENER_ID);
    }

    /**
     * Empty, rather than null, before the listener joins its group or during a rebalance.
     */
    private Collection<TopicPartition> assignedPartitions() {
        Collection<TopicPartition> assigned = listener().getAssignedPartitions();
        return assigned == null ? Collections.emptyList() : assigned;
    }

    /**
     * Whether the listener's group has committed up to the end of its partitions.
     */
    private boolean everythingConsumed() {

        Collection<TopicPartition> assigned = assignedPartitions();
        if (assigned.isEmpty()) {
            //nothing to compare with: not joined yet, or rebalancing
            return false;
        }

        try {
            Map<TopicPartition, OffsetSpec> latest = new LinkedHashMap<>();
            assigned.forEach(p -> latest.put(p, OffsetSpec.latest()));

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> ends =
                    admin.listOffsets(latest).all().get(ADMIN_CALL_MS, TimeUnit.MILLISECONDS);
            Map<TopicPartition, OffsetAndMetadata> committed =
                    admin.listConsumerGroupOffsets(listener().getGroupId())
                            .partitionsToOffsetAndMetadata()
                            .get(ADMIN_CALL_MS, TimeUnit.MILLISECONDS);

            for (Map.Entry<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> end : ends.entrySet()) {
                OffsetAndMetadata done = committed.get(end.getKey());
                if ((done == null ? 0 : done.offset()) < end.getValue().offset()) {
                    return false;
                }
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            SimpleLogger.uniqueWarn("Could not read the listener's offsets: " + e.getMessage());
            return false;
        }
    }

    /**
     * @return whether {@code condition} held within {@code timeoutMs}
     */
    private static boolean await(long timeoutMs, BooleanSupplier condition) {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
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
        return true;
    }
}
