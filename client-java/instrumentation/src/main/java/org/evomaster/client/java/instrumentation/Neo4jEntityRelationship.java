package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Objects;

/**
 * A relationship declared in a Neo4j entity class, from the entity to another entity class.
 */
public class Neo4jEntityRelationship implements Serializable {

    /** Relationship type in the graph. */
    private final String type;

    /** Fully qualified name of the entity class at the other end. */
    private final String targetClassName;

    /** Whether the relationship goes from this entity to the target (outgoing) or the other way round. */
    private final boolean outgoing;

    /**
     * @param type relationship type in the graph
     * @param targetClassName class of the entity at the other end
     * @param outgoing whether this entity is the start node
     */
    public Neo4jEntityRelationship(String type, String targetClassName, boolean outgoing) {
        this.type = Objects.requireNonNull(type);
        this.targetClassName = Objects.requireNonNull(targetClassName);
        this.outgoing = outgoing;
    }

    public String getType() {
        return type;
    }

    public String getTargetClassName() {
        return targetClassName;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    @Override
    public String toString() {
        return (outgoing ? "-[:" + type + "]->" : "<-[:" + type + "]-") + targetClassName;
    }
}
