package org.evomaster.clientJava.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import org.evomaster.clientJava.controller.db.DataRow;
import org.evomaster.clientJava.controller.db.QueryResult;

public class SelectHeuristics {

    public static String removeConstraints(String select) {

        Select stmt;
        try {
            stmt = (Select) CCJSqlParserUtil.parse(select);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Select SQL: " + select + "\n" + e.getMessage(), e);
        }

        SelectBody selectBody = stmt.getSelectBody();
        if(selectBody instanceof PlainSelect){
            PlainSelect plainSelect = (PlainSelect) selectBody;
            plainSelect.setWhere(null);
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

        if(data.isEmpty()){
            //if no data, we have no info whatsoever
            return Double.MAX_VALUE;
        }

        Expression exp = getWhere(stmt);
        if(exp == null){
            //no constraint, and at least one data point
            return 0;
        }

        double min = Double.MAX_VALUE;
        for(DataRow row : data.seeRows()){
            double dist = HeuristicsCalculator.computeExpression(exp, row);
            if(dist == 0){
                return 0;
            }
            if(dist < min){
                min = dist;
            }
        }

        return min;
    }


    private static Expression getWhere(Select select){

        SelectBody selectBody = select.getSelectBody();
        if(selectBody instanceof PlainSelect){
            PlainSelect plainSelect = (PlainSelect) selectBody;
            return plainSelect.getWhere();
        }

        return null;
    }
}
