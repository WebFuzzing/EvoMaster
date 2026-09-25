package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j insertion commands sent to the controller. All the nodes are inserted before any of the
 * relationships, as a relationship refers to the nodes it connects.
 */
public class Neo4jDatabaseCommandsDto {

    /** Nodes to insert, in order. */
    public List<Neo4jNodeInsertionDto> nodes = new ArrayList<>();

    /** Relationships to insert, in order, once every node is in. */
    public List<Neo4jEdgeInsertionDto> edges = new ArrayList<>();
}
