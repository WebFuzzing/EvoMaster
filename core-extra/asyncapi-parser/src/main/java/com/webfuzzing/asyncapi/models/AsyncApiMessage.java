package com.webfuzzing.asyncapi.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One message definition, i.e. the shape of what travels on a channel.
 *
 * A message is what a channel carries: a payload, optional headers, and the metadata that says
 * how to read them.
 */
public class AsyncApiMessage {

    private final String id;

    private final String name;

    private final String contentType;

    private final JsonNode payload;

    private final JsonNode headers;

    private final AsyncApiCorrelationId correlationId;

    private final JsonNode kafkaKey;

    /**
     * Key is the protocol name, e.g. "kafka" or "amqp".
     * Value is the binding this message declares for that protocol, as a raw node.
     */
    private final Map<String, JsonNode> bindings;

    private final List<JsonNode> examples;

    private final String title;

    private final String summary;

    private final String description;

    private AsyncApiMessage(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.contentType = builder.contentType;
        this.payload = builder.payload;
        this.headers = builder.headers;
        this.correlationId = builder.correlationId;
        this.kafkaKey = builder.kafkaKey;
        this.bindings = Collections.unmodifiableMap(builder.bindings);
        this.examples = Collections.unmodifiableList(builder.examples);
        this.title = builder.title;
        this.summary = builder.summary;
        this.description = builder.description;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * The key this message is registered under in {@link AsyncApiDocument#getMessages()}, which
     * is its component key under {@code components.messages}.
     */
    public String getId() {
        return id;
    }

    /**
     * The message's own {@code name} field, falling back to {@link #getId()} when it declares
     * none.
     */
    public String getName() {
        return name;
    }

    /**
     * MIME type of the payload, falling back to the document's {@code defaultContentType}.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * The payload's JSON Schema, as a raw node.
     *
     * Deliberately not resolved: any {@code $ref} in here is left in the local
     * {@code #/components/schemas/<key>} form, and every schema it can reach is in
     * {@link AsyncApiDocument#getComponentSchemas()}. Null when the message declares no
     * payload, or when the payload is in a schema format that cannot be read as JSON Schema
     * (in which case there is a matching entry in {@link AsyncApiDocument#getWarnings()}).
     */
    public JsonNode getPayload() {
        return payload;
    }

    /**
     * The JSON Schema of the message headers, kept separate from the payload exactly as the
     * specification keeps them. Same "raw node, unresolved refs" treatment as
     * {@link #getPayload()}.
     */
    public JsonNode getHeaders() {
        return headers;
    }

    /**
     * Where the correlation id travels, when the message declares it.
     */
    public AsyncApiCorrelationId getCorrelationId() {
        return correlationId;
    }

    /**
     * The JSON Schema of the Kafka message key, from {@code bindings.kafka.key}. Null when the
     * message declares no key binding, in which case the broker distributes records across
     * partitions itself.
     */
    public JsonNode getKafkaKey() {
        return kafkaKey;
    }

    /**
     * Protocol bindings as declared, keyed by protocol name.
     */
    public Map<String, JsonNode> getBindings() {
        return bindings;
    }

    /**
     * The message's {@code examples} entries, kept raw. Not interpreted here, but useful later
     * as seeds for the search.
     */
    public List<JsonNode> getExamples() {
        return examples;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public static class Builder {

        private final String id;
        private String name;
        private String contentType;
        private JsonNode payload;
        private JsonNode headers;
        private AsyncApiCorrelationId correlationId;
        private JsonNode kafkaKey;
        /** @see AsyncApiMessage#bindings */
        private Map<String, JsonNode> bindings = Collections.emptyMap();
        private List<JsonNode> examples = Collections.emptyList();
        private String title;
        private String summary;
        private String description;

        private Builder(String id) {
            this.id = id;
            this.name = id;
        }

        public Builder name(String name) { this.name = name; return this; }

        public Builder contentType(String contentType) { this.contentType = contentType; return this; }

        public Builder payload(JsonNode payload) { this.payload = payload; return this; }

        public Builder headers(JsonNode headers) { this.headers = headers; return this; }

        public Builder correlationId(AsyncApiCorrelationId correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder kafkaKey(JsonNode kafkaKey) { this.kafkaKey = kafkaKey; return this; }

        public Builder bindings(Map<String, JsonNode> bindings) { this.bindings = bindings; return this; }

        public Builder examples(List<JsonNode> examples) { this.examples = examples; return this; }

        public Builder title(String title) { this.title = title; return this; }

        public Builder summary(String summary) { this.summary = summary; return this; }

        public Builder description(String description) { this.description = description; return this; }

        public AsyncApiMessage build() {
            return new AsyncApiMessage(this);
        }
    }
}
