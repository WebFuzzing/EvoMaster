package com.foo.somedifferentpackage.examples.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.instrumentation.example.kafka.KafkaClients;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class KafkaClientsImpl implements KafkaClients {

    private static final String NO_BROKER = "localhost:9";

    private static final Duration WAIT = Duration.ofMillis(10);

    private static final long WAIT_MS = 10;

    private static final long SEND_TIMEOUT_SECONDS = 10;

    @Override
    public int pollAConsumer(String topic, int records) {
        TopicPartition partition = new TopicPartition(topic, 0);
        MockConsumer<String, String> mock = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        mock.assign(Collections.singletonList(partition));
        mock.updateBeginningOffsets(Collections.singletonMap(partition, 0L));
        for (int i = 0; i < records; i++) {
            mock.addRecord(new ConsumerRecord<>(topic, 0, i, "key" + i, "value" + i));
        }
        Consumer<String, String> consumer = mock;
        return consumer.poll(WAIT).count();
    }

    private static KafkaConsumer<String, String> unsubscribedConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, NO_BROKER);
        //instances rather than class names, which kafka-clients would load outside this class loader
        return new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
    }

    @Override
    public void pollAnUnsubscribedKafkaConsumer() {
        KafkaConsumer<String, String> consumer = unsubscribedConsumer();
        try {
            consumer.poll(WAIT);
        } finally {
            consumer.close(Duration.ZERO);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void pollAnUnsubscribedKafkaConsumerTheLegacyWay() {
        KafkaConsumer<String, String> consumer = unsubscribedConsumer();
        try {
            consumer.poll(WAIT_MS);
        } finally {
            consumer.close(Duration.ZERO);
        }
    }

    @Override
    public boolean sendWithAKafkaProducer(String topic, String key, String payload) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, NO_BROKER);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "0");
        KafkaProducer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());
        try {
            Future<?> sent = producer.send(new ProducerRecord<>(topic, key, payload));
            return sent.isDone();
        } finally {
            producer.close(Duration.ZERO);
        }
    }

    @Override
    public int sendWithAKafkaTemplate(String topic, String key, String payload) throws Exception {

        MockProducer<String, String> actual = new MockProducer<>(true, new StringSerializer(), new StringSerializer());

        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, NO_BROKER);

        DefaultKafkaProducerFactory<String, String> factory =
                new DefaultKafkaProducerFactory<String, String>(configs, new StringSerializer(), new StringSerializer()) {
                    @Override
                    protected Producer<String, String> createRawProducer(Map<String, Object> rawConfigs) {
                        return actual;
                    }
                };

        KafkaTemplate<String, String> template = new KafkaTemplate<>(factory);
        template.send(topic, key, payload).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        factory.destroy();

        return actual.history().size();
    }
}
