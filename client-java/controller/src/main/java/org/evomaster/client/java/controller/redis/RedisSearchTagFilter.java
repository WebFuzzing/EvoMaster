package org.evomaster.client.java.controller.redis;

import java.util.Collections;
import java.util.List;

/**
 * A tag filter, e.g. {@code @street:{Main St|2nd Ave}}. Matches when the document's field
 * equals any one of the listed values.
 */
public class RedisSearchTagFilter implements RedisSearchFilter {

    private final String field;
    private final List<String> values;

    public RedisSearchTagFilter(String field, List<String> values) {
        this.field = field;
        this.values = Collections.unmodifiableList(values);
    }

    public String getField() {
        return field;
    }

    public List<String> getValues() {
        return values;
    }
}
