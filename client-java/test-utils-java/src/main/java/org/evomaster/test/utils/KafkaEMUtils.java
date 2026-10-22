package org.evomaster.test.utils;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

        if (headerPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Headers must be given as name/value pairs");
        }

        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("auto.offset.reset", "latest");
        props.put("enable.auto.commit", "false");

        String correlationId = UUID.randomUUID().toString();

        KafkaConsumer<String, String> consumer = null;
        KafkaProducer<String, String> producer = null;
        try {
            if (replyTopic != null) {
                consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
                List<TopicPartition> partitions = new ArrayList<>();
                List<PartitionInfo> info = consumer.partitionsFor(replyTopic);
                if (info != null) {
                    for (PartitionInfo p : info) {
                        partitions.add(new TopicPartition(p.topic(), p.partition()));
                    }
                }
                if (partitions.isEmpty()) {
                    //reply topic does not exist yet, so there is nothing to wait on
                    consumer.close();
                    consumer = null;
                } else {
                    consumer.assign(partitions);
                    consumer.seekToEnd(partitions);
                    //seekToEnd is lazy, asking for the position applies it before publishing
                    consumer.position(partitions.get(0));
                }
            }

            producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, payload == null ? "" : payload);
            for (int i = 0; i < headerPairs.length; i += 2) {
                record.headers().add(headerPairs[i], headerPairs[i + 1].getBytes(StandardCharsets.UTF_8));
            }
            if (correlationHeader != null) {
                record.headers().add(correlationHeader, correlationId.getBytes(StandardCharsets.UTF_8));
            }
            producer.send(record).get();

            if (consumer == null) {
                return null;
            }

            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(POLL_MS))) {
                    if (correlationHeader == null) {
                        return r.value();
                    }
                    Header h = r.headers().lastHeader(correlationHeader);
                    if (h != null && correlationId.equals(new String(h.value(), StandardCharsets.UTF_8))) {
                        return r.value();
                    }
                }
            }
            return null;
        } finally {
            if (producer != null) {
                producer.close();
            }
            if (consumer != null) {
                consumer.close();
            }
        }
    }
}
