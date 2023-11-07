package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.sql.internal.ParserUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import static org.junit.jupiter.api.Assertions.*;

public class ParserUtilsTest {

    @Disabled //TODO see https://github.com/JSQLParser/JSqlParser/issues/420
    @Test
    public void testEscapeInput(){

        assertTrue(ParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE '$a' ESCAPE '$'"));
        assertTrue(ParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE  ?1 ESCAPE '$'"));
        assertTrue(ParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE '$a' ESCAPE ?1"));
        assertTrue(ParserUtils.canParseSqlStatement("SELECT * FROM Foo WHERE x LIKE ?1 ESCAPE ?2"));
    }

    @Disabled //TODO see https://github.com/JSQLParser/JSqlParser/issues/1405
    @Test
    public void testRestartSequence(){

        String sql = "ALTER SEQUENCE SYSTEM_SEQUENCE_40560F88_80C4_4F3B_BDAA_D18CC8D5C5AA RESTART WITH 1";

        boolean parsed = ParserUtils.canParseSqlStatement(sql);

        assertTrue(parsed);
    }

    @Disabled
    @Test
    public void testEmptyInsertValue(){

        /*
            this is accepted by H2, although it does not seem to be valid according to the
            SQL syntax. so i guess it is fine that JSqlParser does not handle it
         */
        String sql = "INSERT INTO Foo() VALUES()";

        boolean parsed = ParserUtils.canParseSqlStatement(sql);

        assertTrue(parsed);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT 1", "SELECT 1;", "Select    1;", "Select 1 ; "})
    public void testSelectOne(String sql) throws JSQLParserException {
        Statement s = CCJSqlParserUtil.parse(sql);
        assertNotNull(s);
        Expression where = ParserUtils.getWhere(s);
        assertNull(where);
        boolean isSelectOne = ParserUtils.isSelectOne(sql);
        assertTrue(isSelectOne);
    }

    @Test
    public void testOnConflictPostgresql(){
        assertThrows(IllegalArgumentException.class, () -> ParserUtils.asStatement("INSERT INTO vets VALUES (1, 'James', 'Carter') ON CONFLICT DO NOTHING;"));
    }
}
