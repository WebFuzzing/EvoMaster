package org.evomaster.client.java.sql;

import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SequenceH2Test {

    private static Connection connection;


    @BeforeAll
    public static void initClass() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
    }

    @BeforeEach
    public void initTest() throws Exception {
        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;");
    }

    @Test
    public void testSequence() throws Exception {
        SqlScriptRunner.execCommand(connection,"CREATE TABLE Foo(id BIGSERIAL, x INT NOT NULL);");
        QueryResult queryResult0 = SqlScriptRunner.execCommand(connection, "SELECT id,x FROM Foo;");
        assertTrue(queryResult0.isEmpty());
        SqlScriptRunner.execCommand(connection, "INSERT INTO Foo(x) VALUES (5);");

        QueryResult queryResult1 = SqlScriptRunner.execCommand(connection, "SELECT id,x FROM Foo ORDER BY id;");
        assertEquals(1, queryResult1.seeRows().size());
        assertEquals(5, queryResult1.seeRows().get(0).getValueByName("x","Foo"));
        assertEquals(1L, queryResult1.seeRows().get(0).getValueByName("id","Foo"));

        SqlScriptRunner.execCommand(connection, "INSERT INTO Foo(x) VALUES (42);");
        QueryResult queryResult2 = SqlScriptRunner.execCommand(connection, "SELECT id,x FROM Foo ORDER BY id;");
        assertEquals(2, queryResult2.seeRows().size());
        assertEquals(5, queryResult2.seeRows().get(0).getValueByName("x","Foo"));
        assertEquals(1L, queryResult2.seeRows().get(0).getValueByName("id","Foo"));
        assertEquals(42, queryResult2.seeRows().get(1).getValueByName("x","Foo"));
        assertEquals(2L, queryResult2.seeRows().get(1).getValueByName("id","Foo"));

        QueryResult queryResult3 = SqlScriptRunner.execCommand(connection, "SELECT * FROM INFORMATION_SCHEMA.SEQUENCES;");
        assertEquals(0, queryResult3.seeRows().size());

        SqlScriptRunner.execCommand(connection, "TRUNCATE TABLE Foo RESTART IDENTITY");

        SqlScriptRunner.execCommand(connection, "INSERT INTO Foo(x) VALUES (5);");

        QueryResult queryResult4 = SqlScriptRunner.execCommand(connection, "SELECT id,x FROM Foo ORDER BY id;");
        assertEquals(1, queryResult4.seeRows().size());
        assertEquals(5, queryResult4.seeRows().get(0).getValueByName("x","Foo"));
        assertEquals(1L, queryResult4.seeRows().get(0).getValueByName("id","Foo"));

        SqlScriptRunner.execCommand(connection, "INSERT INTO Foo(x) VALUES (42);");
        QueryResult queryResult5 = SqlScriptRunner.execCommand(connection, "SELECT id,x FROM Foo ORDER BY id;");
        assertEquals(2, queryResult5.seeRows().size());
        assertEquals(5, queryResult5.seeRows().get(0).getValueByName("x","Foo"));
        assertEquals(1L, queryResult5.seeRows().get(0).getValueByName("id","Foo"));
        assertEquals(42, queryResult5.seeRows().get(1).getValueByName("x","Foo"));
        assertEquals(2L, queryResult5.seeRows().get(1).getValueByName("id","Foo"));

    }

    @Test
    public void testRestartIdentity() throws Exception {
        SqlScriptRunner.execCommand(connection,"CREATE TABLE Foo(id BIGINT NOT NULL PRIMARY KEY, x INT NOT NULL);");
        SqlScriptRunner.execCommand(connection, "TRUNCATE TABLE Foo RESTART IDENTITY");
    }


        @Test
    public void testCreateSequence() throws Exception {
        SqlScriptRunner.execCommand(connection,"CREATE SEQUENCE SEQ1 AS BIGINT START WITH 1;");
        QueryResult queryResult0 = SqlScriptRunner.execCommand(connection, "SELECT * FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_NAME='SEQ1';");
        assertEquals(1, queryResult0.seeRows().size());
        assertEquals(1L, queryResult0.seeRows().get(0).getValueByName("BASE_VALUE","SEQUENCES"));

        QueryResult queryResult1 = SqlScriptRunner.execCommand(connection,"SELECT next value for SEQ1");
        assertEquals(1, queryResult1.seeRows().size());
        assertEquals(1L, queryResult1.seeRows().get(0).getValue(0));

        QueryResult queryResult2 = SqlScriptRunner.execCommand(connection,"SELECT next value for SEQ1");
        assertEquals(1, queryResult2.seeRows().size());
        assertEquals(2L, queryResult2.seeRows().get(0).getValue(0));

        QueryResult queryResult3 = SqlScriptRunner.execCommand(connection,"SELECT next value for SEQ1");
        assertEquals(1, queryResult3.seeRows().size());
        assertEquals(3L, queryResult3.seeRows().get(0).getValue(0));

        QueryResult queryResult4 = SqlScriptRunner.execCommand(connection, "SELECT * FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_NAME='SEQ1';");
        assertEquals(1, queryResult4.seeRows().size());
        assertEquals(4L, queryResult4.seeRows().get(0).getValueByName("BASE_VALUE","SEQUENCES"));

    }


}
