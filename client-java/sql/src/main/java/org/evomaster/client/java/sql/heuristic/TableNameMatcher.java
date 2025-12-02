package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.controller.api.dto.database.schema.TableIdDto;

import java.util.Objects;

public class TableNameMatcher {

    private final TableIdDto tableIdDto;

    public TableNameMatcher(TableIdDto tableIdDto) {
        this.tableIdDto = tableIdDto;
    }

    public boolean matches(String schemaName, String tableName) {
        return (schemaName == null || matchSchemaName(schemaName, tableIdDto.schema))
                && matchTableName(tableName, tableIdDto.name);
    }

    private static boolean matchSchemaName(String schemaName1, String schemaName2) {
        Objects.requireNonNull(schemaName1);
        return equalNames(schemaName1, schemaName2);
    }

    private static boolean equalNames(String l, String r) {
        Objects.requireNonNull(l);
        return l.equalsIgnoreCase(r);
    }

    private static boolean matchTableName(String tableName1, String tableName2) {
        return equalNames(tableName1, tableName2);
    }

}
