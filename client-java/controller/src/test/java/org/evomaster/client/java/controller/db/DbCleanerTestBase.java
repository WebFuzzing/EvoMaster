package org.evomaster.client.java.controller.db;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by arcuri82 on 25-Mar-19.
 */
public abstract class DbCleanerTestBase {

    protected abstract Connection getConnection();

    protected abstract void clearDatabase(List<String> tablesToSkip);

    @Test
    public void testSkipTableMisconfigured() throws Exception{

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id bigserial not null);");

        assertThrows(Exception.class, () -> clearDatabase(Arrays.asList("Bar")));
    }

    @Test
    public void testSkipTable() throws Exception{

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x int);");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(y int);");

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (42)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (66)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (y) VALUES (77)");


        QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(2, res.seeRows().size());
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;");
        assertEquals(1, res.seeRows().size());

        //Foo should be reset, but not Bar
        clearDatabase(Arrays.asList("Bar"));

        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(0, res.seeRows().size());
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;");
        assertEquals(1, res.seeRows().size());
    }

    @Test
    public void testFKs() throws Exception{

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x int, primary key (x));");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(y int, primary key (y));");
        SqlScriptRunner.execCommand(getConnection(), "alter table Bar add constraint FK foreign key (y) references Foo;");

        //can't insert before Foo
        assertThrows(Exception.class, () ->
                SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (y) VALUES (42)")
        );

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (42)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (y) VALUES (42)");

        //shouldn't throw exception
        clearDatabase(null);

        QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(0, res.seeRows().size());
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;");
        assertEquals(0, res.seeRows().size());
    }

    @Test
    public void testResetIdentity() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id bigserial not null, x int, primary key (id));");

        int value = 42;

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (" + value + ")");

        QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(1, res.seeRows().size());
        assertEquals(value, res.seeRows().get(0).getValueByName("x"));

        long id = (Long) res.seeRows().get(0).getValueByName("id");

        //everything should be cleared after this command
        clearDatabase(null);
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(0, res.seeRows().size());

        //re-insert
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (" + value + ")");
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(1, res.seeRows().size());

        long regeneratedId = (Long) res.seeRows().get(0).getValueByName("id");

        /*
            If IDENTITY (eg, bigserial) id generation was reset, should get exactly the same id
         */
        assertEquals(id, regeneratedId);
    }


    @Test
    public void testAvoidViews() throws Exception{

        /*
            A db might have "views". Trying to delete data in those makes no-sense,
            and would result in an error when using TRUNCATE on them
         */

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x int, y int, primary key (x));");
        SqlScriptRunner.execCommand(getConnection(), "CREATE VIEW AView AS SELECT y FROM Foo;");

        //this should work without throwing any exception
        clearDatabase(null);
    }
}