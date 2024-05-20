package org.evomaster.client.java.sql.advanced.helpers;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

import static java.lang.String.format;

public class SqlParserHelper {

    public static Statement parseStatement(String statement) {
        try {
            return CCJSqlParserUtil.parse(statement);
        } catch (JSQLParserException e) {
            throw new RuntimeException(format("Error occurred while parsing SQL statement: %s", statement), e);
        }
    }
}
