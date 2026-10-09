package org.evomaster.client.java.controller.mongo.operations.aggregations;

/**
 * Represents a stage of a MongoDB aggregation pipeline.
 */
public abstract class AggregationStage {

    /**
     * @return the name of the stage, including the dollar sign (eg, $match)
     */
    public abstract String getStageName();
}
