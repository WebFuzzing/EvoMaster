package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * A node to insert into a Neo4j database.
 */
public class Neo4jNodeInsertionDto {

    /**
     * Identifier of this insertion, unique within one {@link Neo4jDatabaseCommandsDto}. It is not
     * stored in the database: it only lets a {@link Neo4jEdgeInsertionDto} refer to this node.
     */
    public Long id;

    /** Labels of the node, possibly none. */
    public List<String> labels = new ArrayList<>();

    /** Properties of the node. */
    public List<Neo4jInsertionEntryDto> properties = new ArrayList<>();
}
