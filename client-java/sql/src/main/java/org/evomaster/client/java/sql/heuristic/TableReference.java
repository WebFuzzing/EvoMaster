package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.select.Select;

class TableReference {
    private final String baseTableName;
    private final Select derivedTableParenthesedSelect;

    private TableReference(String baseTableName, Select derivedTableParenthesedSelect) {
        this.baseTableName = baseTableName;
        this.derivedTableParenthesedSelect = derivedTableParenthesedSelect;
    }

    public static TableReference createBaseTableReference(String name) {
        return new TableReference(name, null);
    }

    public static TableReference createDerivedTableReference(Select select) {
        return new TableReference(null, select);
    }

    public boolean isBaseTableReference() {
        return baseTableName != null;
    }

    public boolean isDerivedTableReference() {
        return derivedTableParenthesedSelect != null;
    }

    public String getBaseTableName() {
        return baseTableName;
    }

    public Select getDerivedTableSelect() {
        return derivedTableParenthesedSelect;
    }

    @Override
    public String toString() {
        return baseTableName;
    }
}
