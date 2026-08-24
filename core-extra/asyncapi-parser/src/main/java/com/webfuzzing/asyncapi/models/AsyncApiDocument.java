package com.webfuzzing.asyncapi.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A parsed AsyncAPI 3.x document, normalised so that a caller never has to walk the raw
 * YAML/JSON tree.
 *
 * "Normalised" means three things:
 *
 * <ol>
 * <li>every message is reachable from {@link #getMessages()} by a single id, including those
 *     written inline inside a channel, which are promoted here under a synthetic id;</li>
 * <li>all {@code $ref} between AsyncAPI constructs (channels, operations, messages, correlation
 *     ids, traits) are already followed, and are represented as plain keys;</li>
 * <li>all {@code $ref} <i>inside</i> a message payload / headers JSON Schema are instead left
 *     verbatim, in the local {@code #/components/schemas/<key>} form, and every schema they can
 *     reach is in {@link #getComponentSchemas()}. A message whose schema reaches a reference
 *     that cannot be followed is dropped rather than handed on, so whatever consumes the
 *     payload later may resolve any reference it meets against the component schemas
 *     alone.</li>
 * </ol>
 *
 * Parsing is deliberately lenient: anything that only makes a single element unusable is
 * reported in {@link #getWarnings()} rather than raised, so one exotic message cannot make a
 * whole document unusable.
 */
public class AsyncApiDocument {

    /**
     * What a message with no {@code contentType} of its own is taken to carry, when the
     * document declares no {@code defaultContentType} either.
     */
    public static final String DEFAULT_CONTENT_TYPE = "application/json";

    private final String rawText;

    private final DocumentLocation sourceLocation;

    private final String version;

    private final String defaultContentType;

    /**
     * Key is the server name, i.e. its key under {@code servers}.
     * Value is the server declared under it.
     */
    private final Map<String, AsyncApiServer> servers;

    /**
     * Key is the channel key, i.e. its key under {@code channels} and the one a {@code $ref}
     * addresses it by. Value is the channel declared under it.
     */
    private final Map<String, AsyncApiChannel> channels;

    /**
     * Key is the operation key, i.e. its key under {@code operations}.
     * Value is the operation declared under it.
     */
    private final Map<String, AsyncApiOperation> operations;

    /**
     * Key is the message id: its key under {@code components.messages}, or the synthetic
     * {@code <channelKey>.<localMessageKey>} for a message written inline in a channel.
     * Value is the message that id refers to.
     */
    private final Map<String, AsyncApiMessage> messages;

    /**
     * Key is the schema name, i.e. its key under {@code components.schemas}.
     * Value is that schema as a raw JSON Schema node.
     */
    private final Map<String, JsonNode> componentSchemas;

    /**
     * Key is the security scheme key, i.e. its key under {@code components.securitySchemes},
     * or a synthetic name for a scheme written inline where it is used.
     * Value is the scheme declared under it.
     */
    private final Map<String, AsyncApiSecurityScheme> securitySchemes;

    /**
     * Everything that could not be read but did not stop the document being usable, one entry
     * per problem, in the order the parser met them.
     */
    private final List<String> warnings;

    private AsyncApiDocument(Builder builder) {
        this.rawText = builder.rawText;
        this.sourceLocation = builder.sourceLocation;
        this.version = builder.version;
        this.defaultContentType = builder.defaultContentType;
        this.servers = Collections.unmodifiableMap(builder.servers);
        this.channels = Collections.unmodifiableMap(builder.channels);
        this.operations = Collections.unmodifiableMap(builder.operations);
        this.messages = Collections.unmodifiableMap(builder.messages);
        this.componentSchemas = Collections.unmodifiableMap(builder.componentSchemas);
        this.securitySchemes = Collections.unmodifiableMap(builder.securitySchemes);
        this.warnings = Collections.unmodifiableList(builder.warnings);
    }

    public static Builder builder(String rawText, DocumentLocation sourceLocation, String version) {
        return new Builder(rawText, sourceLocation, version);
    }

    /**
     * The document exactly as retrieved, before any parsing. What needs the original text is
     * reporting a problem in terms the user can find in their file.
     */
    public String getRawText() {
        return rawText;
    }

    /**
     * Where {@link #getRawText()} came from.
     */
    public DocumentLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Value of the root {@code asyncapi} field, e.g. "3.0.0".
     */
    public String getVersion() {
        return version;
    }

    /**
     * Root {@code defaultContentType}, used by any message not declaring its own
     * {@code contentType}. Falls back to {@link #DEFAULT_CONTENT_TYPE}.
     */
    public String getDefaultContentType() {
        return defaultContentType;
    }

    /**
     * Server name -&gt; server. Empty when the document declares no {@code servers} block, which
     * is common: many published specs describe only the message contract and leave the broker
     * to be supplied at deployment.
     */
    public Map<String, AsyncApiServer> getServers() {
        return servers;
    }

    /**
     * Channel key -&gt; channel.
     */
    public Map<String, AsyncApiChannel> getChannels() {
        return channels;
    }

    /**
     * Operation key -&gt; operation.
     */
    public Map<String, AsyncApiOperation> getOperations() {
        return operations;
    }

    /**
     * Message id -&gt; message. Contains both the messages declared under
     * {@code components.messages} and those declared inline inside a channel, the latter under
     * the synthetic id {@code <channelKey>.<localMessageKey>}.
     */
    public Map<String, AsyncApiMessage> getMessages() {
        return messages;
    }

    /**
     * Schema key -&gt; the raw JSON Schema node, as declared under {@code components.schemas}.
     */
    public Map<String, JsonNode> getComponentSchemas() {
        return componentSchemas;
    }

    /**
     * Security scheme key -&gt; scheme, from {@code components.securitySchemes}.
     */
    public Map<String, AsyncApiSecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    /**
     * Everything that went wrong without being fatal: a reference that could not be resolved, a
     * payload in a format that cannot be read. Reported to the user, so that a surprising
     * result can be traced back to the document.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * The messages an operation can carry on its own channel, resolved to their definitions.
     */
    public List<AsyncApiMessage> messagesOf(AsyncApiOperation operation) {
        return resolveMessages(operation.getMessageIds());
    }

    /**
     * The messages the operation's reply can be. Empty when it declares no reply.
     */
    public List<AsyncApiMessage> replyMessagesOf(AsyncApiOperation operation) {

        if (operation.getReply() == null) {
            return Collections.emptyList();
        }

        return resolveMessages(operation.getReply().getMessageIds());
    }

    /**
     * The channel an operation acts on. An operation naming a channel that is not declared is
     * dropped while parsing, so this is always present.
     */
    public AsyncApiChannel channelOf(AsyncApiOperation operation) {

        AsyncApiChannel channel = channels.get(operation.getChannelName());

        if (channel == null) {
            throw new IllegalArgumentException(
                    "Operation '" + operation.getName() + "' acts on channel '"
                            + operation.getChannelName() + "', which this document does not declare");
        }

        return channel;
    }

    /**
     * The channel an operation's reply arrives on, when it declares one that is usable.
     */
    public AsyncApiChannel replyChannelOf(AsyncApiOperation operation) {

        if (operation.getReply() == null || operation.getReply().getChannelName() == null) {
            return null;
        }

        return channels.get(operation.getReply().getChannelName());
    }

    /**
     * The servers a channel is available on. A channel that names none is available on all of
     * them, which is what the specification prescribes and what callers would otherwise all
     * have to remember for themselves.
     */
    public List<AsyncApiServer> serversOf(AsyncApiChannel channel) {

        if (channel.getServers().isEmpty()) {
            return new ArrayList<>(servers.values());
        }

        List<AsyncApiServer> found = new ArrayList<>();

        for (String name : channel.getServers()) {
            AsyncApiServer server = servers.get(name);
            if (server != null) {
                found.add(server);
            }
        }

        return found;
    }

    private List<AsyncApiMessage> resolveMessages(List<String> ids) {

        List<AsyncApiMessage> found = new ArrayList<>();

        for (String id : ids) {
            AsyncApiMessage message = messages.get(id);
            if (message != null) {
                found.add(message);
            }
        }

        return found;
    }

    public static class Builder {

        private final String rawText;
        private final DocumentLocation sourceLocation;
        private final String version;
        private String defaultContentType = DEFAULT_CONTENT_TYPE;
        /** @see AsyncApiDocument#servers */
        private Map<String, AsyncApiServer> servers = Collections.emptyMap();
        /** @see AsyncApiDocument#channels */
        private Map<String, AsyncApiChannel> channels = Collections.emptyMap();
        /** @see AsyncApiDocument#operations */
        private Map<String, AsyncApiOperation> operations = Collections.emptyMap();
        /** @see AsyncApiDocument#messages */
        private Map<String, AsyncApiMessage> messages = Collections.emptyMap();
        /** @see AsyncApiDocument#componentSchemas */
        private Map<String, JsonNode> componentSchemas = Collections.emptyMap();
        /** @see AsyncApiDocument#securitySchemes */
        private Map<String, AsyncApiSecurityScheme> securitySchemes = Collections.emptyMap();
        /** @see AsyncApiDocument#warnings */
        private List<String> warnings = Collections.emptyList();

        private Builder(String rawText, DocumentLocation sourceLocation, String version) {
            this.rawText = rawText;
            this.sourceLocation = sourceLocation;
            this.version = version;
        }

        public Builder defaultContentType(String defaultContentType) {
            this.defaultContentType = defaultContentType;
            return this;
        }

        public Builder servers(Map<String, AsyncApiServer> servers) { this.servers = servers; return this; }

        public Builder channels(Map<String, AsyncApiChannel> channels) { this.channels = channels; return this; }

        public Builder operations(Map<String, AsyncApiOperation> operations) {
            this.operations = operations;
            return this;
        }

        public Builder messages(Map<String, AsyncApiMessage> messages) { this.messages = messages; return this; }

        public Builder componentSchemas(Map<String, JsonNode> componentSchemas) {
            this.componentSchemas = componentSchemas;
            return this;
        }

        public Builder securitySchemes(Map<String, AsyncApiSecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes;
            return this;
        }

        public Builder warnings(List<String> warnings) { this.warnings = warnings; return this; }

        public AsyncApiDocument build() {
            return new AsyncApiDocument(this);
        }
    }
}
