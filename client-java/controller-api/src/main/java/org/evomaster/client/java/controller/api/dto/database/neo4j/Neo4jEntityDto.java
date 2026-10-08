package org.evomaster.client.java.controller.api.dto.database.neo4j;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of the nodes of one Neo4j entity of the SUT: its labels, properties and relationships.
 * Neo4j has no schema, so this is read from the SUT's mapping classes.
 */
public class Neo4jEntityDto {

    /**
     * Fully qualified name of the entity class in the SUT.
     */
    public String className;

    /**
     * Labels of the nodes of this entity, the primary one first.
     */
    public List<String> labels = new ArrayList<>();

    /**
     * Properties of the nodes, in declaration order.
     */
    public List<Neo4jEntityPropertyDto> properties = new ArrayList<>();

    /**
     * Relationships towards other entities, in declaration order.
     */
    public List<Neo4jEntityRelationshipDto> relationships = new ArrayList<>();

    /**
     * Default constructor
     */
    public Neo4jEntityDto() {
    }
}
