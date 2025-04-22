package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.List;

public class SelectTransformer {



    /**
     * The constraints in the WHERE clause might reference
     * fields that are not retrieved in the SELECT.
     * Therefore, we need to add them, otherwise it
     * would not be possible to calculate any heuristics
     *
     * @param select the string containing the SQL SELECT command
     * @return  the modified SQL SELECT
     */
    public static String addFieldsToSelect(String select) {

        Select stmt = asSelectStatement(select);

        PlainSelect plainSelect = stmt.getPlainSelect();

        Expression where = plainSelect.getWhere();
        if (where == null) {
            //nothing to do
            return select;
        }

        List<SelectItem<?>> fields = plainSelect.getSelectItems();

        boolean allColumns = fields.stream().anyMatch(f -> f.getExpression() instanceof AllColumns
                || f.getExpression() instanceof AllTableColumns);

        if(! allColumns) {
            where.accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {

                    String target = column.toString();

                    boolean found = false;
                    for (SelectItem<?> si : fields) {
                        String exp = si.getExpression().toString();
                        if (target.equals(exp)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        SelectItem<Expression> item = new SelectItem<>();
                        item.setExpression(column);
                        fields.add(item);
                    }
                }
            });
        }

        return stmt.toString();
    }

    /**
     * For example, when we have "select count(*)" we are not interested
     * in the count, but the actual involved fields, so we want to
     * transform it into "select *" by removing the count() operation.
     *
     * @param select SQL command to transform
     * @return a transformed SQL select
     */
    public static String removeOperations(String select){

        Select stmt = asSelectStatement(select);
        PlainSelect plainSelect = stmt.getPlainSelect();

        plainSelect.getSelectItems()
                    .removeIf(item ->
                            (item instanceof SelectItem<?>) &&
                            item.getExpression() instanceof Function);

        if (plainSelect.getSelectItems().isEmpty()) {
            plainSelect.getSelectItems().add(new SelectItem<>(new AllColumns()));
        }

        return stmt.toString();
    }


    public static String removeConstraints(String select) {
        Select stmt = asSelectStatement(select);
        handleSelect(stmt);
        return stmt.toString();
    }

    /**
     * add LIMIT for select in order to control row count
     *
     * @param select                   specifies SELECT sql
     * @param doRemoveOtherConstraints specified whether to remove other constraints
     * @param limitedRowCount          specifies the limit
     * @return select statement with LIMIT clause
     */
    public static String addLimitForHandlingRowCount(String select, boolean doRemoveOtherConstraints, int limitedRowCount){
        Select stmt = asSelectStatement(select);
        if (doRemoveOtherConstraints)
            handleSelect(stmt);
        if (limitedRowCount > 0){
            Limit limit = new Limit();
            limit.setRowCount(new LongValue(limitedRowCount));
            addLimitForSelect(stmt, limit);
        }
        return stmt.toString();
    }

    private static Select asSelectStatement(String select) {
        Statement stmt = SqlParserUtils.parseSqlCommand(select);
        if(! (stmt instanceof Select)){
            throw new IllegalArgumentException("SQL statement is not a SELECT: " + select);
        }
        return (Select) stmt;
    }

    private static void handleSelect(Select select) {
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect)select;
            plainSelect.setWhere(null);
            plainSelect.setLimit(null);
            plainSelect.setGroupByElement(null);
        } else if (select instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) select;
            for (Select innerSelect : setOperationList.getSelects()) {
                handleSelect(innerSelect);
            }
        } else {
            throw new RuntimeException("Cannot handle " + select.getClass());
        }
    }


    private static void addLimitForSelect(Select select, Limit limit) {
        if (select instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect)select;
            plainSelect.setLimit(limit);
        } else if (select instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) select;
            for (Select innerSelect : setOperationList.getSelects()) {
                addLimitForSelect(innerSelect, limit);
            }
        } else {
            throw new RuntimeException("Cannot add limit for " + select.getClass());
        }
    }

}
