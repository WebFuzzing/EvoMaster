package org.evomaster.client.java.controller.internal.db;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserUtilsTest {


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
}