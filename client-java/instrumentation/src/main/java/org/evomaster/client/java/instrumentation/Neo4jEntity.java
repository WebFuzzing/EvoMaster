package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The shape of the nodes the SUT maps to one of its entity classes: labels, properties and relationships.
 * Neo4j has no schema of its own, so this is what stands in for one.
 */
public class Neo4jEntity implements Serializable {

    /** Fully qualified name of the entity class. */
    private final String className;

    /** Labels of the nodes of this entity, the primary one first. */
    private final List<String> labels;

    /** Properties of the nodes, in declaration order. */
    private final List<Neo4jEntityProperty> properties;

    /** Relationships to other entities, in declaration order. */
    private final List<Neo4jEntityRelationship> relationships;

    /**
     * @param className entity class
     * @param labels labels of its nodes, at least one
     * @param properties its properties
     * @param relationships its relationships to other entities
     */
    public Neo4jEntity(String className, List<String> labels, List<Neo4jEntityProperty> properties,
                       List<Neo4jEntityRelationship> relationships) {
        this.className = Objects.requireNonNull(className);
        if (labels == null || labels.isEmpty()) {
            throw new IllegalArgumentException("An entity needs at least one label: " + className);
        }
        this.labels = Collections.unmodifiableList(labels);
        this.properties = Collections.unmodifiableList(Objects.requireNonNull(properties));
        this.relationships = Collections.unmodifiableList(Objects.requireNonNull(relationships));
    }

    public String getClassName() {
        return className;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<Neo4jEntityProperty> getProperties() {
        return properties;
    }

    public List<Neo4jEntityRelationship> getRelationships() {
        return relationships;
    }

    @Override
    public String toString() {
        return className + labels + " " + properties + " " + relationships;
    }
}
