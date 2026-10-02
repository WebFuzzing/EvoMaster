package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The execution result of a {@link Neo4jDatabaseCommandsDto}.
 */
public class Neo4jInsertionResultsDto {

    /**
     * The key is the {@link Neo4jNodeInsertionDto#id} of an inserted node, the value is the
     * {@code elementId} Neo4j gave it. A node that failed to insert has no entry.
     */
    public Map<Long, String> idMapping = new LinkedHashMap<>();

    /**
     * Whether the node at the same index of {@link Neo4jDatabaseCommandsDto#nodes} was inserted.
     */
    public List<Boolean> nodeExecutionResults = new ArrayList<>();

    /**
     * Whether the relationship at the same index of {@link Neo4jDatabaseCommandsDto#edges} was inserted.
     */
    public List<Boolean> edgeExecutionResults = new ArrayList<>();
}
