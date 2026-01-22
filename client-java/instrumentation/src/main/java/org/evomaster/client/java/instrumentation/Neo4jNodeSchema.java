package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Schema of nodes of a specific type (label) in Neo4j.
 */
public class Neo4jNodeSchema implements Serializable {
    private final String nodeLabel;
    private final String nodeSchema;

    public Neo4jNodeSchema(String nodeLabel, String nodeSchema) {
        this.nodeLabel = nodeLabel;
        this.nodeSchema = nodeSchema;
    }

    public String getNodeLabel() {
        return nodeLabel;
    }

    public String getNodeSchema() {
        return nodeSchema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Neo4jNodeSchema that = (Neo4jNodeSchema) o;
        return Objects.equals(nodeLabel, that.nodeLabel) && Objects.equals(nodeSchema, that.nodeSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeLabel, nodeSchema);
    }

    @Override
    public String toString() {
        return "Neo4jNodeSchema{" +
                "nodeLabel='" + nodeLabel + '\'' +
                ", nodeSchema='" + nodeSchema + '\'' +
                '}';
    }
}
