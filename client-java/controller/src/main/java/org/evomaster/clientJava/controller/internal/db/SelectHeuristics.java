package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.TokenMgrError;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.evomaster.clientJava.controller.db.DataRow;
import org.evomaster.clientJava.controller.db.QueryResult;

import java.util.*;

public class SelectHeuristics {

    public static final String UNNAMED_TABLE = "___unnamed_table___";


    /**
     *
     * @return
     */
    public static Map<String, Set<String>> getReadDataFields(String select){

        Map<String, Set<String>> map = new HashMap<>();

        /*
            TODO: for now, we just use * for all read Tables.
            But, we should look at actual read columns.
         */

        Select stmt = asStatement(select);
        SelectBody selectBody = stmt.getSelectBody();

        if (selectBody instanceof PlainSelect) {

            PlainSelect plainSelect = (PlainSelect) selectBody;

            FromItem fromItem = plainSelect.getFromItem();
            if(fromItem == null){
                //is this even possible?
                return map;
            }

            handleFromItem(map, fromItem);

            List<Join> joins = plainSelect.getJoins();
            if(joins != null) {
                for (Join join : joins) {
                    FromItem rightItem = join.getRightItem();
                    handleFromItem(map, rightItem);
                }
            }
        }

        return map;
    }

    private static void handleFromItem(Map<String, Set<String>> map, FromItem fromItem) {
        if(fromItem instanceof Table){
            Table table = (Table) fromItem;
            Set<String> columns = map.computeIfAbsent(table.getName(), k -> new HashSet<>());
            //TODO: should check actual fields
            columns.add("*");

        } // TODO handle other cases, eg sub-selects
    }

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

        Select stmt = asStatement(select);

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

    /**
     * For example, when we have "select count(*)" we are not interested
     * in the count, but the actual involved fields, so we want to
     * transform it into "select *" by removing the count() operation.
     *
     * @param select
     * @return
     */
    public static String removeOperations(String select){

        Select stmt = asStatement(select);
        SelectBody selectBody = stmt.getSelectBody();

        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;

            plainSelect.getSelectItems()
                    .removeIf(item ->
                            ((SelectExpressionItem)item).getExpression() instanceof Function);
        }

        return stmt.toString();
    }


    public static String removeConstraints(String select) {

        Select stmt = asStatement(select);

        SelectBody selectBody = stmt.getSelectBody();
        handleSelectBody(selectBody);

        return stmt.toString();
    }

    private static Select asStatement(String select) {
        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(select);
        } catch (Exception | TokenMgrError e) {
            throw new IllegalArgumentException("Invalid Select SQL: " + select + "\n" + e.getMessage(), e);
        }
        return stmt;
    }

    private static void handleSelectBody(SelectBody selectBody) {
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelect.setWhere(null);
            plainSelect.setLimit(null);
        } else if (selectBody instanceof SetOperationList) {
            for(SelectBody select : ((SetOperationList) selectBody).getSelects()){
                handleSelectBody(select);
            }
        } else {
            throw new RuntimeException("Cannot handle " + selectBody.getClass());
        }

    }


    public static double computeDistance(String select, QueryResult data) {

        Select stmt = asStatement(select);

        if (data.isEmpty()) {
            //if no data, we have no info whatsoever
            return Double.MAX_VALUE;
        }

        Expression where = getWhere(stmt);
        if (where == null) {
            //no constraint, and at least one data point
            return 0;
        }

        /*
            FIXME: aliases should be part of QueryResult
         */
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
     * Workaround to JDBC limitations: it does not provide any API to
     * query meta-data on table aliases!!! ad-hoc solutions for each
     * possible imaginable DB are not scalable.
     * So, we just try to do some best effort to cover most cases
     *
     * @param select the string containing the SQL SELECT command
     * @return map from alias to table name
     */
    public static Map<String, String> getTableAliases(Select select) {

        Map<String, String> aliases = new HashMap<>();

        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;

            FromItem fromItem = plainSelect.getFromItem();
            fromItem.accept(new FromVisitor(aliases));

            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                joins.forEach(j -> j.getRightItem().accept(new FromVisitor(aliases)));
            }
        }

        return aliases;
    }

    private static class FromVisitor extends FromItemVisitorAdapter{

        private final Map<String, String> aliases;

        private FromVisitor(Map<String, String> aliases) {
            this.aliases = aliases;
        }

        @Override
        public void visit(Table table) {
            handleAlias(aliases, table);
        }

        @Override
        public void visit(SubSelect subSelect) {
            handleAlias(aliases, subSelect);
        }

    }



    private static void handleAlias(Map<String, String> aliases, SubSelect subSelect) {
        Alias alias = subSelect.getAlias();
        if (alias != null) {
            String aliasName = alias.getName();
            if (aliasName != null) {
                /*
                    FIXME: need to generalize,
                    ie for when there can be several un-named sub-selects referring
                    to columns with same names
                 */
                String tableName = UNNAMED_TABLE;
                aliases.put(aliasName.trim().toLowerCase(), tableName.trim().toLowerCase());
            }
        }
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
