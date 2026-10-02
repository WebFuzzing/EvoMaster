package org.evomaster.client.java.controller.redis;

/**
 * A free-text filter, e.g. {@code @title:redis} or a bare term such as {@code redis}. A null
 * {@link #getFieldName()} means the term is matched against every field of the document (the
 * grammar's field-less {@code textFilter} form) - this is the only one of the three filter types
 * where the field is allowed to be null.
 */
public class RedisSearchTextFilter implements RedisSearchFilter {

    private final String fieldName;
    private final String term;

    public RedisSearchTextFilter(String fieldName, String term) {
        if (term == null) {
            throw new IllegalArgumentException("term must not be null");
        }
        this.fieldName = fieldName;
        this.term = term;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getTerm() {
        return term;
    }
}
