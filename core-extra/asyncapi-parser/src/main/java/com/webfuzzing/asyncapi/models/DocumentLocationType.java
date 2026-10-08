package com.webfuzzing.asyncapi.models;

/**
 * The kind of place an AsyncAPI document was read from, which decides how a reference to a
 * neighbouring document is retrieved in turn.
 */
public enum DocumentLocationType {

    /**
     * An http(s) URL.
     */
    REMOTE,

    /**
     * A file on disk, given either as a path or as a {@code file:} URL.
     */
    LOCAL,

    /**
     * The document was handed over as text, so it has no neighbours.
     */
    MEMORY,

    /**
     * A resource on the classpath.
     */
    RESOURCE
}
