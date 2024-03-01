package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.operations.QueryResultDto;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * The results of a SQL Select query, in a easy to parse/manipulate data structure
 * compared to java.sql.ResultSet.
 */
public class QueryResult {

    private final List<VariableDescriptor> variableDescriptors = new ArrayList<>();
    private final List<DataRow> rows = new ArrayList<>();

    /**
     * WARNING: Constructor only needed for testing
     *
     * @param columnNames name of columns
     * @param tableName name of table the columns belongs to
     */
    public QueryResult(List<String> columnNames, String tableName) {
        Objects.requireNonNull(columnNames);
        for (String c : columnNames) {
            variableDescriptors.add(new VariableDescriptor(c, null, tableName));
        }
    }


    private String getColumnName(ResultSetMetaData md, int index) throws Exception{
        /*
            Unfortunately, in Postgres, calling getColumnName does NOT return the column
            name, but rather its alias (if any).
            Therefore, we need to use reflection to check if we are dealing with a Postgres
            driver, and call its non-standard methods to retrieve the actual column name :(
         */
        if(Arrays.stream(md.getClass().getInterfaces()).anyMatch(i ->
                i.getSimpleName().equals("PGResultSetMetaData"))){

                Method m = md.getClass().getDeclaredMethod("getBaseColumnName", Integer.TYPE);
                return (String) m.invoke(md, index);
        }

        return  md.getColumnName(index);
    }

    public QueryResult(ResultSet resultSet) {

        if (resultSet == null) {
            return;
        }

        try {
            ResultSetMetaData md = resultSet.getMetaData();

            for (int i = 0; i < md.getColumnCount(); i++) {
                int index = i + 1;
                VariableDescriptor desc = new VariableDescriptor(
                        getColumnName(md, index),
                        md.getColumnLabel(index),
                        md.getTableName(index)
                );
                variableDescriptors.add(desc);
            }

            while (resultSet.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 0; i < md.getColumnCount(); i++) {
                    Object value = resultSet.getObject(i + 1);
                    row.add(value);
                }
                rows.add(new DataRow(variableDescriptors, row));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addRow(DataRow row) {
        if (!sameVariableNames(row)) {
            throw new IllegalArgumentException("Variable name mismatch");
        }
        rows.add(row);
    }

    public boolean sameVariableNames(DataRow row) {
        if (variableDescriptors.size() != row.getVariableDescriptors().size()) {
            return false;
        }
        for (int i = 0; i < variableDescriptors.size(); i++) {
            VariableDescriptor a = variableDescriptors.get(i);
            VariableDescriptor b = row.getVariableDescriptors().get(i);
            if (!a.equals(b)) {
                return false;
            }
        }

        return true;
    }

    public List<DataRow> seeRows() {
        return rows;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public int size(){
        return rows.size();
    }

    @Override
    public String toString() {

        if (variableDescriptors.isEmpty()) {
            return "EMPTY";
        }

        String header =  variableDescriptors.stream()
                .map(d -> d.toString())
                .collect(Collectors.joining(","));

        return header + rows.stream()
                .map(r -> "\n" + r.getAsLine())
                .collect(Collectors.joining());
    }

    public QueryResultDto toDto(){

        QueryResultDto dto = new QueryResultDto();
        dto.rows = rows.stream().map(r -> r.toDto()).collect(Collectors.toList());

        return dto;
    }
}
