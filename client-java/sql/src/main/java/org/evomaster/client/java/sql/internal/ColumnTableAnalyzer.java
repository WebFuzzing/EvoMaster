package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by arcuri82 on 24-Apr-19.
 */
public class ColumnTableAnalyzer {

    private final DbInfoDto schema;

    private final Set<String> booleanConstantNames;

    public ColumnTableAnalyzer(DbInfoDto schema, Set<String> booleanConstantNames) {
        this.schema = schema;
        this.booleanConstantNames = booleanConstantNames;
    }

    /**
     * A given DELETE statement can only delete rows from only one table.
     * Therefore, we return a single SqlTableId.
     *
     * @param delete
     * @return
     */
    public SqlTableId getDeletedTable(String delete) {
        /*
         TODO code in this class is incomplete. For the moment, we just extract
         the name of the tables involved, and not full column info.
         */
        if (!SqlParserUtils.isDelete(delete)) {
            throw new IllegalArgumentException("Input string is not a valid SQL DELETE: " + delete);
        }
        Delete stmt = (Delete) SqlParserUtils.parseSqlCommand(delete);
        String fullyQualifiedName = stmt.getTable().getFullyQualifiedName();
        SqlTableId deletedTableId = new SqlTableId(fullyQualifiedName);
        return deletedTableId;
    }


    /**
     * Given an INSERT statement, we return the inserted table and the columns.
     * Only a single table can be inserted in a given insert statement.
     *
     * @param insertSqlStatement a string with a valid INSERT statement
     * @return an entry with the table being inserted and the columns with values
     */
    public Map.Entry<SqlTableId, Set<SqlColumnId>> getInsertedDataFields(String insertSqlStatement) {

        if (!SqlParserUtils.isInsert(insertSqlStatement)) {
            throw new IllegalArgumentException("Input string is not a valid SQL INSERT: " + insertSqlStatement);
        }

        Insert stmt = (Insert) SqlParserUtils.parseSqlCommand(insertSqlStatement);
        return getTableAndColumnIds(stmt.getTable(), stmt.getColumns());
    }


    /**
     * Given an UPDATE statement, we return the updated table and the columns.
     *
     * @param updateSqlStatement a string with a valid UPDATE statement
     * @return an entry with the table being updated and the columns with values
     */
    public Map.Entry<SqlTableId, Set<SqlColumnId>> getUpdatedDataFields(String updateSqlStatement) {

        if (!SqlParserUtils.isUpdate(updateSqlStatement)) {
            throw new IllegalArgumentException("Input string is not a valid SQL INSERT: " + updateSqlStatement);
        }

        Update stmt = (Update) SqlParserUtils.parseSqlCommand(updateSqlStatement);

        return getTableAndColumnIds(stmt.getTable(), stmt.getColumns());
    }

    private static AbstractMap.SimpleEntry<SqlTableId, Set<SqlColumnId>> getTableAndColumnIds(Table table, List<Column> columns) {
        SqlTableId updatedTableId = new SqlTableId(table.getFullyQualifiedName());
        Set<SqlColumnId> columnIds = columns.stream()
                .map(column -> new SqlColumnId(column.getColumnName()))
                .collect(Collectors.toSet());

        return new AbstractMap.SimpleEntry<>(updatedTableId, columnIds);
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
    public Map<SqlTableId, Set<SqlColumnId>> getSelectReadDataFields(String select) {

        if (!SqlParserUtils.isSelect(select)) {
            throw new IllegalArgumentException("Input string is not a valid SQL SELECT: " + select);
        }
        Statement stmt = SqlParserUtils.parseSqlCommand(select);
        TablesAndColumnsFinder finder = new TablesAndColumnsFinder(this.schema, this.booleanConstantNames);
        stmt.accept(finder);
        Map<SqlTableId, Set<SqlColumnId>> selectReadDataFields = new LinkedHashMap<>();
        finder.getColumnReferences()
                .forEach((table, columns) -> {
                    SqlTableId tableId = new SqlTableId(table.getFullyQualifiedName());
                    Set<SqlColumnId> columnIds = columns.stream()
                            .map(column -> new SqlColumnId(column.getColumnName()))
                            .collect(Collectors.toSet());
                    selectReadDataFields.put(tableId, columnIds);
                });
        return selectReadDataFields;
    }

}
