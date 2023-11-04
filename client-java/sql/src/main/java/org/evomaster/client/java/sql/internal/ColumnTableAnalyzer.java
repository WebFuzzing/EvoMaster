package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;

import java.util.*;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public class ColumnTableAnalyzer {


    /*
        TODO code in this class is incomplete. For the moment, we just extract
        the name of the tables involved, and not full column info.
     */

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
            throw new IllegalArgumentException("Cannot handle delete: " + delete);
        }

        return set;
    }


    public static Map<String, Set<String>> getInsertedDataFields(String insert){

        if(! ParserUtils.isInsert(insert)){
            throw new IllegalArgumentException("Input string is not a valid SQL INSERT: " + insert);
        }

        Map<String, Set<String>> map = new HashMap<>();

        Insert stmt = (Insert) ParserUtils.asStatement(insert);

        Table table = stmt.getTable();
        if(table != null){
            handleTable(map, table);
        } else {
            //TODO all other cases
            throw new IllegalArgumentException("Cannot handle insert: " + insert);
        }

        return map;
    }


    public static Map<String, Set<String>> getUpdatedDataFields(String update){

        if(! ParserUtils.isUpdate(update)){
            throw new IllegalArgumentException("Input string is not a valid SQL INSERT: " + update);
        }

        Map<String, Set<String>> map = new HashMap<>();

        Update stmt = (Update) ParserUtils.asStatement(update);

        Table table = stmt.getTable();
        if(table!=null){
            handleTable(map, table);
        } else {
            throw new IllegalArgumentException("Cannot handle update: " + update);
        }

        return map;
    }


    /**
     * Given a SELECT, check what it returns is based on (columns and tables).
     * Something like "select x from Foo" would give info on "Foo-&gt;{x}".
     * However, at times, what is returned is not directly the content of a column, but
     * rather some computations on it.
     * For example, in "select avg(x) from Foo", we would still be just interested in
     * the info that the data in "Foo-&gt;{x}" was used to compute the result.
     *
     * @param select SQL select command
     * @return a map from table_names to column_names
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
            throw new IllegalArgumentException("Cannot handle select: " + select);
        }

        return map;
    }

    private static void handleTable(Map<String, Set<String>> map, Table table){
        Set<String> columns = map.computeIfAbsent(table.getName(), k -> new HashSet<>());
        //TODO: should check actual fields... would likely need to pass SelectBody as input as well
        if(! columns.contains("*")) {
            columns.add("*");
        }
    }

    private static void extractUsedColumnsAndTables(Map<String, Set<String>> map, FromItem fromItem) {
        if(fromItem instanceof Table){
            Table table = (Table) fromItem;
            handleTable(map, table);
        } else {
            // TODO handle other cases, eg sub-selects
            throw new IllegalArgumentException("Cannot handle fromItem: " + fromItem.toString());
        }
    }

}
