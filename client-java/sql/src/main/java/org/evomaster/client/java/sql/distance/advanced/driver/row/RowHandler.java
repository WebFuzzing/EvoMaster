package org.evomaster.client.java.sql.distance.advanced.driver.row;

import org.apache.commons.dbutils.handlers.AbstractListHandler;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class RowHandler extends AbstractListHandler<Row> {

    protected Row handleRow(ResultSet resultSet) throws SQLException {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        Row row = new Row();
        for(int i = 1; i <= resultSetMetaData.getColumnCount(); ++i) {
            String column = resultSetMetaData.getColumnName(i);
            Object value = resultSet.getObject(i);
            row.put(column, value);
        }
        return row;
    }
}
