package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.List;
import java.util.Objects;

/**
 * Represents the $sort stage.
 * It sorts the documents by some fields, like ORDER BY in SQL.
 */
public class SortStage extends AggregationStage {

    public static final String NAME = "$sort";

    private final List<SortField> fields;

    public SortStage(List<SortField> fields) {
        this.fields = Objects.requireNonNull(fields);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("A $sort stage needs at least one field");
        }
    }

    public List<SortField> getFields() {
        return fields;
    }

    @Override
    public String getStageName() {
        return NAME;
    }
}
