package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.database.execution.ExecutionDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents possible extra heuristics related to the code
 * execution and that do apply to all the reached testing targets.
 *
 * Example: rewarding SQL "select" operations that return non-empty sets
 */
public class ExtraHeuristicDto {

    /**
     * List of heuristics values, where the lower the better.
     * Minimum is 0. It can be considered as a "distance" to
     * minimize.
     */
    public List<Double> toMinimize = new ArrayList<>();

    /**
     * List of heuristics values, where the higher the better.
     * Note: given x, we could rather considered the value
     * 1/x to minimize. But that wouldn't work for negative x,
     * and also would make debugging more difficult (ie better to
     * look at the raw, non-transformed values).
     */
    public List<Double> toMaximize = new ArrayList<>();


    public ExecutionDto databaseExecutionDto;
}
