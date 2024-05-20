package org.evomaster.client.java.sql.advanced.select_query;

import net.sf.jsqlparser.schema.Column;

import static java.util.Objects.nonNull;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;

public class QueryColumn {

    private QueryTable table;
    private String name;

    private QueryColumn(QueryTable table, String name) {
        this.table = table;
        this.name = name;
    }

    public static QueryColumn createQueryColumn(QueryTable table, String name) {
        return new QueryColumn(table, name.toLowerCase());
    }

    public static QueryColumn createQueryColumn(Column column){
        return createQueryColumn(nonNull(column.getTable()) ? createQueryTable(column.getTable()) : null, column.getColumnName());
    }

    public static QueryColumn createQueryColumn(String name){
        return createQueryColumn(null, name);
    }

    public Boolean hasTable(){
        return nonNull(table);
    }

    public QueryTable getTable() {
        return table;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return hasTable() ? "(" + table + ")." + name : name;
    }
}
