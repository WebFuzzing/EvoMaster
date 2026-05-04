package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents Range operation.
 * Searches for a range of values in a field using operators like gte, gt, lte, lt.
 * <p>
 * <a href="https://docs.opensearch.org/latest/query-dsl/term/range/">OpenSearch Range Operation</a>
 */
public class RangeOperation extends QueryOperation {
    private final String fieldName;
    private final Object gte; // Greater than or equal to
    private final Object gt;  // Greater than
    private final Object lte; // Less than or equal to
    private final Object lt;  // Less than
    private final String format;
    private final String relation;
    private final Float boost;
    private final String timeZone;

    public RangeOperation(String fieldName, Object gte, Object gt, Object lte, Object lt) {
        this(fieldName, gte, gt, lte, lt, null, null, null, null);
    }

    public RangeOperation(String fieldName, Object gte, Object gt, Object lte, Object lt, 
                         String format, String relation, Float boost, String timeZone) {
        this.fieldName = fieldName;
        this.gte = gte;
        this.gt = gt;
        this.lte = lte;
        this.lt = lt;
        this.format = format;
        this.relation = relation;
        this.boost = boost;
        this.timeZone = timeZone;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getGte() {
        return gte;
    }

    public Object getGt() {
        return gt;
    }

    public Object getLte() {
        return lte;
    }

    public Object getLt() {
        return lt;
    }

    public String getFormat() {
        return format;
    }

    public String getRelation() {
        return relation;
    }

    public Float getBoost() {
        return boost;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
