package org.evomaster.client.java.controller.api.dto.database.execution;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto;

import java.util.ArrayList;
import java.util.List;

/**
 * A MATCH query that the graph did not satisfy, digested into the insertion that would satisfy it:
 * one node per node of the pattern, one relationship per edge, and a property for each condition
 * that compares a property against a known value.
 */
public class Neo4jFailedQuery {

    /** The Cypher text of the query, kept for reporting. */
    public String query;

    /** The nodes to create, with their labels and the properties the query filters on. */
    public List<Neo4jNodeInsertionDto> nodes = new ArrayList<>();

    /** The relationships to create between those nodes, with their types and properties. */
    public List<Neo4jEdgeInsertionDto> edges = new ArrayList<>();

    public Neo4jFailedQuery() {
    }

    /**
     * @param query the Cypher text of the query
     * @param nodes the nodes to create
     * @param edges the relationships to create
     */
    public Neo4jFailedQuery(String query, List<Neo4jNodeInsertionDto> nodes, List<Neo4jEdgeInsertionDto> edges) {
        this.query = query;
        this.nodes = new ArrayList<>(nodes);
        this.edges = new ArrayList<>(edges);
    }
}
