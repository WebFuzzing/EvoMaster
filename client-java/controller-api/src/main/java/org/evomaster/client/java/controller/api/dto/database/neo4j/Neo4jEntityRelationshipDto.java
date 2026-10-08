package org.evomaster.client.java.controller.api.dto.database.neo4j;

import java.util.ArrayList;
import java.util.List;

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
    public List<String> targetLabels = new ArrayList<>();

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
