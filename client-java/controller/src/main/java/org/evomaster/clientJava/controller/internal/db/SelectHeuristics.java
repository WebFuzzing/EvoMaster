package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.evomaster.clientJava.controller.db.DataRow;
import org.evomaster.clientJava.controller.db.QueryResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectHeuristics {

    /**
     * The constraints in the WHERE clause might reference
     * fields that are not retrieved in the SELECT.
     * Therefore, we need to add them, otherwise it
     * would not be possible to calculate any heuristics
     *
     * @param select
     * @return
     */
    public static String addFieldsToSelect(String select) {

        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(select);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Select SQL: " + select + "\n" + e.getMessage(), e);
        }

        SelectBody selectBody = stmt.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;

            Expression where = plainSelect.getWhere();
            if (where == null) {
                //nothing to do
                return select;
            }

            List<SelectItem> fields = plainSelect.getSelectItems();

            where.accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {

                    String target = column.toString();

                    boolean found = false;
                    for (SelectItem si : fields) {
                        SelectExpressionItem field = (SelectExpressionItem) si;
                        String exp = field.getExpression().toString();
                        if (target.equals(exp)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        SelectExpressionItem item = new SelectExpressionItem();
                        item.setExpression(column);
                        fields.add(item);
                    }
                }
            });
        }

        return stmt.toString();
    }


    public static String removeConstraints(String select) {

        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(select);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Select SQL: " + select + "\n" + e.getMessage(), e);
        }

        SelectBody selectBody = stmt.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelect.setWhere(null);
            plainSelect.setLimit(null);
        }

        return stmt.toString();
    }


    public static double computeDistance(String select, QueryResult data) {

        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(select);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Select SQL: " + select + "\n" + e.getMessage(), e);
        }

        if (data.isEmpty()) {
            //if no data, we have no info whatsoever
            return Double.MAX_VALUE;
        }

        Expression where = getWhere(stmt);
        if (where == null) {
            //no constraint, and at least one data point
            return 0;
        }

        Map<String, String> aliases = getTableAliases(stmt);
        HeuristicsCalculator calculator = new HeuristicsCalculator(aliases);

        double min = Double.MAX_VALUE;
        for (DataRow row : data.seeRows()) {
            double dist = calculator.computeExpression(where, row);
            if (dist == 0) {
                return 0;
            }
            if (dist < min) {
                min = dist;
            }
        }

        return min;
    }

    /**
     * @param select
     * @return map from alias to table name
     */
    public static Map<String, String> getTableAliases(Select select) {
        Map<String, String> aliases = new HashMap<>();

        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;

            FromItem fromItem = plainSelect.getFromItem();
            fromItem.accept(new FromItemVisitorAdapter() {
                @Override
                public void visit(Table table) {
                    handleAlias(aliases, table);
                }
            });

            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                joins.forEach(j -> j.getRightItem().accept(new FromItemVisitorAdapter() {
                    @Override
                    public void visit(Table table) {
                        handleAlias(aliases, table);
                    }
                }));
            }
        }

        return aliases;
    }

    private static void handleAlias(Map<String, String> aliases, Table table) {
        Alias alias = table.getAlias();
        if (alias != null) {
            String aliasName = alias.getName();
            if (aliasName != null) {
                String tableName = table.getName();
                aliases.put(aliasName.trim().toLowerCase(), tableName.trim().toLowerCase());
            }
        }
    }

    private static Expression getWhere(Select select) {

        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            return plainSelect.getWhere();
        }

        return null;
    }
}
