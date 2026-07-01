package org.evomaster.client.java.controller.neo4j.parser;

import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;

/**
 * Parses the MATCH clause of a Cypher query into a structural {@link MatchOperation}.
 * <br>
 * Implementations are built on top of the official Neo4j Cypher grammar (ANTLR4),
 * so that any query accepted by this parser is guaranteed to be valid for Neo4j.
 */
public interface CypherParser {

    /**
     * Parse a Cypher query, extracting its MATCH pattern and conditions.
     *
     * @param query the Cypher query string
     * @return the structural pattern and conditions of the MATCH clause
     * @throws CypherParserException if the query is not syntactically valid Cypher,
     *                               or does not contain a MATCH clause
     */
    MatchOperation parse(String query) throws CypherParserException;
}
