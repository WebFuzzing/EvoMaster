package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.select.Select;

/**
 * Represents a reference to a derived table in SQL.
 * Derived tables are temporary tables created within a query.
 * A derived table is defined by a subquery in the FROM clause, or an the WHERE clause.
 */
public class SqlDerivedTable extends SqlTableReference {

    private final Select select;

    public SqlDerivedTable(Select select) {
        this.select = select;
    }

    public Select getSelect() {
        return select;
    }

}
