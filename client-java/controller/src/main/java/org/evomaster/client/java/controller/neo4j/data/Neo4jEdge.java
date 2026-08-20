package org.evomaster.client.java.controller.neo4j.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An edge (relationship) of a captured Neo4j graph: a stable id, a single type, the ids of its two
 * endpoint nodes (source and target), and its property map. A relationship in Neo4j is always stored
 * with a direction (source → target).
 */
public class Neo4jEdge {

    /**
     * Stable identifier of this relationship, as reported by the driver ({@code elementId}). Unique
     * within the captured graph, and used to tell two relationships apart when enumerating mappings.
     */
    private final String id;

    /**
     * The relationship type, e.g. {@code KNOWS}. Neo4j gives a relationship exactly one type (unlike a
     * node, which can carry several labels), so this is a single value and never {@code null}.
     */
    private final String type;

    /** Id of the node this relationship starts from, matching some {@link Neo4jNode#getId()}. */
    private final String sourceId;

    /** Id of the node this relationship points to, matching some {@link Neo4jNode#getId()}. */
    private final String targetId;

    /**
     * The relationship's properties. Keys are property names as stored in Neo4j (e.g. {@code since});
     * values are the corresponding property values, already converted to plain Java types by the graph
     * reader ({@code String}, {@code Long}, {@code Double}, {@code Boolean}, ...). A key that is absent
     * means the property is not set, which is distinct from a key present with a {@code null} value.
     */
    private final Map<String, Object> properties;

    public Neo4jEdge(String id, String type, String sourceId, String targetId,
                     Map<String, Object> properties) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.properties = properties != null ? new LinkedHashMap<>(properties) : new LinkedHashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * Returns true when the property is present on this relationship. Distinguishes an absent property
     * (the operand cannot be valuated) from a present property whose value is {@code null}.
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * The value of the given property, or {@code null} if it is absent <em>or</em> present with a
     * {@code null} value. Use {@link #hasProperty(String)} to tell those two apart.
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String toString() {
        return "Neo4jEdge{" + id + ", type=" + type + ", " + sourceId + "->" + targetId
                + ", props=" + properties + "}";
    }
}
