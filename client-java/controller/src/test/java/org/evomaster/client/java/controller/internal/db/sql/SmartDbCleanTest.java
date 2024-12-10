package org.evomaster.client.java.controller.internal.db.sql;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import  io.restassured.RestAssured;
import  org.evomaster.client.java.controller.api.ControllerConstants;
import static org.junit.jupiter.api.Assertions.assertEquals;

public interface SmartDbCleanTest extends DatabaseTestTemplate {


    @Test
    default void testAccessedClean() throws Exception {
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT, y INT)", true);

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += ControllerConstants.BASE_PATH;

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.INFO_SUT_PATH)
                    .then()
                    .statusCode(200);

            QueryResult res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            startNewTest(url);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (0, 0)", true);

            // perform select sql and the result should be one as init
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // empty data in db
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (1, 1);", true);
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // the table is empty
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

        } finally {
            starter.stop();
        }
    }


    @Test
    default void testFkClean() throws Exception {
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(id INT Primary Key, valueColumn INT)", true);
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id INT Primary Key, valueColumn INT, bar_id INT, " +
                "CONSTRAINT fk FOREIGN KEY (bar_id) REFERENCES Bar(id) )", true);



        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += ControllerConstants.BASE_PATH;

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.INFO_SUT_PATH)
                    .then()
                    .statusCode(200);

            QueryResult res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(0, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            startNewTest(url);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (0, 0)", true);
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (0, 0, 0)", true);

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // empty data in db
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(0, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (0, 0)", true);
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (1, 1, 0);", true);
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // empty data in db
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(0, res.seeRows().size());

        } finally {
            starter.stop();
        }
    }


    @Test
    default void testAccessedFkClean() throws Exception {
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(id INT Primary Key, valueColumn INT)", true);
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id INT Primary Key, valueColumn INT, bar_id INT, " +
                "CONSTRAINT fk FOREIGN KEY (bar_id) REFERENCES Bar(id) )", true);

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += ControllerConstants.BASE_PATH;

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.INFO_SUT_PATH)
                    .then()
                    .statusCode(200);

            QueryResult res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(0, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            startNewTest(url);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (0, 0)", true);
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (0, 0, 0)", true);

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(1, res.seeRows().size());

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(0, res.seeRows().size());
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (1, 1);", true);
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(1, res.seeRows().size());

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200);

            startNewTest(url);

            // Man: we might need to clean Foo as well, since now it refers to invalid bar_id
            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Foo;", true);
            assertEquals(0, res.seeRows().size());

            res = EMSqlScriptRunner.execCommand(getConnection(), "SELECT * FROM Bar;", true);
            assertEquals(0, res.seeRows().size());

        } finally {
            starter.stop();
        }
    }
}
