package org.evomaster.client.java.controller.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 25-Mar-19.
 */
public class DbCleanerTest extends DatabaseTestTemplate {

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
        DbCleaner.clearDatabase_H2(getConnection());
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

}