package org.evomaster.client.java.sql.advanced.driver;

import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.sql.SchemaExtractor;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Schema {

    private Map<String, List<String>> tables;

    private Schema(){}

    public Schema(Map<String, List<String>> tables) {
        this.tables = tables;
    }

    public static Schema createSchema(Connection connection){
        try {
            return createSchema(SchemaExtractor.extract(connection));
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while extracting schema", e);
        }
    }

    public static Schema createSchema(DbSchemaDto dbSchemaDto) {
        Map<String, List<String>> tables =
            dbSchemaDto.tables.stream().collect(Collectors.toMap(
                table -> table.name.toLowerCase(),
                table -> table.columns.stream()
                        .map(column -> column.name.toLowerCase())
                        .collect(Collectors.toList())));
        return new Schema(tables);
    }

    public Map<String, List<String>> getTables() {
        return tables;
    }

    @Override
    public String toString() {
        return tables.entrySet()
            .stream()
            .map(e -> e.getKey() + " = " + e.getValue())
            .collect(Collectors.joining("\n"));
    }
}
