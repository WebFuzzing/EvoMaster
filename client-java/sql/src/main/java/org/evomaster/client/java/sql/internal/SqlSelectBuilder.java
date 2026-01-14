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

        final String fullyQualifiedTableName = tableId.buildQualifiedTableName(databaseType);
        return String.format("SELECT %s FROM %s", columnList, fullyQualifiedTableName);
    }


}
