package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
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

    protected void clearDatabase(List<String> tablesToSkip){
        clearDatabase(tablesToSkip, null);
    }
    protected abstract void clearDatabase(List<String> tablesToSkip, List<String> tableToClean);

    protected abstract DatabaseType getDbType();

    @Test
    public void testSkipTableMisconfigured() throws Exception{

        String command = "CREATE TABLE Foo(id bigserial not null);";
        if (getDbType() == DatabaseType.MYSQL || getDbType() == DatabaseType.MARIADB)
            command = "CREATE TABLE Foo(id serial not null);";
        else if (getDbType() == DatabaseType.MS_SQL_SERVER)
            command = "CREATE TABLE Foo(id bigint not null IDENTITY);";

        SqlScriptRunner.execCommand(getConnection(), command);
        assertThrows(Exception.class, () -> clearDatabase(Arrays.asList("Bar")));
    }

    @Test
    public void testTableToClean() throws Exception{

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x int);");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(y int);");

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (42)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (66)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (y) VALUES (77)");


        QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(2, res.seeRows().size());
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;");
        assertEquals(1, res.seeRows().size());

        //Bar should be reset, but not Foo
        clearDatabase(null, Arrays.asList("Foo"));

        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(0, res.seeRows().size());
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;");
        assertEquals(1, res.seeRows().size());
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
        if (getDbType() == DatabaseType.MYSQL || getDbType() == DatabaseType.MARIADB){
            SqlScriptRunner.execCommand(getConnection(), "alter table Bar add foreign key (y) references Foo(x);");
        }else{
            SqlScriptRunner.execCommand(getConnection(), "alter table Bar add constraint FK foreign key (y) references Foo;");
        }


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

        if (getDbType() == DatabaseType.MYSQL || getDbType() == DatabaseType.MARIADB)
            SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id serial not null, x int, primary key (id));");
        else if(getDbType() == DatabaseType.MS_SQL_SERVER)
            SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id bigint not null IDENTITY, x int, primary key (id));");
        else
            SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id bigserial not null, x int, primary key (id));");

        int value = 42;

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (" + value + ")");

        QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(1, res.seeRows().size());
        assertEquals(value, res.seeRows().get(0).getValueByName("x"));

        long id;
        if (getDbType() == DatabaseType.MYSQL || getDbType() == DatabaseType.MARIADB){
            id = ((BigInteger) res.seeRows().get(0).getValueByName("id")).longValue();
        }else{
            id = (Long) res.seeRows().get(0).getValueByName("id");
        }

        //everything should be cleared after this command
        clearDatabase(null);
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(0, res.seeRows().size());

        //re-insert
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (" + value + ")");
        res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;");
        assertEquals(1, res.seeRows().size());

        long regeneratedId;
        if (getDbType() == DatabaseType.MYSQL || getDbType() == DatabaseType.MARIADB){
            regeneratedId = ((BigInteger) res.seeRows().get(0).getValueByName("id")).longValue();
        }else{
            regeneratedId = (Long) res.seeRows().get(0).getValueByName("id");
        }

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