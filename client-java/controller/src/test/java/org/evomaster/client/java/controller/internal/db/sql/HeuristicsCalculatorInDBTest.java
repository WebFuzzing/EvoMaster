package org.evomaster.client.java.controller.internal.db.sql;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import  io.restassured.RestAssured;
import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.evomaster.client.java.controller.api.ControllerConstants.TEST_RESULTS;
import org.evomaster.client.java.controller.api.ControllerConstants;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface HeuristicsCalculatorInDBTest extends DatabaseTestTemplate {


    @Test
    default void testHeuristic() throws Exception {

        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)", true);


        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            startNewTest(url);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (10)", false);

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS)
                    .then()
                    .statusCode(200)
                    .body("data.extraHeuristics.size()", is(1))
                    .body("data.extraHeuristics[0].heuristics.size()", is(0));

            startNewTest(url);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (10)", false);

            EMSqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 12", true);
            EMSqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 10", true);

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200)
                    .body("data.extraHeuristics.size()", Matchers.is(1))
                    .body("data.extraHeuristics[0].heuristics.size()", Matchers.is(2))
                    .body("data.extraHeuristics[0].heuristics[0].value", Matchers.greaterThan(0f))
                    .body("data.extraHeuristics[0].heuristics[1].value", Matchers.is(0f));

            startNewActionInSameTest(url, 1);

            EMSqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 13", true);

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS)
                    .then()
                    .statusCode(200)
                    .body("data.extraHeuristics.size()", Matchers.is(2))
                    .body("data.extraHeuristics[0].heuristics.size()", Matchers.is(2))
                    .body("data.extraHeuristics[0].heuristics[0].value", Matchers.greaterThan(0f))
                    .body("data.extraHeuristics[0].heuristics[1].value", Matchers.is(0f))
                    .body("data.extraHeuristics[1].heuristics.size()", Matchers.is(1))
                    .body("data.extraHeuristics[1].heuristics[0].value", Matchers.greaterThan(0f));
        } finally {
            starter.stop();
        }
    }

    @Test
    default void testMultiline() throws Exception {

        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT, y INT)", true);


        int y = 42;
        String select = "select f.x \n from Foo f \n where f.y=" + y;


        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += ControllerConstants.BASE_PATH;

            startNewTest(url);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (0, 0)", false);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double a = getFirstAndStartNew(url);
            assertTrue(a > 0d);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (1, " + y + ")", true);
            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double b = getFirstAndStartNew(url);
            assertTrue(b < a);
            assertEquals(0d, b, 0.0001);

        } finally {
            starter.stop();
        }
    }

    @Test
    default void testVarNotInSelect() throws Exception {

        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT, y INT)", true);


        int y = 42;
        String select = "select f.x from Foo f where f.y=" + y;


        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += ControllerConstants.BASE_PATH;

            startNewTest(url);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (0, 0)", false);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double a = getFirstAndStartNew(url);
            assertTrue(a > 0d);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (1, " + y + ")", true);
            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double b = getFirstAndStartNew(url);
            assertTrue(b < a);
            assertEquals(0d, b, 0.0001);

        } finally {
            starter.stop();
        }
    }

    @Test
    default void testInnerJoin() throws Exception {

        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(id INT Primary Key, valueColumn INT)", true);
        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id INT Primary Key, valueColumn INT, bar_id INT, " +
                "CONSTRAINT fk FOREIGN KEY (bar_id) REFERENCES Bar(id) )", true);



        int x = 10;
        int y = 20;

        String select = "select f.id, f.valueColumn, f.bar_id  from Foo f inner join Bar b on f.bar_id=b.id " +
                "where f.valueColumn=" + x + " and b.valueColumn=" + y + " limit 1";

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += ControllerConstants.BASE_PATH;

            startNewTest(url);


            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (0, 0)", false);
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (0, 0, 0)", false);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double a = getFirstAndStartNew(url);
            assertTrue(a > 0d);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (1, " + x + ", 0)", true);
            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double b = getFirstAndStartNew(url);
            assertTrue(b < a);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (1, " + y + ")", true);
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (2, 0, 1)", true);
            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double c = getFirstAndStartNew(url);
            assertTrue(c < b);

            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, valueColumn) VALUES (2, " + y + ")", true);
            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, valueColumn, bar_id) VALUES (3, " + x + ", 2)", true);
            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double d = getFirstAndStartNew(url);
            assertTrue(d < c);
            assertEquals(0d, d, 0.0001);

        } finally {
            starter.stop();
        }
    }


    default Double getFirstAndStartNew(String url) {

        double value = Double.parseDouble(RestAssured.given().accept(ContentType.JSON)
                .get(url + ControllerConstants.TEST_RESULTS)
                .then()
                .statusCode(200)
                .extract().body().path("data.extraHeuristics[0].heuristics[0].value").toString());

        startNewTest(url);

        return value;
    }

}
