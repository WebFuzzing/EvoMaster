package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * A relationship to insert into a Neo4j database, between two nodes inserted by the same
 * {@link Neo4jDatabaseCommandsDto}.
 */
public class Neo4jEdgeInsertionDto {

    /** Type of the relationship. */
    public String type;

    /** The {@link Neo4jNodeInsertionDto#id} of the node the relationship starts from. */
    public Long fromNodeId;

    /** The {@link Neo4jNodeInsertionDto#id} of the node the relationship points to. */
    public Long toNodeId;

    /** Properties of the relationship. */
    public List<Neo4jInsertionEntryDto> properties = new ArrayList<>();
}
