package org.evomaster.client.java.controller.redis;

/**
 * A free-text filter, e.g. {@code @title:redis} or a bare term such as {@code redis}. A null
 * {@link #getField()} means the term is matched against every field of the document (the
 * grammar's field-less {@code textFilter} form).
 */
public class RedisSearchTextFilter implements RedisSearchFilter {

    private final String field;
    private final String term;

    public RedisSearchTextFilter(String field, String term) {
        this.field = field;
        this.term = term;
    }

    public String getField() {
        return field;
    }

    public String getTerm() {
        return term;
    }
}
