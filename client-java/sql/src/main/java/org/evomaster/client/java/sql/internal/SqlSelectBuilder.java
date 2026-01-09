package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

import java.util.List;
import java.util.stream.Collectors;

public class SqlSelectBuilder {

    public static String buildSelect(
            DatabaseType databaseType,
            SqlTableId tableId,
            List<SqlColumnId> columnIds
    ) {
        if (columnIds == null || columnIds.isEmpty()) {
            throw new IllegalArgumentException("Column list cannot be empty");
        }

        String columnList = columnIds.stream()
                .map(SqlColumnId::getColumnId)
                .collect(Collectors.joining(", "));

        return String.format("SELECT %s FROM %s", columnList, buildQualifiedTableName(databaseType, tableId));
    }

    private static String buildQualifiedTableName(DatabaseType databaseType, SqlTableId tableId) {
        StringBuilder sb = new StringBuilder();

        if (databaseType == DatabaseType.MYSQL) {
            // MySQL: schema is ignored unless catalog is null
            if (tableId.getCatalogName() != null) {
                sb.append(tableId.getCatalogName()).append(".");
            } else if (tableId.getSchemaName() != null) {
                sb.append(tableId.getSchemaName()).append(".");
            }
        } else {
            // Standard behavior: include both if present
            if (tableId.getCatalogName() != null) {
                sb.append(tableId.getCatalogName()).append(".");
            }
            if (tableId.getSchemaName() != null) {
                sb.append(tableId.getSchemaName()).append(".");
            }
        }

        sb.append(tableId.getTableName());
        return sb.toString();
    }

}
