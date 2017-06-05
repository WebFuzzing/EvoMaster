package org.evomaster.clientJava.controller.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class QueryResult {

    private final List<String> variableNames = new ArrayList<>();
    private final List<DataRow> rows = new ArrayList<>();

    public QueryResult(List<String> names) {
        Objects.requireNonNull(names);
        variableNames.addAll(names);
    }

    public QueryResult(ResultSet resultSet) {

        if (resultSet == null) {
            return;
        }

        try {
            ResultSetMetaData md = resultSet.getMetaData();

            for (int i = 0; i < md.getColumnCount(); i++) {
                variableNames.add(md.getColumnLabel(i + 1));
            }

            while (resultSet.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 0; i < md.getColumnCount(); i++) {
                    Object value = resultSet.getObject(i + 1);
                    row.add(value);
                }
                rows.add(new DataRow(variableNames, row));
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
        if (variableNames.size() != row.getVariableNames().size()) {
            return false;
        }
        for (int i = 0; i < variableNames.size(); i++) {
            String a = variableNames.get(i);
            String b = row.getVariableNames().get(i);
            if (!a.equalsIgnoreCase(b)) {
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

    @Override
    public String toString() {

        if (variableNames.isEmpty()) {
            return "EMPTY";
        }

        return String.join(",", variableNames) +
                String.join("",
                        rows.stream().map(r -> "\n" + r.getAsLine()).collect(Collectors.toList())
                );
    }
}
