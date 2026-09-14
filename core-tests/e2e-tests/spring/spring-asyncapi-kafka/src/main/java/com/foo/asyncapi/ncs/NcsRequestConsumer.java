package com.foo.asyncapi.ncs;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Properties;

/**
 * Reads every request topic, answers each request on the matching reply topic, and copies the
 * correlation id header over so that the requester can pair the two.
 */
@Component
public class NcsRequestConsumer implements SmartLifecycle {

    /**
     * The header a request carries its correlation id in, as the document declares.
     */
    static final String CORRELATION_HEADER = "correlationId";

    private static final Duration POLL = Duration.ofMillis(200);

    private final String bootstrapServers;

    private final NcsService service;

    private volatile boolean running;

    private Thread loop;

    private KafkaConsumer<String, String> consumer;

    private KafkaProducer<String, String> producer;

    public NcsRequestConsumer(@Value("${ncs.kafka.bootstrap}") String bootstrapServers, NcsService service) {
        this.bootstrapServers = bootstrapServers;
        this.service = service;
    }

    @Override
    public void start() {

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        //a fresh group each start, so that a restarted service does not resume old offsets
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ncs-" + System.nanoTime());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(service.requestTopics());

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProps);

        running = true;
        loop = new Thread(this::consume, "ncs-kafka-consumer");
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

    private void answer(ConsumerRecord<String, String> request) {

        NcsService.Reply reply = service.handle(request.topic(), request.value());

        ProducerRecord<String, String> record = new ProducerRecord<>(reply.topic, request.key(), reply.body);

        Header correlation = request.headers().lastHeader(CORRELATION_HEADER);
        if (correlation != null) {
            record.headers().add(CORRELATION_HEADER, correlation.value());
        }

        producer.send(record);
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
