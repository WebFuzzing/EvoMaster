package com.webfuzzing.asyncapi.models;

import com.fasterxml.jackson.databind.JsonNode;

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
 * <li>every message is reachable from {@link #getMessages()} by a single id;</li>
 * <li>all {@code $ref} between AsyncAPI constructs (messages, correlation ids, traits) are
 *     already followed, and are represented as plain keys;</li>
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
     * Key is the message id, i.e. its key under {@code components.messages}.
     * Value is the message declared under it.
     */
    private final Map<String, AsyncApiMessage> messages;

    /**
     * Key is the schema name, i.e. its key under {@code components.schemas}.
     * Value is that schema as a raw JSON Schema node.
     */
    private final Map<String, JsonNode> componentSchemas;

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
        this.messages = Collections.unmodifiableMap(builder.messages);
        this.componentSchemas = Collections.unmodifiableMap(builder.componentSchemas);
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
     * Message id -&gt; message, as declared under {@code components.messages}.
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
     * Everything that went wrong without being fatal: a reference that could not be resolved, a
     * payload in a format that cannot be read. Reported to the user, so that a surprising
     * result can be traced back to the document.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    public static class Builder {

        private final String rawText;
        private final DocumentLocation sourceLocation;
        private final String version;
        private String defaultContentType = DEFAULT_CONTENT_TYPE;
        /** @see AsyncApiDocument#messages */
        private Map<String, AsyncApiMessage> messages = Collections.emptyMap();
        /** @see AsyncApiDocument#componentSchemas */
        private Map<String, JsonNode> componentSchemas = Collections.emptyMap();
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

        public Builder messages(Map<String, AsyncApiMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder componentSchemas(Map<String, JsonNode> componentSchemas) {
            this.componentSchemas = componentSchemas;
            return this;
        }

        public Builder warnings(List<String> warnings) { this.warnings = warnings; return this; }

        public AsyncApiDocument build() {
            return new AsyncApiDocument(this);
        }
    }
}
