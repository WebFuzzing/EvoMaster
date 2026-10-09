package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents the $unionWith stage.
 * It adds the documents of another collection (after its own pipeline, if it has one), like UNION ALL in SQL.
 */
public class UnionWithStage extends AggregationStage {

    public static final String NAME = "$unionWith";

    private final String collection;
    private final AggregationPipeline pipeline;

    /**
     * @param collection the other collection
     * @param pipeline   the pipeline that is applied to the other collection. It has no stages if there is none.
     */
    public UnionWithStage(String collection, AggregationPipeline pipeline) {
        this.collection = Objects.requireNonNull(collection);
        this.pipeline = Objects.requireNonNull(pipeline);
    }

    public String getCollection() {
        return collection;
    }

    public AggregationPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public String getStageName() {
        return NAME;
    }
}
