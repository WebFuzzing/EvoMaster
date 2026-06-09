package org.evomaster.client.java.controller.neo4j.operations;

import java.util.Objects;

/**
 * Represents an edge in a structural MATCH pattern.
 * Contains source/target variable names, direction, and variable-length path information.
 * Type and properties are not part of the structural pattern - they become conditions.
 */
public class PatternEdge {

    private final String variableName;
    private final String sourceVariable;
    private final String targetVariable;
    private final boolean directed;
    private final boolean variableLength;
    private final Integer minLength;
    private final Integer maxLength;

    public PatternEdge(String variableName, String sourceVariable, String targetVariable, boolean directed) {
        this(variableName, sourceVariable, targetVariable, directed, false, null, null);
    }

    public PatternEdge(String variableName, String sourceVariable, String targetVariable,
                       boolean directed, boolean variableLength, Integer minLength, Integer maxLength) {
        this.variableName = variableName;
        this.sourceVariable = sourceVariable;
        this.targetVariable = targetVariable;
        this.directed = directed;
        this.variableLength = variableLength;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getSourceVariable() {
        return sourceVariable;
    }

    public String getTargetVariable() {
        return targetVariable;
    }

    public boolean isDirected() {
        return directed;
    }

    public boolean isVariableLength() {
        return variableLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    @Override
    public String toString() {
        StringBuilder relPart = new StringBuilder("[");
        if (variableName != null) {
            relPart.append(variableName);
        }
        if (variableLength) {
            relPart.append("*");
            if (minLength != null || maxLength != null) {
                if (minLength != null) relPart.append(minLength);
                relPart.append("..");
                if (maxLength != null) relPart.append(maxLength);
            }
        }
        relPart.append("]");

        if (directed) {
            return "(" + sourceVariable + ")-" + relPart + "->(" + targetVariable + ")";
        } else {
            return "(" + sourceVariable + ")-" + relPart + "-(" + targetVariable + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternEdge that = (PatternEdge) o;
        if (directed != that.directed) return false;
        if (variableLength != that.variableLength) return false;
        if (!Objects.equals(variableName, that.variableName)) return false;
        if (!Objects.equals(sourceVariable, that.sourceVariable)) return false;
        if (!Objects.equals(targetVariable, that.targetVariable)) return false;
        if (!Objects.equals(minLength, that.minLength)) return false;
        return Objects.equals(maxLength, that.maxLength);
    }

    @Override
    public int hashCode() {
        int result = variableName != null ? variableName.hashCode() : 0;
        result = 31 * result + (sourceVariable != null ? sourceVariable.hashCode() : 0);
        result = 31 * result + (targetVariable != null ? targetVariable.hashCode() : 0);
        result = 31 * result + (directed ? 1 : 0);
        result = 31 * result + (variableLength ? 1 : 0);
        result = 31 * result + (minLength != null ? minLength.hashCode() : 0);
        result = 31 * result + (maxLength != null ? maxLength.hashCode() : 0);
        return result;
    }
}
