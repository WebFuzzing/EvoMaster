package org.evomaster.client.java.instrumentation.shared.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Serializable representation of the control-dependence information of a method.
 * Contains only the data needed by EvoMaster core: branch objective identifiers,
 * the subset that are roots, and the parent→child relationships between them.
 */
public class ControlDependenceGraphDto implements Serializable {

    private static final long serialVersionUID = -1703430508792441369L;

    private String className;
    private String methodName;
    private List<BranchObjectiveDto> objectives = new ArrayList<>();
    private List<Integer> rootObjectiveIds = new ArrayList<>();
    private List<DependencyEdgeDto> edges = new ArrayList<>();

    public ControlDependenceGraphDto() {
    }

    public ControlDependenceGraphDto(String className,
                                     String methodName,
                                     List<BranchObjectiveDto> objectives,
                                     List<Integer> rootObjectiveIds,
                                     List<DependencyEdgeDto> edges) {
        this.className = className;
        this.methodName = methodName;
        if (objectives != null) {
            this.objectives = new ArrayList<>(objectives);
        }
        if (rootObjectiveIds != null) {
            this.rootObjectiveIds = new ArrayList<>(rootObjectiveIds);
        }
        if (edges != null) {
            this.edges = new ArrayList<>(edges);
        }
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<BranchObjectiveDto> getObjectives() {
        return Collections.unmodifiableList(objectives);
    }

    public void setObjectives(List<BranchObjectiveDto> objectives) {
        this.objectives = objectives == null ? new ArrayList<>() : new ArrayList<>(objectives);
    }

    public List<Integer> getRootObjectiveIds() {
        return Collections.unmodifiableList(rootObjectiveIds);
    }

    public void setRootObjectiveIds(List<Integer> rootObjectiveIds) {
        this.rootObjectiveIds = rootObjectiveIds == null ? new ArrayList<>() : new ArrayList<>(rootObjectiveIds);
    }

    public List<DependencyEdgeDto> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public void setEdges(List<DependencyEdgeDto> edges) {
        this.edges = edges == null ? new ArrayList<>() : new ArrayList<>(edges);
    }

    public void addObjective(BranchObjectiveDto objective) {
        if (objective != null) {
            this.objectives.add(objective);
        }
    }

    public void addRootObjectiveId(Integer id) {
        if (id != null) {
            this.rootObjectiveIds.add(id);
        }
    }

    public void addEdge(DependencyEdgeDto edge) {
        if (edge != null) {
            this.edges.add(edge);
        }
    }

    /**
     * Descriptor and numeric id for a branch objective (true or false outcome).
     */
    public static class BranchObjectiveDto implements Serializable {

        private static final long serialVersionUID = -8122197698885173402L;

        private int id;
        private String descriptiveId;

        public BranchObjectiveDto() {
        }

        public BranchObjectiveDto(int id, String descriptiveId) {
            this.id = id;
            this.descriptiveId = descriptiveId;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getDescriptiveId() {
            return descriptiveId;
        }

        public void setDescriptiveId(String descriptiveId) {
            this.descriptiveId = descriptiveId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchObjectiveDto that = (BranchObjectiveDto) o;
            return id == that.id && Objects.equals(descriptiveId, that.descriptiveId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, descriptiveId);
        }
    }

    /**
     * Directed edge describing that {@code parentObjectiveId} must be covered before {@code childObjectiveId}.
     */
    public static class DependencyEdgeDto implements Serializable {

        private static final long serialVersionUID = 3167520314102399629L;

        private int parentObjectiveId;
        private int childObjectiveId;

        public DependencyEdgeDto() {
        }

        public DependencyEdgeDto(int parentObjectiveId, int childObjectiveId) {
            this.parentObjectiveId = parentObjectiveId;
            this.childObjectiveId = childObjectiveId;
        }

        public int getParentObjectiveId() {
            return parentObjectiveId;
        }

        public void setParentObjectiveId(int parentObjectiveId) {
            this.parentObjectiveId = parentObjectiveId;
        }

        public int getChildObjectiveId() {
            return childObjectiveId;
        }

        public void setChildObjectiveId(int childObjectiveId) {
            this.childObjectiveId = childObjectiveId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyEdgeDto that = (DependencyEdgeDto) o;
            return parentObjectiveId == that.parentObjectiveId &&
                    childObjectiveId == that.childObjectiveId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(parentObjectiveId, childObjectiveId);
        }
    }
}

