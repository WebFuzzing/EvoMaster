package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of a relationship type in Neo4j.
 * Represents the actual graph relationship: (sourceNode)-[type]->(targetNode)
 */
public class Neo4jRelationshipSchema implements Serializable {

    private final String relationshipType;
    private final String sourceNodeLabel;
    private final String targetNodeLabel;
    private final String propertiesSchema;

    public Neo4jRelationshipSchema(String relationshipType, String sourceNodeLabel,
                                    String targetNodeLabel, String propertiesSchema) {
        this.relationshipType = relationshipType;
        this.sourceNodeLabel = sourceNodeLabel;
        this.targetNodeLabel = targetNodeLabel;
        this.propertiesSchema = propertiesSchema;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getSourceNodeLabel() {
        return sourceNodeLabel;
    }

    public String getTargetNodeLabel() {
        return targetNodeLabel;
    }

    public String getPropertiesSchema() {
        return propertiesSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Neo4jRelationshipSchema that = (Neo4jRelationshipSchema) o;
        return Objects.equals(relationshipType, that.relationshipType)
                && Objects.equals(sourceNodeLabel, that.sourceNodeLabel)
                && Objects.equals(targetNodeLabel, that.targetNodeLabel)
                && Objects.equals(propertiesSchema, that.propertiesSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relationshipType, sourceNodeLabel, targetNodeLabel, propertiesSchema);
    }

    @Override
    public String toString() {
        return "Neo4jRelationshipSchema{" +
                "relationshipType='" + relationshipType + '\'' +
                ", sourceNodeLabel='" + sourceNodeLabel + '\'' +
                ", targetNodeLabel='" + targetNodeLabel + '\'' +
                ", propertiesSchema='" + propertiesSchema + '\'' +
                '}';
    }
}
