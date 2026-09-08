package com.webfuzzing.asyncapi.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * An entry under {@code channels:}: one addressable place on the broker (a Kafka topic, an AMQP
 * queue or routing key, an MQTT topic, a WebSocket endpoint) plus the set of messages it
 * carries.
 */
public class AsyncApiChannel {

    private final String name;

    private final String address;

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

    private AsyncApiChannel(Builder builder) {
        this.name = builder.name;
        this.address = builder.address;
        this.messageKeys = Collections.unmodifiableMap(new LinkedHashMap<>(builder.messageKeys));
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
     * {@code {parameter}} placeholders, which are left in place here.
     */
    public String getAddress() {
        return address;
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

    public static class Builder {

        private final String name;
        private String address;
        /** @see AsyncApiChannel#messageKeys */
        private Map<String, String> messageKeys = Collections.emptyMap();

        private Builder(String name) {
            this.name = name;
        }

        public Builder address(String address) { this.address = address; return this; }

        public Builder messageKeys(Map<String, String> messageKeys) {
            this.messageKeys = messageKeys;
            return this;
        }

        public AsyncApiChannel build() {
            return new AsyncApiChannel(this);
        }
    }
}
