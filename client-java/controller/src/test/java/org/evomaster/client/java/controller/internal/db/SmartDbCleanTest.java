package org.evomaster.client.java.controller.internal.db;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.db.QueryResult;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public interface SmartDbCleanTest extends DatabaseTestTemplate {


    @Test
    default void testAccessedClean() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT, y INT)", true);
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (0, 0)", true);

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            given().accept(ContentType.JSON)
                    .get(url + INFO_SUT_PATH)
                    .then()
                    .statusCode(200);

            QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            startNewTest(url);

            // perform select sql and the result should be one as init
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // the row should be still one since the select would not lead to clean data
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            // perform insert sql, and the table should be clean afterwards
            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (1, 1);", true);
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(2, res.seeRows().size());

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // the table is empty
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

        } finally {
            starter.stop();
        }
    }


    @Test
    default void testFkClean() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(id INT Primary Key, valueColumn INT)", true);
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id INT Primary Key, valueColumn INT, bar_id INT, " +
                "CONSTRAINT fk FOREIGN KEY (bar_id) REFERENCES Bar(id) )", true);

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (0, 0)", true);
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (0, 0, 0)", true);

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            given().accept(ContentType.JSON)
                    .get(url + INFO_SUT_PATH)
                    .then()
                    .statusCode(200);

            QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            startNewTest(url);

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (1, 1, 0);", true);
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(2, res.seeRows().size());

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());

        } finally {
            starter.stop();
        }
    }


    @Test
    default void testAccessedFkClean() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(id INT Primary Key, valueColumn INT)", true);
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id INT Primary Key, valueColumn INT, bar_id INT, " +
                "CONSTRAINT fk FOREIGN KEY (bar_id) REFERENCES Bar(id) )", true);

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (0, 0)", true);
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (0, 0, 0)", true);

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            given().accept(ContentType.JSON)
                    .get(url + INFO_SUT_PATH)
                    .then()
                    .statusCode(200);

            QueryResult res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            startNewTest(url);

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (1, 1);", true);
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(2, res.seeRows().size());

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // Man: we might need to clean Foo as well, since now it refers to invalid bar_id
            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            res = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(0, res.seeRows().size());

        } finally {
            starter.stop();
        }
    }
}
