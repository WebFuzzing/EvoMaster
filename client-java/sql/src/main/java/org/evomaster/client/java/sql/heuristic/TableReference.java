package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;

class TableReference {
    private final Table table;
    private final Select select;

    private TableReference(Table table, Select select) {
        this.table = table;
        this.select = select;
    }

    public static TableReference createBaseTableReference(Table table) {
        return new TableReference(table, null);
    }

    public static TableReference createDerivedTableReference(Select select) {
        return new TableReference(null, select);
    }

    public boolean isBaseTableReference() {
        return table != null;
    }

    public boolean isDerivedTableReference() {
        return select != null;
    }

    public Table getBaseTable() {
        return table;
    }

    public Select getDerivedTableSelect() {
        return select;
    }

    @Override
    public String toString() {
        return isBaseTableReference() ? table.toString() : select.toString();
    }
}
