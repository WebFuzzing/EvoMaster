package com.webfuzzing.asyncapi.models;

import java.util.Objects;

/**
 * A parsed {@code correlationId.location}, i.e. where in a message the value that pairs a
 * request with its reply is to be written and read.
 *
 * The specification writes it as a runtime expression, either {@code $message.header#/<pointer>}
 * or {@code $message.payload#/<pointer>}. It is parsed once, here, so that nothing downstream
 * has to take that string apart again.
 *
 * Which of the two is usable is a property of the transport, not of the document: AMQP and
 * Kafka can carry the id as metadata, while MQTT 3.1.1 and raw WebSocket have no metadata at
 * all and can only carry it inside the payload. A document may well declare a header location
 * on a transport that has no headers.
 */
public class AsyncApiCorrelationId {

    /**
     * Where the correlation id travels.
     */
    public enum Source { HEADER, PAYLOAD }

    private static final String HEADER_PREFIX = "$message.header#";

    private static final String PAYLOAD_PREFIX = "$message.payload#";

    private final String raw;

    private final Source source;

    private final String pointer;

    private final String fieldName;

    private final String description;

    public AsyncApiCorrelationId(String raw, Source source, String pointer, String description) {
        this.raw = Objects.requireNonNull(raw, "raw");
        this.source = Objects.requireNonNull(source, "source");
        this.pointer = Objects.requireNonNull(pointer, "pointer");
        this.description = description;
        this.fieldName = fieldNameOf(pointer);
    }

    /**
     * @see #parse(String, String)
     */
    public static AsyncApiCorrelationId parse(String location) {
        return parse(location, null);
    }

    /**
     * Parse a {@code correlationId.location} runtime expression, or return null if it is not
     * one of the two forms the specification defines.
     */
    public static AsyncApiCorrelationId parse(String location, String description) {

        if (location == null) {
            return null;
        }

        String trimmed = location.trim();

        Source source;
        String prefix;

        if (trimmed.startsWith(HEADER_PREFIX)) {
            source = Source.HEADER;
            prefix = HEADER_PREFIX;
        } else if (trimmed.startsWith(PAYLOAD_PREFIX)) {
            source = Source.PAYLOAD;
            prefix = PAYLOAD_PREFIX;
        } else {
            return null;
        }

        String pointer = trimmed.substring(prefix.length());

        if (pointer.trim().isEmpty() || !pointer.startsWith("/")) {
            return null;
        }

        return new AsyncApiCorrelationId(trimmed, source, pointer, description);
    }

    /**
     * The name of the field the id lives in, for the common case of a pointer one level deep.
     * Null for a nested pointer, where callers have to walk {@link #getPointer()} themselves.
     */
    private static String fieldNameOf(String pointer) {

        String[] segments = pointer.substring(pointer.startsWith("/") ? 1 : 0).split("/", -1);

        if (segments.length != 1 || segments[0].trim().isEmpty()) {
            return null;
        }

        //JSON Pointer escaping: a field whose name contains a slash writes it as '~1'
        return segments[0].replace("~1", "/").replace("~0", "~");
    }

    /**
     * The expression exactly as written, kept for error messages.
     */
    public String getRaw() {
        return raw;
    }

    public Source getSource() {
        return source;
    }

    /**
     * The JSON Pointer part, e.g. "/correlationId" or "/request_id". Always starts with "/".
     */
    public String getPointer() {
        return pointer;
    }

    /**
     * @see #fieldNameOf(String)
     */
    public String getFieldName() {
        return fieldName;
    }

    public String getDescription() {
        return description;
    }
}
