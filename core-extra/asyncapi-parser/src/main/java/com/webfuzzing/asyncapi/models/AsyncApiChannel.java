package com.webfuzzing.asyncapi.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An entry under {@code channels:}: one addressable place on the broker (a Kafka topic, an AMQP
 * queue or routing key, an MQTT topic, a WebSocket endpoint) plus the set of messages it
 * carries.
 */
public class AsyncApiChannel {

    /**
     * The one protocol whose binding decides where a message actually goes.
     */
    public static final String KAFKA = "kafka";

    private final String name;

    private final String address;

    /**
     * Names of the servers this channel is available on, from its {@code servers} array.
     * Empty means every server, which is the specification's default.
     */
    private final List<String> servers;

    /**
     * Key is the channel-local message key, which is what a {@code $ref} of the form
     * {@code #/channels/<channel>/messages/<localKey>} addresses and is frequently not the
     * component id. Value is the id of that message in {@link AsyncApiDocument#getMessages()}.
     */
    private final Map<String, String> messageKeys;

    /**
     * Ids of every message this channel carries, in declaration order and distinct: the values
     * of {@link #messageKeys}, since two local keys may name the same message.
     */
    private final List<String> messageIds;

    /**
     * Key is the parameter name, as used by a {@code {placeholder}} in the address.
     * Value is that parameter's declaration, kept as a raw node.
     */
    private final Map<String, JsonNode> parameters;

    private final AsyncApiChannelBindings bindings;

    private AsyncApiChannel(Builder builder) {
        this.name = builder.name;
        this.address = builder.address;
        this.servers = Collections.unmodifiableList(builder.servers);
        this.messageKeys = Collections.unmodifiableMap(new LinkedHashMap<>(builder.messageKeys));
        this.parameters = Collections.unmodifiableMap(builder.parameters);
        this.bindings = builder.bindings;
        //distinct, as two local keys may well point at the same message definition
        this.messageIds = Collections.unmodifiableList(
                new ArrayList<>(new LinkedHashSet<>(this.messageKeys.values())));
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * The map key under {@code channels:}, e.g. "bessjRequest". This is the key {@code $ref}
     * uses, and it is not the same thing as the broker address.
     */
    public String getName() {
        return name;
    }

    /**
     * The broker-side address, e.g. "ncs.bessj.request".
     *
     * Null on purpose: the specification allows an explicit {@code address: null} to say the
     * address is not known statically and is determined at run time. It may also contain
     * {@code {parameter}} placeholders, which are left in place here -- see
     * {@link #getParameters()}.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Names of the servers this channel is available on, from the channel's {@code servers}
     * array. Empty means "all of them", which is the specification's default.
     */
    public List<String> getServers() {
        return servers;
    }

    /**
     * Channel-local message key -&gt; the id of that message in
     * {@link AsyncApiDocument#getMessages()}.
     *
     * The indirection is needed because a {@code $ref} may address a message through the
     * channel, as {@code #/channels/<channel>/messages/<localKey>}, and the local key is
     * frequently not the component id: a channel may expose
     * {@code #/components/messages/errorMessage} as just "error". Messages written inline in
     * the channel are also here, mapped to the synthetic id they were promoted under.
     */
    public Map<String, String> getMessageKeys() {
        return messageKeys;
    }

    /**
     * The ids of every message this channel carries, in declaration order.
     */
    public List<String> getMessageIds() {
        return messageIds;
    }

    /**
     * Parameter name -&gt; its declaration node, from {@code parameters:}. These back the
     * {@code {placeholders}} in {@link #getAddress()}. Kept as raw nodes: resolving an address
     * to a concrete topic is a run-time concern, not a parsing one.
     */
    public Map<String, JsonNode> getParameters() {
        return parameters;
    }

    /**
     * Protocol bindings declared on this channel.
     */
    public AsyncApiChannelBindings getBindings() {
        return bindings;
    }

    /**
     * The address to actually use on the wire for a given protocol.
     *
     * Normally this is just {@link #getAddress()}, but a binding may override it: the Kafka
     * binding has its own {@code topic} field, and when present it is the topic that is used
     * rather than the channel address. The address itself is left untouched so the declaration
     * stays readable.
     */
    public String effectiveAddress(String protocol) {

        String topic = bindings.getKafkaTopic();

        if (protocol != null && KAFKA.equals(protocol.toLowerCase(Locale.ENGLISH))
                && topic != null && !topic.trim().isEmpty()) {
            return topic;
        }

        return address;
    }

    public static class Builder {

        private final String name;
        private String address;
        /** @see AsyncApiChannel#servers */
        private List<String> servers = Collections.emptyList();
        /** @see AsyncApiChannel#messageKeys */
        private Map<String, String> messageKeys = Collections.emptyMap();
        /** @see AsyncApiChannel#parameters */
        private Map<String, JsonNode> parameters = Collections.emptyMap();
        private AsyncApiChannelBindings bindings = AsyncApiChannelBindings.none();

        private Builder(String name) {
            this.name = name;
        }

        public Builder address(String address) { this.address = address; return this; }

        public Builder servers(List<String> servers) { this.servers = servers; return this; }

        public Builder messageKeys(Map<String, String> messageKeys) {
            this.messageKeys = messageKeys;
            return this;
        }

        public Builder parameters(Map<String, JsonNode> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder bindings(AsyncApiChannelBindings bindings) { this.bindings = bindings; return this; }

        public AsyncApiChannel build() {
            return new AsyncApiChannel(this);
        }
    }
}
