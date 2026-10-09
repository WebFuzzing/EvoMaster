package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents the $lookup stage, in its form with a local and a foreign field.
 * It adds to each document an array with the documents of another collection that have the same value
 * in a field, like a LEFT OUTER JOIN in SQL.
 */
public class LookupStage extends AggregationStage {

    public static final String NAME = "$lookup";

    private final String from;
    private final String localField;
    private final String foreignField;
    private final String as;

    /**
     * @param from         the other collection
     * @param localField   the field of the documents that are looked up
     * @param foreignField the field of the documents in the other collection
     * @param as           the field where the matching documents are added
     */
    public LookupStage(String from, String localField, String foreignField, String as) {
        this.from = Objects.requireNonNull(from);
        this.localField = Objects.requireNonNull(localField);
        this.foreignField = Objects.requireNonNull(foreignField);
        this.as = Objects.requireNonNull(as);
    }

    public String getFrom() {
        return from;
    }

    public String getLocalField() {
        return localField;
    }

    public String getForeignField() {
        return foreignField;
    }

    public String getAs() {
        return as;
    }

    @Override
    public String getStageName() {
        return NAME;
    }
}
