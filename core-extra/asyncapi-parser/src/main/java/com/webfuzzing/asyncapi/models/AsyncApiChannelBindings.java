package com.webfuzzing.asyncapi.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Map;

/**
 * The subset of the protocol bindings that a client acts on, plus the untouched originals.
 *
 * Only a handful of fields are lifted out, because only a handful change what a client has to
 * do. Everything else stays in {@link #getRaw()} so nothing is lost and a later transport can
 * read it without the model having to grow first.
 */
public class AsyncApiChannelBindings {

    private final String kafkaTopic;

    private final String amqpIs;

    private final String amqpQueue;

    private final String amqpExchange;

    private final String wsMethod;

    /**
     * Key is the protocol name, e.g. "kafka" or "amqp".
     * Value is the binding declared for that protocol, exactly as written.
     */
    private final Map<String, JsonNode> raw;

    private AsyncApiChannelBindings(Builder builder) {
        this.kafkaTopic = builder.kafkaTopic;
        this.amqpIs = builder.amqpIs;
        this.amqpQueue = builder.amqpQueue;
        this.amqpExchange = builder.amqpExchange;
        this.wsMethod = builder.wsMethod;
        this.raw = Collections.unmodifiableMap(builder.raw);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Nothing declared, for a channel that has no bindings at all.
     */
    public static AsyncApiChannelBindings none() {
        return builder().build();
    }

    /**
     * {@code bindings.kafka.topic}. When set, it overrides the channel address for Kafka.
     */
    public String getKafkaTopic() {
        return kafkaTopic;
    }

    /**
     * {@code bindings.amqp.is}, either "queue" or "routingKey". Decides whether a publisher
     * should address a queue directly or go through an exchange.
     */
    public String getAmqpIs() {
        return amqpIs;
    }

    /**
     * {@code bindings.amqp.queue.name}.
     */
    public String getAmqpQueue() {
        return amqpQueue;
    }

    /**
     * {@code bindings.amqp.exchange.name}.
     */
    public String getAmqpExchange() {
        return amqpExchange;
    }

    /**
     * {@code bindings.ws.method}, the HTTP method used for the opening handshake.
     */
    public String getWsMethod() {
        return wsMethod;
    }

    /**
     * Every binding as declared, keyed by protocol name.
     */
    public Map<String, JsonNode> getRaw() {
        return raw;
    }

    public static class Builder {

        private String kafkaTopic;
        private String amqpIs;
        private String amqpQueue;
        private String amqpExchange;
        private String wsMethod;
        /** @see AsyncApiChannelBindings#raw */
        private Map<String, JsonNode> raw = Collections.emptyMap();

        private Builder() {
        }

        public Builder kafkaTopic(String kafkaTopic) { this.kafkaTopic = kafkaTopic; return this; }

        public Builder amqpIs(String amqpIs) { this.amqpIs = amqpIs; return this; }

        public Builder amqpQueue(String amqpQueue) { this.amqpQueue = amqpQueue; return this; }

        public Builder amqpExchange(String amqpExchange) { this.amqpExchange = amqpExchange; return this; }

        public Builder wsMethod(String wsMethod) { this.wsMethod = wsMethod; return this; }

        public Builder raw(Map<String, JsonNode> raw) { this.raw = raw; return this; }

        public AsyncApiChannelBindings build() {
            return new AsyncApiChannelBindings(this);
        }
    }
}
