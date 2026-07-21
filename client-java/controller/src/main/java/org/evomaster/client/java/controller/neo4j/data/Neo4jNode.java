package org.evomaster.client.java.controller.neo4j.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A node of a captured Neo4j graph, used as the {@code G} side when computing the heuristic
 * {@code H(Q, G)}. It is the in-memory counterpart of a stored node: a stable id (the driver's
 * {@code elementId}), the set of labels, and the property map.
 * <p>
 * This is a plain data holder built either by hand (tests) or by reading the live database; it has
 * no relation to {@link org.evomaster.client.java.controller.neo4j.operations.PatternNode}, which is
 * the structural node of a parsed query pattern.
 */
public class Neo4jNode {

    private final String id;
    private final Set<String> labels;
    private final Map<String, Object> properties;

    public Neo4jNode(String id, Set<String> labels, Map<String, Object> properties) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.labels = labels != null ? new LinkedHashSet<>(labels) : new LinkedHashSet<>();
        this.properties = properties != null ? new LinkedHashMap<>(properties) : new LinkedHashMap<>();
    }

    public String getId() {
        return id;
    }

    public Set<String> getLabels() {
        return Collections.unmodifiableSet(labels);
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Returns true when the property is present on this node. Distinguishes an absent property
     * (the operand cannot be valuated) from a present property whose value is {@code null}.
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String toString() {
        return "Neo4jNode{" + id + ", labels=" + labels + ", props=" + properties + "}";
    }
}
