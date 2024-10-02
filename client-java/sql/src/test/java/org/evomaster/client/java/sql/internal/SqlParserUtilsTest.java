package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import static org.junit.jupiter.api.Assertions.*;

public class SqlParserUtilsTest {

    //TODO see https://github.com/JSQLParser/JSqlParser/issues/420
    @Test
    public void testEscapeInput(){

        assertTrue(SqlParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE '$a' ESCAPE '$'"));
        assertTrue(SqlParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE  ?1 ESCAPE '$'"));
        assertTrue(SqlParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE '$a' ESCAPE ?1"));
        assertTrue(SqlParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE ?1 ESCAPE ?2"));
    }

     //TODO see https://github.com/JSQLParser/JSqlParser/issues/1405
    @Test
    public void testRestartSequence(){

        String sql = "ALTER SEQUENCE SYSTEM_SEQUENCE_40560F88_80C4_4F3B_BDAA_D18CC8D5C5AA RESTART WITH 1";

        boolean parsed = SqlParserUtils.canParseSqlStatement(sql);

        assertTrue(parsed);
    }


    @Test
    public void testEmptyInsertValue(){
        /*
            this is accepted by H2, although it does not seem to be valid according to the
            SQL syntax. so I guess it is fine that JSqlParser does not handle it
         */
        String sql = "INSERT INTO Foo() VALUES()";

        boolean parsed = SqlParserUtils.canParseSqlStatement(sql);

        assertFalse(parsed);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT 1", "SELECT 1;", "Select    1;", "Select 1 ; "})
    public void testSelectOne(String sql) throws JSQLParserException {
        Statement s = CCJSqlParserUtil.parse(sql);
        assertNotNull(s);
        Expression where = SqlParserUtils.getWhere(s);
        assertNull(where);
        boolean isSelectOne = SqlParserUtils.isSelectOne(sql);
        assertTrue(isSelectOne);
    }

    @Test
    public void testOnConflictPostgresql(){
        SqlParserUtils.parseSqlCommand("INSERT INTO vets VALUES (1, 'James', 'Carter') ON CONFLICT DO NOTHING;");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT -42;", "SELECT -1;", "Select 10;", "Select -0;", "SELECT 0;"})
    public void testMoreThanSelectOne(String sql) throws JSQLParserException {
        Statement s = CCJSqlParserUtil.parse(sql);
        assertNotNull(s);
        Expression where = SqlParserUtils.getWhere(s);
        assertNull(where);
        boolean isSelectOne = SqlParserUtils.isSelectOne(sql);
        assertTrue(isSelectOne);
    }
}
