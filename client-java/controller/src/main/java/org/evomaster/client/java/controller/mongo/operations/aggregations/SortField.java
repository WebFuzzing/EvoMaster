package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents a field of a $sort stage, and the direction it is sorted in.
 */
public class SortField {

    private final String fieldPath;
    private final boolean ascending;

    public SortField(String fieldPath, boolean ascending) {
        this.fieldPath = Objects.requireNonNull(fieldPath);
        this.ascending = ascending;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public boolean isAscending() {
        return ascending;
    }
}
