package org.evomaster.client.java.controller.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable representation of the control-dependence information of a method.
 * Contains only the data needed by EvoMaster core: branch objective identifiers,
 * the subset that are roots, and the parent-child relationships between them.
 */
public class ControlDependenceGraphDto implements Serializable {

    public String className;
    public String methodName;
    public List<BranchObjectiveDto> objectives = new ArrayList<>();
    public List<Integer> rootObjectiveIds = new ArrayList<>();
    public List<DependencyEdgeDto> edges = new ArrayList<>();
}

