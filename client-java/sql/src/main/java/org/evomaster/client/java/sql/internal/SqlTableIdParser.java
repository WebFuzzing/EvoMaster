package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

import java.util.Objects;

public class SqlTableIdParser {

    public static SqlTableId parseFullyQualifiedTableName(String fullyQualifiedTableName, DatabaseType databaseType) {
        Objects.requireNonNull(databaseType);
        Objects.requireNonNull(fullyQualifiedTableName);

        if (fullyQualifiedTableName.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        String[] parts = fullyQualifiedTableName.split("\\.");

        switch (parts.length) {
            case 1:
                // table only
                return new SqlTableId(null, null, parts[0]);

            case 2:
                if (databaseType == DatabaseType.MYSQL || databaseType == DatabaseType.MARIADB) {
                    // catalog.table
                    return new SqlTableId(parts[0], null, parts[1]);
                } else {
                    // schema.table
                    return new SqlTableId(null, parts[0], parts[1]);
                }
            case 3:
                // catalog.schema.table
                return new SqlTableId(parts[0], parts[1], parts[2]);

            default:
                throw new IllegalArgumentException(
                        "Invalid fully qualified name for a table: expected 1, 2, or 3 parts, got " + parts.length
                );
        }
    }

}
