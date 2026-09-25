package org.evomaster.client.java.controller.redis;

import java.util.Collections;
import java.util.List;

/**
 * A tag filter, e.g. {@code @street:{Main St|2nd Ave}}. Matches when the document's field
 * equals any one of the listed values.
 */
public class RedisSearchTagFilter implements RedisSearchFilter {

    private final String fieldName;
    private final List<String> values;

    public RedisSearchTagFilter(String fieldName, List<String> values) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must not be null");
        }
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        this.fieldName = fieldName;
        this.values = Collections.unmodifiableList(values);
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<String> getValues() {
        return values;
    }
}
