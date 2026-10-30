package com.foo.asyncapi.kafka.simple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * Reads foo.request and bar.request, answers each on its reply topic, and copies the
 * correlation id header over so that the requester can pair the two.
 */
@Component
public class SimpleRequestConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(SimpleRequestConsumer.class);

    public static final String FOO_REQUEST = "foo.request";
    public static final String FOO_REPLY = "foo.reply";
    public static final String BAR_REQUEST = "bar.request";
    public static final String BAR_REPLY = "bar.reply";

    static final String CORRELATION_HEADER = "correlationId";

    private static final Duration POLL = Duration.ofMillis(200);

    private final ObjectMapper mapper = new ObjectMapper();

    private final String bootstrapServers;

    private final SimpleService service;

    private volatile boolean running;

    private volatile boolean repliesEnabled = true;

    private Thread loop;

    private KafkaConsumer<String, String> consumer;

    private KafkaProducer<String, String> producer;

    public SimpleRequestConsumer(@Value("${kafka.bootstrap}") String bootstrapServers, SimpleService service) {
        this.bootstrapServers = bootstrapServers;
        this.service = service;
    }

    @Override
    public void start() {

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        //a fresh group each start, so that a restarted service does not resume old offsets
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "simple-" + System.nanoTime());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(FOO_REQUEST, BAR_REQUEST));

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProps);

        running = true;
        loop = new Thread(this::consume, "simple-kafka-consumer");
        loop.start();
    }

    private void consume() {
        try {
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(POLL);
                for (ConsumerRecord<String, String> request : records) {
                    answer(request);
                }
            }
        } catch (WakeupException e) {
            //asked to stop
        } finally {
            consumer.close();
            producer.close();
        }
    }

    /** Used by the no-reply E2E to suppress responses independently of broker timing. */
    public void setRepliesEnabled(boolean enabled) {
        repliesEnabled = enabled;
    }

    private void answer(ConsumerRecord<String, String> request) {
        if (!repliesEnabled) {
            return;
        }

        String replyTopic = replyTopicOf(request.topic());

        String body;
        try {
            JsonNode x = mapper.readTree(request.value()).get("x");
            if (x == null || !x.isInt()) {
                throw new IllegalArgumentException("x must be an integer");
            }
            int value = compute(request.topic(), x.intValue());
            body = mapper.createObjectNode().put("value", value).toString();
        } catch (Exception e) {
            //as a web framework answers a crash with a 500: a reply, but not one the contract declares
            body = mapper.createObjectNode().put("error", String.valueOf(e.getMessage())).toString();
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(replyTopic, request.key(), body);

        Header correlation = request.headers().lastHeader(CORRELATION_HEADER);
        if (correlation != null) {
            record.headers().add(CORRELATION_HEADER, correlation.value());
        }

        producer.send(record, (metadata, failure) -> {
            if (failure != null) {
                log.warn("Could not publish a reply to {}", replyTopic, failure);
            }
        });
    }

    private static String replyTopicOf(String requestTopic) {
        switch (requestTopic) {
            case FOO_REQUEST: return FOO_REPLY;
            case BAR_REQUEST: return BAR_REPLY;
            default: throw new IllegalArgumentException("Not a request topic: " + requestTopic);
        }
    }

    private int compute(String requestTopic, int x) {
        switch (requestTopic) {
            case FOO_REQUEST: return service.foo(x);
            case BAR_REQUEST: return service.bar(x);
            default: throw new IllegalArgumentException("Not a request topic: " + requestTopic);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
        if (loop != null) {
            try {
                loop.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
