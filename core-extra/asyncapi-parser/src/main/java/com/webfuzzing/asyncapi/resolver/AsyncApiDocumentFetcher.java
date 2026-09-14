package com.webfuzzing.asyncapi.resolver;

import com.webfuzzing.asyncapi.models.DocumentLocationType;

/**
 * How the parser gets hold of a document it does not already have: given an absolute location
 * and how to read it, hand back the text.
 *
 * Kept as an interface so that the resolver does not depend on the retrieval code, and so tests
 * can supply documents without any I/O.
 */
@FunctionalInterface
public interface AsyncApiDocumentFetcher {

    /**
     * @param location an absolute location, already resolved against the referring document
     * @param type     how that location is to be read
     * @return the text of the document
     * @throws RuntimeException if the document cannot be retrieved. The caller turns that into
     *                          a warning rather than letting it escape, as one unreachable
     *                          document costs only what refers to it.
     */
    String fetch(String location, DocumentLocationType type);
}
