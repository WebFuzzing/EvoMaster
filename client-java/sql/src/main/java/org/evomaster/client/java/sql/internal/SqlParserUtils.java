package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SqlParserUtils {

    /**
     * We only use the selects that refer to objects in the database that are meaningful for testing purposes,
     * when code access to a sequence for example when getting the next id for a new object in the table,
     * then we don't want to use that select as a target.
     *
     * @param sqlCommand
     * @return
     */
    public static boolean isSelect(String sqlCommand) {
        return startsWithIgnoreCase(sqlCommand, "select") && !isASequence(sqlCommand);
    }

    public static boolean isDelete(String sqlCommand) {
        return startsWithIgnoreCase(sqlCommand, "delete");
    }

    public static boolean isUpdate(String sqlCommand) {
        return startsWithIgnoreCase(sqlCommand, "update");
    }

    public static boolean isInsert(String sqlCommand) {
        return startsWithIgnoreCase(sqlCommand, "insert");
    }

    private static boolean startsWithIgnoreCase(String input, String prefix) {
        return input != null && input.trim().toLowerCase().startsWith(prefix);
    }

    private static boolean isASequence(String input) {
        return input != null && input.trim().toLowerCase().matches(".*(currval|nextval).*");
    }

    /**
     * check if the sql is `Select 1`
     * detected by proxyprint as
     * ERROR - FAILED TO COMPUTE HEURISTICS FOR SQL: SELECT 1
     * <p>
     * https://stackoverflow.com/questions/3668506/efficient-sql-test-query-or-validation-query-that-will-work-across-all-or-most
     */
    public static boolean isSelectOne(String sqlCommand) {
        return sqlCommand != null && sqlCommand.trim().toLowerCase().matches("select\\s+-?\\d+\\s*;?");
    }


    public static Expression getWhere(Statement parsedStatement) {
        if (parsedStatement instanceof Select) {
            Select select = (Select) parsedStatement;
            PlainSelect plainSelect = select.getPlainSelect();
            return plainSelect.getWhere();
        } else if (parsedStatement instanceof Delete) {
            return ((Delete) parsedStatement).getWhere();
        } else if (parsedStatement instanceof Update) {
            return ((Update) parsedStatement).getWhere();
        } else {
            throw new IllegalArgumentException("Cannot handle statement: " + parsedStatement.toString());
        }
    }

    /**
     * Extracts the "FROM" clause or the primary table involved in a SQL statement.
     * This method supports SELECT, DELETE, and UPDATE SQL statements.
     *
     * @param parsedStatement The parsed SQL statement as a {@link Statement} object.
     *                        This is typically obtained using JSQLParser's `CCJSqlParserUtil.parse`.
     * @return The {@link FromItem} representing the "FROM" clause or the main table for the statement.
     * - For a SELECT statement, returns the main {@link FromItem} in the "FROM" clause.
     * - For a DELETE statement, returns the table being deleted from.
     * - For an UPDATE statement, returns the table being updated.
     * @throws IllegalArgumentException If the provided statement type is not SELECT, DELETE, or UPDATE.
     */
    public static FromItem getFrom(Statement parsedStatement) {
        if (parsedStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) parsedStatement;
            return plainSelect.getFromItem();
        } else if (parsedStatement instanceof Delete) {
            return ((Delete) parsedStatement).getTable();
        } else if (parsedStatement instanceof Update) {
            return ((Update) parsedStatement).getTable();
        }
        throw new IllegalArgumentException("Cannot get FROM in statement: " + parsedStatement.toString());
    }

    public static List<Join> getJoins(Statement parsedStatement) {
        if (parsedStatement instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) parsedStatement;
            return plainSelect.getJoins();
        } else if (parsedStatement instanceof Delete) {
            Delete delete = (Delete) parsedStatement;
            return delete.getJoins();
        } else if (parsedStatement instanceof Update) {
            Update update = (Update) parsedStatement;
            if (update.getStartJoins() != null) {
                return update.getStartJoins();
            } else {
                return update.getJoins();
            }
        } else {
            throw new IllegalArgumentException("Cannot get JOIN in statement " + parsedStatement.toString());
        }
    }

    /**
     * This method assumes that the SQL command can be successfully parsed.
     *
     * @param sqlCommand to be parsed
     * @return the AST root node
     */
    public static Statement parseSqlCommand(String sqlCommand) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sqlCommand);
            return stmt;
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("Cannot parse SQL command: " + sqlCommand + "\n" + e.getMessage(), e);
        }
    }

    public static boolean canParseSqlStatement(String sqlCommand) {
        try {
            CCJSqlParserUtil.parse(sqlCommand);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the given FromItem is a Table.
     *
     * @param fromItem the FromItem to check
     * @return true if the FromItem is a Table, false otherwise
     */
    public static boolean isTable(FromItem fromItem) {
        return fromItem instanceof Table;
    }




    /**
     * Retrieves the fully qualified name of a table from the provided {@link FromItem}.
     * <p>
     * This method checks if the given {@code fromItem} is an instance of {@link Table}.
     * If it is, the method extracts and returns the fully qualified name of the table.
     * Otherwise, it throws an {@link IllegalArgumentException}.
     * </p>
     *
     * @param fromItem the {@link FromItem} instance to extract the table name from.
     * @return the fully qualified name of the table as a {@link String}.
     * @throws IllegalArgumentException if the provided {@code fromItem} is not an instance of {@link Table}.
     * @see net.sf.jsqlparser.schema.Table#getFullyQualifiedName()
     */
    public static String getTableName(FromItem fromItem) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            return table.getFullyQualifiedName();
        } else {
            throw new IllegalArgumentException("From item " + fromItem + " is not a table");
        }
    }

    public static Table getTable(FromItem fromItem) {
        if (fromItem instanceof Table) {
            return (Table) fromItem;
        } else {
            throw new IllegalArgumentException("From item " + fromItem + " is not a table");
        }
    }

    /**
     * Checks if the given {@link Statement} is a UNION statement.
     *
     * @param statement
     * @return
     */
    public static boolean isUnion(Statement statement) {
        if (statement instanceof Select) {
            Select select = (Select) statement;
            return select instanceof SetOperationList;
        }
        return false;
    }

    /**
     * Retrieves the "FROM" and "JOIN" items from a given SQL SELECT statement.
     *
     * @param select the SQL SELECT statement
     * @return a list of FromItem objects representing the "FROM" and "JOIN" items
     */
    public static List<FromItem> getFromAndJoinItems(Select select) {
        final FromItem fromItem = SqlParserUtils.getFrom(select);
        final List<Join> joins = SqlParserUtils.getJoins(select);
        List<FromItem> fromAndJoinItems = new ArrayList<>();
        if (fromItem != null) {
            fromAndJoinItems.add(fromItem);
        }
        if (joins != null) {
            for (Join join : joins) {
                fromAndJoinItems.add(join.getRightItem());
            }
        }
        return fromAndJoinItems;
    }
}
