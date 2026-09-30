package org.evomaster.client.java.controller.api.dto.database.neo4j;

/**
 * A relationship declared in a Neo4j entity, towards another entity.
 */
public class Neo4jEntityRelationshipDto {

    /**
     * Relationship type in the graph.
     */
    public String type;

    /**
     * Labels of the entity at the other end, its primary label first.
     */
    public java.util.List<String> targetLabels = new java.util.ArrayList<>();

    /**
     * Whether the relationship starts from this entity (outgoing) or arrives to it.
     */
    public Boolean isOutgoing;

    /**
     * Default constructor
     */
    public Neo4jEntityRelationshipDto() {
    }
}
