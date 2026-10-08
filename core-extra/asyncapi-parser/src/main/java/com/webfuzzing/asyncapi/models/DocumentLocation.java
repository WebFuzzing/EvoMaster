package com.webfuzzing.asyncapi.models;

import java.util.Locale;
import java.util.Objects;

/**
 * Where a document came from.
 *
 * Kept alongside the parsed model because a {@code $ref} to another document is written
 * relative to the one making it, so following one is impossible without knowing where the
 * referring document lives.
 */
public class DocumentLocation {

    /**
     * A document that was handed over as text. It has no location, so no reference to another
     * document can be resolved from it.
     */
    public static final DocumentLocation MEMORY = new DocumentLocation("", DocumentLocationType.MEMORY);

    /**
     * How a file on disk looks when written as a URL rather than as a path.
     */
    private static final String FILE_URL_PREFIX = "file:";

    private final String location;

    private final DocumentLocationType type;

    public DocumentLocation(String location, DocumentLocationType type) {
        this.location = Objects.requireNonNull(location, "location");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static DocumentLocation ofRemote(String url) {
        return new DocumentLocation(url, DocumentLocationType.REMOTE);
    }

    public static DocumentLocation ofLocal(String urlOrPath) {
        return new DocumentLocation(urlOrPath, DocumentLocationType.LOCAL);
    }

    public static DocumentLocation ofResource(String path) {
        return new DocumentLocation(path, DocumentLocationType.RESOURCE);
    }

    public String getLocation() {
        return location;
    }

    public DocumentLocationType getType() {
        return type;
    }

    /**
     * Whether this is a path on the file system written as a path, rather than as a
     * {@code file:} URL. Both are read from disk, but only the former is not a URI, so a
     * relative reference is resolved against it through the file system rather than per
     * RFC 3986.
     */
    public boolean isPlainFilePath() {
        return type == DocumentLocationType.LOCAL
                && !location.toLowerCase(Locale.ENGLISH).startsWith(FILE_URL_PREFIX);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DocumentLocation)) {
            return false;
        }
        DocumentLocation that = (DocumentLocation) other;
        return location.equals(that.location) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, type);
    }

    @Override
    public String toString() {
        return type + ":" + location;
    }
}
