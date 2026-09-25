package org.evomaster.client.java.controller.api.dto.database.execution;

/**
 * RediSearch field types supported by data generation.
 * Used both for the fields of an index schema and for the filters of a query.
 */
public enum RedisSearchFieldType {

    TEXT,

    TAG,

    NUMERIC,

    /**
     * Any other RediSearch type, e.g. GEO or VECTOR. Only used for schema fields, never for filters.
     */
    OTHER;

    /**
     * @param type name as returned by FT.INFO, e.g. "TEXT". Case is ignored.
     * @return the matching constant, or {@link #OTHER} if there is none
     */
    public static RedisSearchFieldType fromRediSearch(String type) {
        for (RedisSearchFieldType candidate : values()) {
            if (candidate != OTHER && candidate.name().equalsIgnoreCase(type)) {
                return candidate;
            }
        }
        return OTHER;
    }
}
