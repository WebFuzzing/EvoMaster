package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseFakeMySQLSutController;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class H2InsertionTest extends DatabaseH2TestInit implements DatabaseTestTemplate {

    @Test
    public void testInsertPolygon() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE GEOMETRYTYPES(polygonColumn GEOMETRY(POLYGON) NOT NULL);");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))');");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('POLYGON ((-1 -2, 10 1, 2 20, -1 -2))');");
        assertThrows(SQLException.class,()->
            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('POLYGON ((-1 -2, 10 1, 2 20))');")
        );
        assertThrows(SQLException.class,()->
                SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('POLYGON ((-1 -2, 10 1, -1 -2))');")
        );
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('POLYGON ((-1 -2, -1 -2, -1 -2, -1 -2))');");

    }


    @Test
    public void testInsertMultiPolygon() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE GEOMETRYTYPES(polygonColumn GEOMETRY(MULTIPOLYGON) NOT NULL);");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('MULTIPOLYGON(((0 0, 0 1, 1 1, 1 0, 0 0)))');");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('MULTIPOLYGON(((-1 -2, 10 1, 2 20, -1 -2)))');");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('MULTIPOLYGON(((-1 -2, -1 -2, -1 -2, -1 -2)))');");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('MULTIPOLYGON EMPTY');");

        assertThrows(SQLException.class,()->
                SqlScriptRunner.execCommand(getConnection(), "INSERT INTO GEOMETRYTYPES(polygonColumn) VALUES ('MULTIPOLYGON(((0.121834956 0.14660847 , 0.28196073 0.69853836)))');")
        );
    }


    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }

}
