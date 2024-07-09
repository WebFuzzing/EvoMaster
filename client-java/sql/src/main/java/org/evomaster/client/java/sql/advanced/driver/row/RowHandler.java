package org.evomaster.client.java.sql.advanced.driver.row;

import org.apache.commons.dbutils.handlers.AbstractListHandler;
import org.evomaster.client.java.sql.advanced.select_query.SelectQuery;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class RowHandler extends AbstractListHandler<Row> {

    private RowHandler(){}

    public static RowHandler createRowHandler(){
        return new RowHandler();
    }

    protected Row handleRow(ResultSet resultSet) throws SQLException {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        Row row = new Row();
        for(int i = 1; i <= resultSetMetaData.getColumnCount(); ++i) {
            String tableName = resultSetMetaData.getTableName(i).toLowerCase();
            String columnName = resultSetMetaData.getColumnName(i).toLowerCase();
            Object value = resultSet.getObject(i);
            if(row.containsKey(tableName)) {
                Map<String, Object> columns = row.get(tableName);
                columns.put(columnName, value);
            } else {
                Map<String, Object> columns = new HashMap<>();
                columns.put(columnName, value);
                row.put(tableName, columns);
            }
        }
        return row;
    }
}
