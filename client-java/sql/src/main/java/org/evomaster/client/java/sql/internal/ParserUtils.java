package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.update.Update;

public class ParserUtils {

    /**
     * We only use the selects that refer to objects in the database that are meaningful for testing purposes,
     * when code access to a sequence for example when getting the next id for a new object in the table,
     * then we don't want to use that select as a target.
     * @param sql
     * @return
     */
    public static boolean isSelect(String sql) {
        return startsWithIgnoreCase(sql, "select") && !isASequence(sql);
    }

    public static boolean isDelete(String sql) {
        return startsWithIgnoreCase(sql, "delete");
    }

    public static boolean isUpdate(String sql) {
        return startsWithIgnoreCase(sql, "update");
    }

    public static boolean isInsert(String sql) {
        return startsWithIgnoreCase(sql, "insert");
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
    public static boolean isSelectOne(String sql) {
        return sql!= null && sql.trim().toLowerCase().matches("select\\s+1\\s*;?");
    }


    public static Expression getWhere(Statement statement) {

        if(statement instanceof Select) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                return plainSelect.getWhere();
            }
        } else if(statement instanceof Delete){
            return ((Delete) statement).getWhere();
        } else if(statement instanceof Update){
            return ((Update) statement).getWhere();
        }

        throw new IllegalArgumentException("Cannot handle statement: " + statement.toString());
    }

    public static Statement asStatement(String statement) {
        Statement stmt;
        try {
            stmt = CCJSqlParserUtil.parse(statement);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SQL statement: " + statement + "\n" + e.getMessage(), e);
        }
        return stmt;
    }

    public static boolean canParseSqlStatement(String statement){
        try {
            CCJSqlParserUtil.parse(statement);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
