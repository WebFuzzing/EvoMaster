package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.expressions;

import net.sf.jsqlparser.statement.select.Select;

public class SelectExpression {

    private Select select;

    public SelectExpression(Select select) {
        this.select = select;
    }

    public Select getSelect() {
        return select;
    }
}
