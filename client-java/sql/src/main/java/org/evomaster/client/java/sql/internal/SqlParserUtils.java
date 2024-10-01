package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

public class SqlParserUtils {

    /**
     * We only use the selects that refer to objects in the database that are meaningful for testing purposes,
     * when code access to a sequence for example when getting the next id for a new object in the table,
     * then we don't want to use that select as a target.
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

    private static boolean startsWithIgnoreCase(String input, String prefix){
        return input!= null && input.trim().toLowerCase().startsWith(prefix);
    }

    private static boolean isASequence(String input) {
        return input!= null && input.trim().toLowerCase().matches(".*(currval|nextval).*");
    }

    /**
     * check if the sql is `Select 1`
     * detected by proxyprint as
     *      ERROR - FAILED TO COMPUTE HEURISTICS FOR SQL: SELECT 1
     *
     * https://stackoverflow.com/questions/3668506/efficient-sql-test-query-or-validation-query-that-will-work-across-all-or-most
     */
    public static boolean isSelectOne(String sqlCommand) {
        return sqlCommand!= null && sqlCommand.trim().toLowerCase().matches("select\\s+-?\\d+\\s*;?");
    }


    public static Expression getWhere(Statement parsedStatement) {

        if (parsedStatement instanceof Select) {
            Select select = (Select) parsedStatement;
            PlainSelect plainSelect = select.getPlainSelect();
            return plainSelect.getWhere();
        } else if(parsedStatement instanceof Delete){
            return ((Delete) parsedStatement).getWhere();
        } else if(parsedStatement instanceof Update){
            return ((Update) parsedStatement).getWhere();
        } else {
            throw new IllegalArgumentException("Cannot handle statement: " + parsedStatement.toString());
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

    public static boolean canParseSqlStatement(String sqlCommand){
        try {
            CCJSqlParserUtil.parse(sqlCommand);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
