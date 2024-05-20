package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.expressions;

import net.sf.jsqlparser.expression.AnyType;
import net.sf.jsqlparser.statement.select.Select;

public class SelectComparisonExpression {

    private Select select;
    private AnyType anyType;

    public SelectComparisonExpression(Select select, AnyType anyType) {
        this.select = select;
        this.anyType = anyType;
    }

    public Select getSelect() {
        return select;
    }

    public AnyType getAnyType() {
        return anyType;
    }
}
