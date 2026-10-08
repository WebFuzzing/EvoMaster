package org.evomaster.client.java.controller.cassandra.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single row of a Cassandra table, keyed by column name.
 * Column-name lookups are case-insensitive and tolerate CQL-quoted identifiers
 * (e.g. {@code "myCol"}), matching CQL's own identifier normalisation rules.
 */
public class CassandraRow {

    private static final String DOUBLE_QUOTE = "\"";

    private final Map<String, Object> columns;

    public CassandraRow(Map<String, Object> columns) {
        this.columns = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : columns.entrySet()) {
            this.columns.put(normaliseColumnName(e.getKey()), e.getValue());
        }
    }

    /**
     * @param rawColumnName the column name, possibly CQL-quoted
     * @return the column's value, or {@code null} if the column is absent from this
     *         row or its value is {@code null}
     */
    public Object getValue(String rawColumnName) {
        return columns.get(normaliseColumnName(rawColumnName));
    }

    /**
     * Normalises a column name for lookup: strips surrounding double quotes (CQL's
     * case-sensitive quoted-identifier syntax) and lower-cases the result, since
     * unquoted CQL identifiers are case-insensitive.
     *
     * @param rawColumnName the column name as it appears in the CQL query, possibly quoted
     * @return the normalized column name, or {@code null} if {@code rawColumnName} is {@code null}
     */
    private static String normaliseColumnName(String rawColumnName) {
        if (rawColumnName == null) return null;
        if (rawColumnName.startsWith(DOUBLE_QUOTE) && rawColumnName.endsWith(DOUBLE_QUOTE)) {
            return rawColumnName.substring(1, rawColumnName.length() - 1).toLowerCase();
        }
        return rawColumnName.toLowerCase();
    }
}