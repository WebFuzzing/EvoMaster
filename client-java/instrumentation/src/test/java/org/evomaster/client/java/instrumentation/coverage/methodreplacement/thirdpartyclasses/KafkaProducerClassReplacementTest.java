package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.evomaster.client.java.instrumentation.EmittedMessage;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.KafkaProducerInterfaceClassReplacementTest.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * KafkaProducer.send, against a real producer with no broker. Told not to wait for one, it fails
 * each send through the returned future and the callback rather than by throwing.
 */
public class KafkaProducerClassReplacementTest {

    /**
     * Never reached: with no time to wait for metadata, a send fails before connecting.
     */
    static final String NO_BROKER = "localhost:9";

    @BeforeEach
    public void init() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void tearDown() {
        ExecutionTracer.setExecutingDriverAction(false);
        ExecutionTracer.reset();
    }

    static KafkaProducer<String, Object> producerWithNoBroker() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, NO_BROKER);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "0");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    @Test
    public void testASendThatFailsLaterIsStillRecorded() {

        KafkaProducer<String, Object> producer = producerWithNoBroker();
        try {
            Future<?> sent = KafkaProducerClassReplacement.send(producer, record(PAYLOAD));
            assertThrows(ExecutionException.class, sent::get);
        } finally {
            producer.close(Duration.ZERO);
        }

        EmittedMessage message = onlyEmitted();
        assertEquals(TOPIC, message.getAddress());
        assertEquals(PAYLOAD, message.getPayload());
    }

    @Test
    public void testWithACallbackTheCallbackGetsTheFailure() {

        AtomicReference<Exception> failure = new AtomicReference<>();

        KafkaProducer<String, Object> producer = producerWithNoBroker();
        try {
            KafkaProducerClassReplacement.send(producer, record(PAYLOAD),
                    (org.apache.kafka.clients.producer.Callback) (metadata, e) -> failure.set(e));
        } finally {
            producer.close(Duration.ZERO);
        }

        assertNotNull(failure.get(), "the callback was not given to the original");
        assertEquals(1, emitted().size());
    }

    @Test
    public void testTheDriversSendsAreNotRecorded() {

        ExecutionTracer.setExecutingDriverAction(true);

        KafkaProducer<String, Object> producer = producerWithNoBroker();
        try {
            KafkaProducerClassReplacement.send(producer, record(PAYLOAD));
        } finally {
            producer.close(Duration.ZERO);
        }

        assertTrue(emitted().isEmpty());
    }
}
