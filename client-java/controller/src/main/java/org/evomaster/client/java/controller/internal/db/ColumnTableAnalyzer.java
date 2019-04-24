package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public class ColumnTableAnalyzer {


    public static Set<String> getDeletedTables(String delete){

        if(! ParserUtils.isDelete(delete)){
            throw new IllegalArgumentException("Input string is not a valid SQL DELETE: " + delete);
        }

        Set<String> set = new HashSet<>();
        Delete stmt = (Delete) ParserUtils.asStatement(delete);

        Table table = stmt.getTable();
        if(table != null){
            set.add(table.getName());
        } else {
            //TODO need to handle special cases of multi-tables with JOINs
            throw new IllegalArgumentException("Cannot handle: " + delete);
        }

        return set;
    }


    /**
     * Given a SELECT, check what it returns is based on (columns and tables).
     * Something like "select x from Foo" would give info on "Foo->{x}".
     * However, at times, what is returned is not directly the content of a column, but
     * rather some computations on it.
     * For example, in "select avg(x) from Foo", we would still be just interested in
     * the info that the data in "Foo->{x}" was used to compute the result.
     */
    public static Map<String, Set<String>> getSelectReadDataFields(String select){

        if(! ParserUtils.isSelect(select)){
            throw new IllegalArgumentException("Input string is not a valid SQL SELECT: " + select);
        }

        Map<String, Set<String>> map = new HashMap<>();

        /*
            TODO: for now, we just use * for all read Tables.
            But, we should look at actual read columns.
         */

        Select stmt = (Select) ParserUtils.asStatement(select);
        SelectBody selectBody = stmt.getSelectBody();

        if (selectBody instanceof PlainSelect) {

            PlainSelect plainSelect = (PlainSelect) selectBody;

            FromItem fromItem = plainSelect.getFromItem();
            if(fromItem == null){
                //is this even possible? ie, a SELECT without FROM
                return map;
            }

            extractUsedColumnsAndTables(map, fromItem);

            List<Join> joins = plainSelect.getJoins();
            if(joins != null) {
                for (Join join : joins) {
                    FromItem rightItem = join.getRightItem();
                    extractUsedColumnsAndTables(map, rightItem);
                }
            }
        } else {
            throw new IllegalArgumentException("Cannot handle: " + select);
        }

        return map;
    }

    private static void extractUsedColumnsAndTables(Map<String, Set<String>> map, FromItem fromItem) {
        if(fromItem instanceof Table){
            Table table = (Table) fromItem;
            Set<String> columns = map.computeIfAbsent(table.getName(), k -> new HashSet<>());
            //TODO: should check actual fields... would likely need to pass SelectBody as input as well
            if(! columns.contains("*")) {
                columns.add("*");
            }
        } else {
            // TODO handle other cases, eg sub-selects
            throw new IllegalArgumentException("Cannot handle: " + fromItem.toString());
        }
    }

}
