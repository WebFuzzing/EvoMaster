package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionsDto;
import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionsDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents possible extra heuristics related to the code
 * execution and that do apply to all the reached testing targets.
 *
 * Example: rewarding SQL "select" operations that return non-empty sets
 */
public class ExtraHeuristicsDto {

    /**
     * List of extra heuristic values we want to optimize
     */
    public List<ExtraHeuristicEntryDto> heuristics = new ArrayList<>();

    public SqlExecutionsDto sqlSqlExecutionsDto;

    public MongoExecutionsDto mongoExecutionsDto;
}
