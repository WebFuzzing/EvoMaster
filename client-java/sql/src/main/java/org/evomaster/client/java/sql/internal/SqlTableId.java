package org.evomaster.client.java.sql.internal;

import java.util.Objects;

/**
 * Represents an identifier for a SQL table, including an optional catalog and schema.
 * This class is immutable and supports case-insensitive comparisons for schema and table IDs.
 */
public class SqlTableId {

    private final String catalogName;
    private final String schemaName;
    private final String tableName;

    /**
     *
     * @param catalogName
     * @param schemaName
     * @param tableName
     */
    public SqlTableId(String catalogName, String schemaName, String tableName) {
        Objects.requireNonNull(tableName);
        if (tableName.contains(".")) {
            throw new IllegalArgumentException("Table name cannot contain dots: " + tableName);
        }
        this.catalogName = catalogName ==null ? null : catalogName.toLowerCase();
        this.schemaName = schemaName == null ? null : schemaName.toLowerCase();
        this.tableName = tableName.toLowerCase();
    }

    public SqlTableId(String tableName) {
        this(null, null, tableName);
    }

    public String getTableName() {
        return tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getCatalogName() {
        return catalogName;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SqlTableId)) return false;
        SqlTableId that = (SqlTableId) o;
        return Objects.equals(getCatalogName(), that.getCatalogName())
                && Objects.equals(getSchemaName(), that.getSchemaName())
                && Objects.equals(getTableName(), that.getTableName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCatalogName(), getSchemaName(), getTableName());
    }

    @Override
    public String toString() {
        return String.valueOf(catalogName) + '.' +
                String.valueOf(schemaName) + '.' + tableName;
    }

}
