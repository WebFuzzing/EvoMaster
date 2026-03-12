package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

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
     * Creates a new table identifier.
     * Catalog and schema names may be null.
     * Catalog, schema and table names must not contain dots.
     * Catalog and schema names are case-insensitive.
     *
     * @param catalogName
     * @param schemaName
     * @param tableName
     */
    public SqlTableId(String catalogName, String schemaName, String tableName) {
        Objects.requireNonNull(tableName);

        if (catalogName!=null && catalogName.contains(".")) {
            throw new IllegalArgumentException("Catalog name cannot contain dots: " + catalogName);
        }
        if (schemaName!=null && schemaName.contains(".")) {
            throw new IllegalArgumentException("Schema name cannot contain dots: " + schemaName);
        }
        if (tableName.contains(".")) {
            throw new IllegalArgumentException("Table name cannot contain dots: " + tableName);
        }
        this.catalogName = catalogName ==null ? null : catalogName.toLowerCase();
        this.schemaName = schemaName == null ? null : schemaName.toLowerCase();
        this.tableName = tableName.toLowerCase();
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

    /**
     * Returns the fully qualified table name, including catalog and schema if present.
     *
     * @param databaseType
     * @return
     */
    public String buildQualifiedTableName(DatabaseType databaseType) {
        StringBuilder sb = new StringBuilder();

        if (databaseType == DatabaseType.MYSQL) {
            // MySQL: schema is ignored unless catalog is null
            if (getCatalogName() != null) {
                sb.append(getCatalogName()).append(".");
            } else if (getSchemaName() != null) {
                sb.append(getSchemaName()).append(".");
            }
        } else {
            // Standard behavior: include both if present
            if (getCatalogName() != null) {
                sb.append(getCatalogName()).append(".");
            }
            if (getSchemaName() != null) {
                sb.append(getSchemaName()).append(".");
            }
        }

        sb.append(getTableName());
        return sb.toString();
    }


}
