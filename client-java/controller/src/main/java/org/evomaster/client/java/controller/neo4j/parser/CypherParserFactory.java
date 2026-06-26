package org.evomaster.client.java.controller.neo4j.parser;

import org.evomaster.client.java.controller.neo4j.parser.cypher25.Cypher25AntlrParser;

/**
 * Builds the default {@link CypherParser} implementation.
 */
public class CypherParserFactory {

    private CypherParserFactory() {
    }

    public static CypherParser buildParser() {
        return new Cypher25AntlrParser();
    }
}
