package com.webfuzzing.asyncapi.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An entry under {@code operations:}, i.e. something an application does on a channel.
 *
 * A {@link Action#RECEIVE} operation carrying a {@link #getReply()} is the case black-box
 * testing is built on: it is the only shape in which publishing produces something observable
 * from outside.
 */
public class AsyncApiOperation {

    /**
     * What the application does with the messages on the channel -- stated from the
     * application's point of view, which is usually the system under test's.
     *
     * So a {@link #RECEIVE} is what the service consumes, and therefore what a tester would
     * publish to; a {@link #SEND} is what it emits, and therefore what a tester would subscribe
     * to. The polarity is easy to invert and worth stating: AsyncAPI 2.x expressed the same
     * idea with the opposite one, naming its blocks after what <i>other</i> parties do.
     */
    public enum Action { SEND, RECEIVE }

    private final String name;

    private final Action action;

    private final String channelName;

    /**
     * Ids of the messages this operation carries, in declaration order: the subset its
     * {@code messages} array selects, or all of its channel's when it declares none.
     */
    private final List<String> messageIds;

    private final AsyncApiReply reply;

    /**
     * Names of the security schemes this operation requires, as keys into
     * {@link AsyncApiDocument#getSecuritySchemes()}.
     */
    private final List<String> security;

    /**
     * Key is the protocol name, e.g. "kafka" or "amqp".
     * Value is the binding this operation declares for that protocol, as a raw node.
     */
    private final Map<String, JsonNode> bindings;

    private final String title;

    private final String summary;

    private final String description;

    private AsyncApiOperation(Builder builder) {
        this.name = builder.name;
        this.action = builder.action;
        this.channelName = builder.channelName;
        this.messageIds = Collections.unmodifiableList(builder.messageIds);
        this.reply = builder.reply;
        this.security = Collections.unmodifiableList(builder.security);
        this.bindings = Collections.unmodifiableMap(builder.bindings);
        this.title = builder.title;
        this.summary = builder.summary;
        this.description = builder.description;
    }

    public static Builder builder(String name, Action action, String channelName) {
        return new Builder(name, action, channelName);
    }

    /**
     * The map key under {@code operations:}. This is the stable identity a coverage target
     * would hang on, so it is never synthesised or rewritten.
     */
    public String getName() {
        return name;
    }

    public Action getAction() {
        return action;
    }

    /**
     * Key of the channel this operation acts on.
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Ids of the messages this operation carries on its own channel: the subset its
     * {@code messages} array selects, or all of the channel's when it declares none.
     *
     * The distinction matters on transports like WebSocket, where one channel routinely carries
     * dozens of unrelated messages and each operation drives exactly one.
     */
    public List<String> getMessageIds() {
        return messageIds;
    }

    public AsyncApiReply getReply() {
        return reply;
    }

    /**
     * Names of the security schemes this operation requires.
     */
    public List<String> getSecurity() {
        return security;
    }

    /**
     * Protocol bindings as declared, keyed by protocol. Nothing is lifted out of these yet, as
     * no operation-level binding field currently changes what a client does.
     */
    public Map<String, JsonNode> getBindings() {
        return bindings;
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

        private final String name;
        private final Action action;
        private final String channelName;
        /** @see AsyncApiOperation#messageIds */
        private List<String> messageIds = Collections.emptyList();
        private AsyncApiReply reply;
        /** @see AsyncApiOperation#security */
        private List<String> security = Collections.emptyList();
        /** @see AsyncApiOperation#bindings */
        private Map<String, JsonNode> bindings = Collections.emptyMap();
        private String title;
        private String summary;
        private String description;

        private Builder(String name, Action action, String channelName) {
            this.name = Objects.requireNonNull(name, "name");
            this.action = Objects.requireNonNull(action, "action");
            this.channelName = Objects.requireNonNull(channelName, "channelName");
        }

        public Builder messageIds(List<String> messageIds) { this.messageIds = Objects.requireNonNull(messageIds, "messageIds"); return this; }

        public Builder reply(AsyncApiReply reply) { this.reply = reply; return this; }

        public Builder security(List<String> security) { this.security = Objects.requireNonNull(security, "security"); return this; }

        public Builder bindings(Map<String, JsonNode> bindings) { this.bindings = Objects.requireNonNull(bindings, "bindings"); return this; }

        public Builder title(String title) { this.title = title; return this; }

        public Builder summary(String summary) { this.summary = summary; return this; }

        public Builder description(String description) { this.description = description; return this; }

        public AsyncApiOperation build() {
            return new AsyncApiOperation(this);
        }
    }
}
