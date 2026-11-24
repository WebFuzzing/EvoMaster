package org.evomaster.test.utils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility functions used in the generated tests of AsyncAPI services to publish over Kafka
 */
public class KafkaEMUtils {

    private static final long POLL_MS = 200;

    /**
     * Publish one message and, when a reply topic is given, wait for the reply to it.
     *
     * The consumer is positioned at the end of the reply topic before publishing, and a new
     * correlation id is used on each call, so a reply from an earlier call is never returned.
     *
     * @param broker            bootstrap servers, eg "localhost:9092"
     * @param topic             where to publish
     * @param replyTopic        where the reply is expected, or null if none is
     * @param payload           message body, or null for an empty one
     * @param correlationHeader header the correlation id is sent and matched in, or null to take
     *                          the first message on the reply topic
     * @param timeoutMs         how long to wait for the reply
     * @param headerPairs       extra headers, as name/value pairs
     * @return the reply payload, or null if none arrived in time or none was expected
     */
    public static String publishAndAwaitReply(String broker, String topic, String replyTopic, String payload,
                                              String correlationHeader, long timeoutMs, String... headerPairs)
            throws Exception {

        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("auto.offset.reset", "latest");
        props.put("enable.auto.commit", "false");

        return publishAndAwaitReply(
                () -> new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer()),
                () -> new KafkaProducer<>(props, new StringSerializer(), new StringSerializer()),
                topic, replyTopic, payload, correlationHeader, timeoutMs, headerPairs);
    }

    /**
     * The same publish/reply lifecycle with client factories, so module tests can exercise it
     * using Kafka's in-memory clients. Clients created here are owned and closed by this call.
     */
    static String publishAndAwaitReply(Supplier<Consumer<String, String>> consumerFactory,
                                      Supplier<Producer<String, String>> producerFactory,
                                      String topic, String replyTopic, String payload,
                                      String correlationHeader, long timeoutMs, String... headerPairs)
            throws Exception {
        if (headerPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Headers must be given as name/value pairs");
        }

        String correlationId = UUID.randomUUID().toString();
        try (Consumer<String, String> consumer = replyTopic == null ? null : consumerFactory.get()) {
            boolean awaitReply = consumer != null && positionAtEnd(consumer, replyTopic);
            try (Producer<String, String> producer = producerFactory.get()) {
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, payload == null ? "" : payload);
                for (int i = 0; i < headerPairs.length; i += 2) {
                    record.headers().add(headerPairs[i], headerPairs[i + 1].getBytes(StandardCharsets.UTF_8));
                }
                if (correlationHeader != null) {
                    record.headers().add(correlationHeader, correlationId.getBytes(StandardCharsets.UTF_8));
                }
                producer.send(record).get();

                if (!awaitReply) {
                    return null;
                }
                long deadline = System.currentTimeMillis() + timeoutMs;
                while (System.currentTimeMillis() < deadline) {
                    for (ConsumerRecord<String, String> consumed : consumer.poll(Duration.ofMillis(POLL_MS))) {
                        if (correlationHeader == null) {
                            return consumed.value();
                        }
                        Header correlation = consumed.headers().lastHeader(correlationHeader);
                        if (correlation != null && correlation.value() != null
                                && correlationId.equals(new String(correlation.value(), StandardCharsets.UTF_8))) {
                            return consumed.value();
                        }
                    }
                }
                return null;
            }
        }
    }

    /** Position every reply partition before publishing, excluding replies from earlier calls. */
    private static boolean positionAtEnd(Consumer<String, String> consumer, String replyTopic) {
        List<TopicPartition> partitions = new ArrayList<>();
        List<PartitionInfo> partitionInfos = consumer.partitionsFor(replyTopic);
        if (partitionInfos != null) {
            for (PartitionInfo info : partitionInfos) {
                partitions.add(new TopicPartition(info.topic(), info.partition()));
            }
        }
        if (partitions.isEmpty()) {
            //No existing reply partitions to observe; the message is still published.
            return false;
        }
        consumer.assign(partitions);
        consumer.seekToEnd(partitions);
        //seekToEnd is lazy: resolve every partition before the service can reply to the publish.
        for (TopicPartition partition : partitions) {
            consumer.position(partition);
        }
        return true;
    }

    /**
     * Start observing what the service emits to the given topics, from their current end, so
     * that only what a message published afterwards makes it emit is collected. A topic that
     * does not exist yet is picked up from its start once it does, by awaitEmitted.
     * Close it, or hand it to awaitEmitted, which does.
     */
    public static AutoCloseable observeEmitted(String broker, String... topics) {
        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("enable.auto.commit", "false");
        KafkaConsumer<String,String> consumer =
                new KafkaConsumer<>(props,
                        new StringDeserializer(), new StringDeserializer());
        try {
            List<TopicPartition> parts = new ArrayList<>();
            for (String topic : topics) {
                List<PartitionInfo> info = consumer.partitionsFor(topic);
                for (PartitionInfo p : info == null
                        ? Collections.<PartitionInfo>emptyList() : info) {
                    parts.add(new TopicPartition(p.topic(), p.partition()));
                }
            }
            if (!parts.isEmpty()) {
                consumer.assign(parts);
                consumer.seekToEnd(parts);
                //seekToEnd is lazy: asking for the positions is what makes it take effect now
                for (TopicPartition p : parts) {
                    consumer.position(p);
                }
            }
            return consumer;
        } catch (RuntimeException e) {
            consumer.close();
            throw e;
        }
    }

    /**
     * Collect what the service emitted to the given topics since observeEmitted, until
     * each topic got as many messages as expected holds for it, at the same position, or
     * timeoutMs went by, and close the observer.
     *
     * @return topic -> payloads emitted to it, in arrival order, with every topic present
     */
    public static Map<String, List<String>> awaitEmitted(AutoCloseable observer,
            int[] expected, long timeoutMs, String... topics) throws Exception {
        @SuppressWarnings("unchecked")
        KafkaConsumer<String,String> consumer =
                (KafkaConsumer<String,String>) observer;
        Map<String, List<String>> emitted = new LinkedHashMap<>();
        for (String topic : topics) {
            emitted.put(topic, new ArrayList<>());
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        try {
            while (System.currentTimeMillis() < deadline) {
                boolean arrived = true;
                for (int i = 0; i < topics.length; i++) {
                    if (emitted.get(topics[i]).size() < expected[i]) {
                        arrived = false;
                    }
                }
                if (arrived) {
                    break;
                }
                Set<TopicPartition> assigned =
                        new LinkedHashSet<>(consumer.assignment());
                Set<String> known = new HashSet<>();
                for (TopicPartition p : assigned) {
                    known.add(p.topic());
                }
                //a topic created since observing began only holds what was emitted since: read it all
                List<TopicPartition> created = new ArrayList<>();
                for (String topic : topics) {
                    List<PartitionInfo> info =
                            known.contains(topic) ? null : consumer.partitionsFor(topic);
                    for (PartitionInfo p : info == null
                            ? Collections.<PartitionInfo>emptyList() : info) {
                        created.add(new TopicPartition(p.topic(), p.partition()));
                    }
                }
                if (!created.isEmpty()) {
                    assigned.addAll(created);
                    consumer.assign(assigned);
                    consumer.seekToBeginning(created);
                }
                if (assigned.isEmpty()) {
                    Thread.sleep(POLL_MS);
                    continue;
                }
                for (ConsumerRecord<String,String> r :
                        consumer.poll(Duration.ofMillis(POLL_MS))) {
                    List<String> payloads = emitted.get(r.topic());
                    if (payloads != null) {
                        payloads.add(r.value());
                    }
                }
            }
            return emitted;
        } finally {
            consumer.close();
        }
    }

    /**
     * Publish while observing emitted messages. On failure, close the observer before
     * propagating the exception; on success, awaitEmitted owns its cleanup.
     */
    public static String publishAndAwaitReplyObserved(AutoCloseable observer, String broker, String topic,
            String replyTopic, String payload, String correlationHeader, long timeoutMs, String... headerPairs)
            throws Exception {
        try {
            return publishAndAwaitReply(broker, topic, replyTopic, payload, correlationHeader, timeoutMs, headerPairs);
        } catch (Exception | Error failure) {
            try {
                observer.close();
            } catch (Exception closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
    }
}
