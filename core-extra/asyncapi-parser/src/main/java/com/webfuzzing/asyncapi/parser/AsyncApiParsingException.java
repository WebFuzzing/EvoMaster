package com.webfuzzing.asyncapi.parser;

/**
 * Raised when a document cannot be turned into a model at all: it is not readable as YAML or
 * JSON, it is not an AsyncAPI document, or it is of a version that is not handled.
 *
 * Anything narrower than that -- one broken message, one unresolvable reference, one payload in
 * a format that is not JSON Schema -- is not raised but recorded as a warning on the parsed
 * document, so that one exotic element cannot make the rest of it unusable.
 */
public class AsyncApiParsingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AsyncApiParsingException(String message) {
        super(message);
    }

    public AsyncApiParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
