package org.evomaster.client.java.sql.advanced.select_query;

import net.sf.jsqlparser.schema.Column;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;

public class QueryColumn {

    private String tableName;
    private String name;

    private QueryColumn(String tableName, String name) {
        this.tableName = tableName;
        this.name = name;
    }

    public static QueryColumn createQueryColumn(String tableName, String name) {
        return new QueryColumn(nonNull(tableName) ? tableName.toLowerCase() : null, name.toLowerCase());
    }

    public static QueryColumn createQueryColumn(Column column){
        return createQueryColumn(nonNull(column.getTable()) ? column.getTable().getName() : null, column.getColumnName());
    }

    public static QueryColumn createQueryColumn(String name){
        return createQueryColumn(null, name);
    }

    public Boolean hasTable(){
        return nonNull(tableName);
    }

    public String getTableName() {
        return tableName;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return hasTable() ? tableName + "." + name : name;
    }
}
