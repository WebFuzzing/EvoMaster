package org.evomaster.client.java.controller.neo4j.dsl;

/**
 * Entry point for a Neo4j insertion sequence.
 */
public interface Neo4jSequenceDsl {

    /**
     * Starts a node insertion.
     *
     * @param id identifier of this insertion, through which a relationship refers to the node
     * @param labels labels of the node, possibly none
     * @return node statement
     */
    Neo4jStatementDsl createNode(Long id, String... labels);

    /**
     * Starts a relationship insertion between two nodes of this sequence.
     *
     * @param type type of the relationship
     * @param fromNodeId id of the node the relationship starts from
     * @param toNodeId id of the node the relationship points to
     * @return relationship statement
     */
    Neo4jStatementDsl createEdge(String type, Long fromNodeId, Long toNodeId);
}
