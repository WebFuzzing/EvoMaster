package org.evomaster.client.java.controller.neo4j.parser;

/**
 * Thrown when a Cypher query cannot be parsed: either it is not syntactically
 * valid Cypher, or it does not contain the expected MATCH clause.
 */
public class CypherParserException extends Exception {

    public CypherParserException(String message) {
        super(message);
    }

    public CypherParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public CypherParserException(Throwable cause) {
        super(cause);
    }
}
