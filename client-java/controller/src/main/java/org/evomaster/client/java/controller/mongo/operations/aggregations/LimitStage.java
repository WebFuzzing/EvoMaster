package org.evomaster.client.java.controller.mongo.operations.aggregations;

/**
 * Represents the $limit stage.
 * It keeps the first documents, like LIMIT in SQL.
 */
public class LimitStage extends AggregationStage {

    public static final String NAME = "$limit";

    private final long limit;

    public LimitStage(long limit) {
        this.limit = limit;
    }

    public long getLimit() {
        return limit;
    }

    @Override
    public String getStageName() {
        return NAME;
    }
}
