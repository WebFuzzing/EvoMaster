package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.List;
import java.util.Objects;

/**
 * Represents a MongoDB aggregation pipeline: the stages that the documents go through, in order.
 */
public class AggregationPipeline {

    private final List<AggregationStage> stages;

    public AggregationPipeline(List<AggregationStage> stages) {
        this.stages = Objects.requireNonNull(stages);
    }

    public List<AggregationStage> getStages() {
        return stages;
    }
}
