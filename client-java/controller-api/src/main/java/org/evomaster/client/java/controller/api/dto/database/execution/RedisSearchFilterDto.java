package org.evomaster.client.java.controller.api.dto.database.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * A single filter parsed out of a failed FT.SEARCH/FT.AGGREGATE query, plain enough to be
 * interpreted from the core module without depending on the controller's query parser classes.
 */
public class RedisSearchFilterDto {

    /**
     * The kind of filter: {@link RedisSearchFieldType#TAG}, {@link RedisSearchFieldType#NUMERIC}
     * or {@link RedisSearchFieldType#TEXT}. It determines which of the fields below are set.
     */
    public RedisSearchFieldType type;

    /**
     * The hash field this filter applies to. Null for a TEXT filter with no field (matches any
     * field of the document).
     */
    public String field;

    /**
     * TAG only: the list of values, any one of which satisfies the filter.
     */
    public List<String> values;

    /**
     * NUMERIC only: the inclusive lower bound.
     */
    public Double min;

    /**
     * NUMERIC only: the inclusive upper bound.
     */
    public Double max;

    /**
     * TEXT only: the search term, possibly ending with "*" for a prefix match.
     */
    public String term;

    public RedisSearchFilterDto() {}

    public static RedisSearchFilterDto tag(String field, List<String> values) {
        RedisSearchFilterDto dto = new RedisSearchFilterDto();
        dto.type = RedisSearchFieldType.TAG;
        dto.field = field;
        dto.values = new ArrayList<>(values);
        return dto;
    }

    public static RedisSearchFilterDto numeric(String field, double min, double max) {
        RedisSearchFilterDto dto = new RedisSearchFilterDto();
        dto.type = RedisSearchFieldType.NUMERIC;
        dto.field = field;
        dto.min = min;
        dto.max = max;
        return dto;
    }

    public static RedisSearchFilterDto text(String field, String term) {
        RedisSearchFilterDto dto = new RedisSearchFilterDto();
        dto.type = RedisSearchFieldType.TEXT;
        dto.field = field;
        dto.term = term;
        return dto;
    }
}
