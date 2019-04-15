package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

public class ParserUtils {


    public static boolean isSelect(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }

    public static boolean isDelete(String sql) {
        return sql.trim().toLowerCase().startsWith("delete");
    }

    public static boolean isUpdate(String sql) {
        return sql.trim().toLowerCase().startsWith("insert");
    }

    public static boolean isInsert(String sql) {
        return sql.trim().toLowerCase().startsWith("update");
    }


    public static Expression getWhere(Statement statement) {

        if(statement instanceof Select) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                return plainSelect.getWhere();
            }
        }

        throw new IllegalArgumentException("Cannot handle: " + statement.toString());
    }
}
