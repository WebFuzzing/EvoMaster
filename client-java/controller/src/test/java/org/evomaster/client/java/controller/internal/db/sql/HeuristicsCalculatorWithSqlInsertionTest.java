package org.evomaster.client.java.controller.internal.db.sql;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.ControllerConstants;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.evomaster.client.java.controller.api.ControllerConstants.TEST_RESULTS;
import static org.evomaster.client.java.sql.dsl.SqlDsl.sql;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface HeuristicsCalculatorWithSqlInsertionTest extends DatabaseTestTemplate {


    @Test
    default void testHeuristic() throws Exception {

        EMSqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)", true);


        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH;

            startNewTest(url);

            List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                    .d("x", "10")
                    .dtos();
            executeSqlCommand(insertions, url);
//            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (10)", false);

            given().accept(ContentType.JSON)
                    .get(url + TEST_RESULTS + "?queryFromDatabase=false")
                    .then()
                    .statusCode(200)
                    .body("data.extraHeuristics.size()", is(1))
                    .body("data.extraHeuristics[0].heuristics.size()", is(0));

            startNewTest(url);

            executeSqlCommand(insertions, url);
//            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (10)", false);

            EMSqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 12", true);
            EMSqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 10", true);

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS + "?queryFromDatabase=false")
                    .then()
                    .statusCode(200)
                    .body("data.extraHeuristics.size()", Matchers.is(1))
                    .body("data.extraHeuristics[0].heuristics.size()", Matchers.is(2))
                    .body("data.extraHeuristics[0].heuristics[0].value", Matchers.greaterThan(0f))
                    .body("data.extraHeuristics[0].heuristics[1].value", Matchers.is(0f));

            startNewActionInSameTest(url, 1);

            EMSqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 13", true);

            RestAssured.given().accept(ContentType.JSON)
                    .get(url + ControllerConstants.TEST_RESULTS + "?queryFromDatabase=false")
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

            List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                    .d("x", "0")
                    .d("y", "0")
                    .dtos();
            executeSqlCommand(insertions, url);
//            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (0, 0)", false);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double a = getFirstAndStartNew(url);
            assertTrue(a > 0d);

            List<InsertionDto> insertions2 = sql().insertInto("Foo", 2L)
                    .d("x", "0")
                    .d("y", "0")
                    .and().insertInto("Foo", 3L)
                    .d("x", "1")
                    .d("y", ""+y)
                    .dtos();
            executeSqlCommand(insertions2, url);
//            EMSqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (1, " + y + ")", true);
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

            List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                    .d("x", "0")
                    .d("y", "0")
                    .dtos();
            executeSqlCommand(insertions, url);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double a = getFirstAndStartNew(url);
            assertTrue(a > 0d);

            insertions = sql()
                    .insertInto("Foo", 2L)
                    .d("x", "0")
                    .d("y", "0")
                    .and().insertInto("Foo", 3L)
                    .d("x", "1")
                    .d("y", ""+y)
                    .dtos();
            executeSqlCommand(insertions, url);

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


            List<InsertionDto> insertions = sql().insertInto("Bar", 1L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .and().insertInto("Foo", 2L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .d("bar_id", "0")
                    .dtos();
            executeSqlCommand(insertions, url);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double a = getFirstAndStartNew(url);
            assertTrue(a > 0d);

            insertions = sql().insertInto("Bar", 3L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .and().insertInto("Foo", 4L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .d("bar_id", "0")
                    .and().insertInto("Foo", 5L)
                    .d("id", "1")
                    .d("valueColumn", ""+x)
                    .d("bar_id", "0")
                    .dtos();
            executeSqlCommand(insertions, url);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double b = getFirstAndStartNew(url);
            assertTrue(b < a);

            insertions = sql().insertInto("Bar", 6L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .and().insertInto("Foo", 7L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .d("bar_id", "0")
                    .and()
                    .insertInto("Bar", 8L)
                    .d("id", "1")
                    .d("valueColumn", ""+y)
                    .and().insertInto("Foo", 9L)
                    .d("id", "2")
                    .d("valueColumn", "0")
                    .d("bar_id", "1")
                    .dtos();
            executeSqlCommand(insertions, url);

            EMSqlScriptRunner.execCommand(getConnection(), select, true);

            double c = getFirstAndStartNew(url);
            assertTrue(c < b);

            insertions = sql()
                    .insertInto("Bar", 10L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .and().insertInto("Foo", 11L)
                    .d("id", "0")
                    .d("valueColumn", "0")
                    .d("bar_id", "0")
                    .and().insertInto("Bar", 12L)
                    .d("id", "2")
                    .d("valueColumn", ""+y)
                    .and().insertInto("Foo", 13L)
                    .d("id", "3")
                    .d("valueColumn", ""+x)
                    .d("bar_id", "2")
                    .dtos();
            executeSqlCommand(insertions, url);

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
                .get(url + ControllerConstants.TEST_RESULTS + "?queryFromDatabase=false")
                .then()
                .statusCode(200)
                .extract().body().path("data.extraHeuristics[0].heuristics[0].value").toString());

        startNewTest(url);

        return value;
    }

}
