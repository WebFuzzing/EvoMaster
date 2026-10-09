package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.List;
import java.util.Objects;

/**
 * Represents the $group stage.
 * It groups the documents by the value of an expression, and computes accumulators for each group,
 * like GROUP BY in SQL.
 */
public class GroupStage extends AggregationStage {

    public static final String NAME = "$group";

    private final AggregationExpression id;
    private final List<GroupAccumulator> accumulators;

    public GroupStage(AggregationExpression id, List<GroupAccumulator> accumulators) {
        this.id = Objects.requireNonNull(id);
        this.accumulators = Objects.requireNonNull(accumulators);
    }

    /**
     * @return the expression that is the key of the groups (the _id field of the stage)
     */
    public AggregationExpression getId() {
        return id;
    }

    public List<GroupAccumulator> getAccumulators() {
        return accumulators;
    }

    @Override
    public String getStageName() {
        return NAME;
    }
}
