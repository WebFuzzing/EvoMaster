package org.evomaster.client.java.controller.neo4j.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A relationship of a captured Neo4j graph: a stable id, a single type, the ids of its two endpoint
 * nodes (source and target), and its property map. A relationship in Neo4j is always stored with a
 * direction (source → target).
 */
public class Neo4jRelationship {

    private final String id;
    private final String type;
    private final String sourceId;
    private final String targetId;
    private final Map<String, Object> properties;

    public Neo4jRelationship(String id, String type, String sourceId, String targetId,
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

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String toString() {
        return "Neo4jRelationship{" + id + ", type=" + type + ", " + sourceId + "->" + targetId
                + ", props=" + properties + "}";
    }
}
