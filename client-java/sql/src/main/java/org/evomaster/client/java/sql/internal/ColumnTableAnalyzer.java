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

    public static SqlTableId getDeletedTable(String delete) {

        if (!SqlParserUtils.isDelete(delete)) {
            throw new IllegalArgumentException("Input string is not a valid SQL DELETE: " + delete);
        }

        Delete stmt = (Delete) SqlParserUtils.parseSqlCommand(delete);

        Table table = stmt.getTable();
        if (table != null) {
            SqlTableId sqlTableId = new SqlTableId(table.getFullyQualifiedName());
            return sqlTableId;
        } else {
            //TODO need to handle special cases of multi-tables with JOINs
            throw new IllegalArgumentException("Cannot handle delete: " + delete);
        }
    }


    public static Map.Entry<SqlTableId, Set<SqlColumnId>> getInsertedDataFields(String insert) {

        if (!SqlParserUtils.isInsert(insert)) {
            throw new IllegalArgumentException("Input string is not a valid SQL INSERT: " + insert);
        }

        Insert stmt = (Insert) SqlParserUtils.parseSqlCommand(insert);
        Table table = stmt.getTable();
        if (table != null) {
            Map.Entry<SqlTableId, Set<SqlColumnId>> insertedDataFields = new AbstractMap.SimpleEntry<>(
                    new SqlTableId(table.getFullyQualifiedName()),
                    Collections.singleton(new SqlColumnId("*")));
            return insertedDataFields;
        } else {
            //TODO all other cases
            throw new IllegalArgumentException("Cannot handle insert: " + insert);
        }
    }


    public static Map.Entry<SqlTableId, Set<SqlColumnId>> getUpdatedDataFields(String update) {

        if (!SqlParserUtils.isUpdate(update)) {
            throw new IllegalArgumentException("Input string is not a valid SQL INSERT: " + update);
        }

        Update stmt = (Update) SqlParserUtils.parseSqlCommand(update);

        Table table = stmt.getTable();
        if (table != null) {
            Map.Entry<SqlTableId, Set<SqlColumnId>> updatedDataFields = new AbstractMap.SimpleEntry<>(
                    new SqlTableId(table.getFullyQualifiedName()),
                    Collections.singleton(new SqlColumnId("*")));
            return updatedDataFields;
        } else {
            throw new IllegalArgumentException("Cannot handle update: " + update);
        }
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
    public static Map<SqlTableId, Set<SqlColumnId>> getSelectReadDataFields(String select) {

        if (!SqlParserUtils.isSelect(select)) {
            throw new IllegalArgumentException("Input string is not a valid SQL SELECT: " + select);
        }

        Map<SqlTableId, Set<SqlColumnId>> map = new HashMap<>();

        /*
            TODO: for now, we just use * for all read Tables.
            But, we should look at actual read columns.
         */

        Select stmt = (Select) SqlParserUtils.parseSqlCommand(select);
        PlainSelect plainSelect = stmt.getPlainSelect();

        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem == null) {
            //is this even possible? ie, a SELECT without FROM
            return map;
        }

        extractUsedColumnsAndTables(map, fromItem);

        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                extractUsedColumnsAndTables(map, rightItem);
            }
        }

        return map;
    }

    private static void handleTable(Map<SqlTableId, Set<SqlColumnId>> map, Table table) {
        Set<SqlColumnId> columns = map.computeIfAbsent(new SqlTableId(table.getFullyQualifiedName()), k -> new HashSet<>());
        //TODO: should check actual fields... would likely need to pass SelectBody as input as well
        if (!columns.contains("*")) {
            columns.add(new SqlColumnId("*"));
        }
    }

    private static void extractUsedColumnsAndTables(Map<SqlTableId, Set<SqlColumnId>> map, FromItem fromItem) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            handleTable(map, table);
        } else {
            // TODO handle other cases, eg sub-selects
            throw new IllegalArgumentException("Cannot handle fromItem: " + fromItem.toString());
        }
    }

}
