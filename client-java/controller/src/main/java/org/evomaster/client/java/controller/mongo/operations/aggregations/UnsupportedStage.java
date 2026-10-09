package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents a stage that cannot be evaluated: either its name is not one of the supported stages, or it
 * uses something of a supported one that is not supported.
 */
public class UnsupportedStage extends AggregationStage {

    private final String name;
    private final String reason;

    public UnsupportedStage(String name, String reason) {
        this.name = Objects.requireNonNull(name);
        this.reason = Objects.requireNonNull(reason);
    }

    /**
     * @return why it is not supported
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String getStageName() {
        return name;
    }
}
