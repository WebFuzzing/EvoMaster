package org.evomaster.clientJava.controller.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class QueryResult {

    private final List<VariableDescriptor> variableDescriptors = new ArrayList<>();
    private final List<DataRow> rows = new ArrayList<>();

    public QueryResult(List<String> names) {
        Objects.requireNonNull(names);
        for (String n : names) {
            variableDescriptors.add(new VariableDescriptor(n));
        }
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
                        md.getColumnName(index),
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
}
