package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.List;
import java.util.Objects;

/**
 * Represents the $project stage.
 * It selects, excludes or computes the fields of the documents, like SELECT in SQL.
 */
public class ProjectStage extends AggregationStage {

    public static final String NAME = "$project";
    public static final String ID_FIELD = "_id";

    private final List<ProjectField> fields;

    public ProjectStage(List<ProjectField> fields) {
        this.fields = Objects.requireNonNull(fields);
    }

    public List<ProjectField> getFields() {
        return fields;
    }

    /**
     * @return true if it removes the given fields and keeps all the others. Otherwise, it keeps
     * only the given ones. The _id field does not count, as it is kept unless it is explicitly excluded.
     */
    public boolean isExclusion() {
        return fields.stream().anyMatch(f -> f.getType() == ProjectFieldType.EXCLUDE && !f.getFieldPath().equals(ID_FIELD));
    }

    /**
     * @return true if it keeps the _id field, which is the default
     */
    public boolean isIdIncluded() {
        return fields.stream().noneMatch(f -> f.getType() == ProjectFieldType.EXCLUDE && f.getFieldPath().equals(ID_FIELD));
    }

    @Override
    public String getStageName() {
        return NAME;
    }
}
