package org.evomaster.client.java.sql.internal;


import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ForeignKeyDto;

import java.util.*;

import static org.evomaster.client.java.sql.DbInfoExtractor.getTable;


public class SqlDbHarvester {


    /**
     *  Sorts a list of tables from the schema in topological order based on their foreign key dependencies.
     *  This ensures that parent tables (referenced by foreign keys) are ordered before their child tables.
     * @param schema the sql schema for databases
     * @return a sequence of tables
     */
    public static List<TableDto> sortTablesByDependency(DbInfoDto schema) {

        Objects.requireNonNull(schema);
        Objects.requireNonNull(schema.tables);

        List<TableDto> tables = schema.tables;

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, TableDto> tableMap = new HashMap<>();

        // init
        for (TableDto table : tables) {
            String key = getTableKey(table);
            inDegree.put(key, 0);
            graph.put(key, new ArrayList<>());
            tableMap.put(key, table);
        }

        // construct dependency graph based on schema
        for (TableDto childTable : tables) {
            String childKey = getTableKey(childTable);
            for (ForeignKeyDto fk : childTable.foreignKeys) {
                TableDto parentTable = getTable(schema, fk.targetTable);
                String parentKey = getTableKey(parentTable);
                if (tableMap.containsKey(parentKey)) {
                    graph.get(parentKey).add(childKey);
                    inDegree.put(childKey, inDegree.get(childKey) + 1);
                }
            }
        }

        // Kahn
        Queue<String> queue = new LinkedList<>();
        for (String tableKey : inDegree.keySet()) {
            if (inDegree.get(tableKey) == 0) {
                queue.add(tableKey);
            }
        }

        List<TableDto> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String currentKey = queue.poll();
            result.add(tableMap.get(currentKey));

            for (String neighborKey : graph.get(currentKey)) {
                inDegree.put(neighborKey, inDegree.get(neighborKey) - 1);
                if (inDegree.get(neighborKey) == 0) {
                    queue.add(neighborKey);
                }
            }
        }

        if (result.size() != tables.size()) {
            throw new RuntimeException("Unable to perform topological sort: circular foreign key dependency exists between tables");
        }

        return result;
    }


    private static String getTableKey(TableDto table) {
        return getTableKey(table.catalog, table.schema, table.name);
    }

    private static String getTableKey(String catalog, String schema, String tableName) {
        StringBuilder key = new StringBuilder();
        if (catalog != null && !catalog.isEmpty()) {
            key.append(catalog).append(".");
        }
        if (schema != null && !schema.isEmpty()) {
            key.append(schema).append(".");
        }
        key.append(tableName);
        return key.toString();
    }
}
