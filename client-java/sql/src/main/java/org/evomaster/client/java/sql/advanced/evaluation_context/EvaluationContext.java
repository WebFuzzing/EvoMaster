package org.evomaster.client.java.sql.advanced.evaluation_context;

import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.DEFAULT_TABLE;

public class EvaluationContext {

    private List<TableColumnsValues> tablesColumnsValues;

    private EvaluationContext(List<TableColumnsValues> tablesColumnsValues) {
        this.tablesColumnsValues = tablesColumnsValues;
    }

    public static EvaluationContext createEvaluationContext(List<QueryTable> tables, Row row) {
        List<TableColumnsValues> columnsValues =
            Stream.concat(Stream.of(tables).flatMap(List::stream), Stream.of(DEFAULT_TABLE))
                .filter(table -> row.containsKey(table.getName()))
                .map(table -> new TableColumnsValues(table, row.get(table.getName())))
                .collect(Collectors.toList());
        return new EvaluationContext(columnsValues);
    }

    public Boolean includes(QueryColumn column) {
        return tablesColumnsValues.stream().anyMatch(tableColumnsValues -> tableColumnsValues.includes(column));
    }

    public Object getValue(QueryColumn column) {
        return tablesColumnsValues.stream()
            .filter(tableColumnsValues -> tableColumnsValues.includes(column))
            .map(tableColumnsValues -> tableColumnsValues.getValue(column))
            .map(Optional::ofNullable)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                format("Column %s value must be present in %s", column, tablesColumnsValues)))
            .orElse(null);
    }

    @Override
    public String toString() {
        return tablesColumnsValues.toString();
    }
}
