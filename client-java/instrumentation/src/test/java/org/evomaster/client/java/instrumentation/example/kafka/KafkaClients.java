package org.evomaster.client.java.instrumentation.example.kafka;

/**
 * A SUT that consumes and emits Kafka records in the ways the Kafka replacements have to catch.
 * Only JDK types cross this interface, as the Kafka classes behind it are loaded, and
 * instrumented, by another class loader.
 */
public interface KafkaClients {

    /**
     * Poll, once, a consumer declared as a Consumer, with {@code records} records waiting on
     * {@code topic}.
     *
     * @return how many records the poll returned
     */
    int pollAConsumer(String topic, int records);

    /**
     * Poll a KafkaConsumer that was never subscribed, which fails with an IllegalStateException
     * before contacting any broker.
     */
    void pollAnUnsubscribedKafkaConsumer();

    /**
     * The same, with the poll(long) deprecated since kafka-clients 2.0.
     */
    void pollAnUnsubscribedKafkaConsumerTheLegacyWay();

    /**
     * Send a record with a KafkaProducer declared as such, through send(record), which
     * KafkaProducer implements by calling send(record, null) on itself.
     *
     * @return whether the send was over, as it fails at once with no broker to wait for
     */
    boolean sendWithAKafkaProducer(String topic, String key, String payload);

    /**
     * Send a record with Spring's KafkaTemplate, which sends through the CloseSafeProducer that
     * wraps the actual producer: two Producer.send calls for one record.
     *
     * @return how many records reached the actual producer
     */
    int sendWithAKafkaTemplate(String topic, String key, String payload) throws Exception;
}
