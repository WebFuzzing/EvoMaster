package org.evomaster.client.java.sql.advanced.evaluation_context;

import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;

public class EvaluationContext {

    private List<TableColumnsValues> tablesColumnsValues;

    private EvaluationContext(List<TableColumnsValues> tablesColumnsValues) {
        this.tablesColumnsValues = tablesColumnsValues;
    }

    public static EvaluationContext createEvaluationContext(Row row, List<QueryTable> tables) {
        List<TableColumnsValues> columnsValues = row.entrySet().stream()
            .map(entry -> new TableColumnsValues(enrichTable(entry.getKey(), tables), entry.getValue()))
            .collect(Collectors.toList());
        return new EvaluationContext(columnsValues);
    }

    private static QueryTable enrichTable(String tableName, List<QueryTable> tables) {
        return tables.stream()
            .filter(table -> table.getName().equals(tableName))
            .findFirst()
            .orElse(createQueryTable(tableName));
    }

    public Boolean includes(QueryColumn column) {
        return tablesColumnsValues.stream()
            .anyMatch(tableColumnsValues -> tableColumnsValues.includes(column, false));
    }

    public Object getValue(QueryColumn column) {
        TableColumnsValues table;
        Optional<TableColumnsValues> exactMatchTable = tablesWithColumn(column, true).stream().findFirst();
        if(exactMatchTable.isPresent()) {
            table = exactMatchTable.get();
        } else {
            List<TableColumnsValues> candidateTables = tablesWithColumn(column, false);
            if(candidateTables.size() == 1) {
                table = candidateTables.get(0);
            } else {
                throw new RuntimeException(format("Column %s value is ambiguous in %s", column, tablesColumnsValues));
            }
        }
        return table.getValue(column);
    }

    private List<TableColumnsValues> tablesWithColumn(QueryColumn column, Boolean exactMatch) {
        return tablesColumnsValues.stream()
            .filter(tableColumnsValues -> tableColumnsValues.includes(column, exactMatch))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return tablesColumnsValues.toString();
    }
}
