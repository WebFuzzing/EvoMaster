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
}
