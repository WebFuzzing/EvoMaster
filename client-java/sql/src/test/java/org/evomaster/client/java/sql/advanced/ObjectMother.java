package org.evomaster.client.java.sql.advanced;

import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext.createEvaluationContext;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;

public class ObjectMother {

    public static Row createRow(String tableName1, Map<String, Object> columns) {
        Row row = new Row();
        row.put(tableName1, columns);
        return row;
    }

    public static Row createSimpleRow(String tableName, String columnName, Object columnValue) {
        Map<String, Object> columns = new HashMap<>();
        columns.put(columnName, columnValue);
        return createRow(tableName, columns);
    }

    public static EvaluationContext createSimpleEvaluationContext(String tableName, String columnName, Object columnValue) {
        return createEvaluationContext(createSimpleRow(tableName, columnName, columnValue), singletonList(createQueryTable(tableName)));
    }
}
