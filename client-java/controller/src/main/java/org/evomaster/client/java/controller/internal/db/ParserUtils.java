package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

import java.util.List;

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

    /**
     * inspired by this example from https://stackoverflow.com/questions/46890089/how-can-i-purify-a-sql-query-and-replace-all-parameters-with-using-regex
     * @param sql is an original sql command which might contain comments or be dynamic sql with parameters
     * @param params are parameters which exists in the [sql]
     * @return a formatted sql. note that if the sql could not be parsed, then we return null
     *
     */
    public static String formatSql(String sql, List<String> params)  {
        StringBuilder sqlbuffer = new StringBuilder();

        ExpressionDeParser expDeParser = new ExpressionDeParser() {
            @Override
            public void visit(JdbcParameter parameter) {
                int index = parameter.getIndex();
                this.getBuffer().append(params.get(index-1));
            }
        };
        SelectDeParser selectDeparser = new SelectDeParser(expDeParser, sqlbuffer);
        expDeParser.setSelectVisitor(selectDeparser);
        expDeParser.setBuffer(sqlbuffer);
        StatementDeParser stmtDeparser = new StatementDeParser(expDeParser, selectDeparser, sqlbuffer);

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            stmt.accept(stmtDeparser);
            return stmtDeparser.getBuffer().toString();
        } catch (Exception e) {
            // catch all kinds of exception here since there might exist problems in processing params
            return null;
        }
    }
}
