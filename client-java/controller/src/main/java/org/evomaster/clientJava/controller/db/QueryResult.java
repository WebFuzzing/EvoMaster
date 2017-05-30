package org.evomaster.clientJava.controller.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QueryResult {

    private final String[] columnNames;
    private final List<String[]> raws;

    public QueryResult(ResultSet resultSet) {

        raws = new ArrayList<>();

        if(resultSet == null){
            columnNames = new String[0];
            return;
        }

        try {
            ResultSetMetaData md = resultSet.getMetaData();
            columnNames = new String[md.getColumnCount()];

            for (int i = 0; i < columnNames.length; i++) {
                columnNames[i] = md.getColumnLabel(i + 1);
            }

            while (resultSet.next()) {
                String[] row = new String[columnNames.length];
                for (int i = 0; i < row.length; i++) {
                    String value = resultSet.getString(i + 1);
                    row[i] = value;
                }
                raws.add(row);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {

        if(columnNames.length == 0){
            return "EMPTY";
        }

        return String.join(",", columnNames) +
                String.join("",
                        raws.stream().map(a -> "\n" + String.join(",", a)).collect(Collectors.toList())
                );
    }
}
