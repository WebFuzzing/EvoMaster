package org.evomaster.client.java.sql.internal;

public class SqlTableIdParser {

    public static SqlTableId parseFullyQualifiedTableName(String fullyQualifiedTableName) {
        if (fullyQualifiedTableName == null || fullyQualifiedTableName.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        String[] parts = fullyQualifiedTableName.split("\\.");

        switch (parts.length) {
            case 1:
                // table
                return new SqlTableId(null, null, parts[0]);

            case 2:
                // schema.table
                return new SqlTableId(null, parts[0], parts[1]);

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
