package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.select.Select;

public class SqlDerivedTableReference extends SqlTableReference{

    private final Select select;

    public SqlDerivedTableReference(Select select) {
        this.select = select;
    }

    public Select getSelect() {
        return select;
    }

}
