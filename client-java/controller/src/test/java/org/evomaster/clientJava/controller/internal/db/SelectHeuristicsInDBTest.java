package org.evomaster.clientJava.controller.internal.db;

import io.restassured.http.ContentType;
import org.evomaster.clientJava.controller.InstrumentedSutStarter;
import org.evomaster.clientJava.controller.db.DataRow;
import org.evomaster.clientJava.controller.db.DatabaseTestTemplate;
import org.evomaster.clientJava.controller.db.QueryResult;
import org.evomaster.clientJava.controller.db.SqlScriptRunner;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.evomaster.clientJava.controllerApi.ControllerConstants.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class SelectHeuristicsInDBTest extends DatabaseTestTemplate {




    @Test
    public void testBase() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");

        QueryResult res = SqlScriptRunner.execCommand(getConnection(), "select * from Foo");
        assertTrue(res.isEmpty());

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (4)");

        res = SqlScriptRunner.execCommand(getConnection(), "select * from Foo");
        assertFalse(res.isEmpty());
    }


    @Test
    public void testParentheses() throws Exception{

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (5)");

        QueryResult res = SqlScriptRunner.execCommand(getConnection(), "select * from Foo where x = (5)");
        assertFalse(res.isEmpty());
    }


    @Test
    public void testConstants() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (4)");

        String select = "select x, 1 as y, null as z, 'bar' as w from Foo";

        QueryResult res = SqlScriptRunner.execCommand(getConnection(), select);
        assertFalse(res.isEmpty());

        DataRow row = res.seeRows().get(0);
        assertEquals(4, row.getValue(0));
        assertEquals(1, row.getValue(1));
        assertEquals(null, row.getValue(2));
        assertEquals("bar", row.getValue(3));
    }

    @Test
    public void testNested() throws Exception{

        String select = "select t.a, t.b from (select x as a, 1 as b from Foo where x<10) t where a>3";

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (1)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (4)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (7)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (20)");

        QueryResult res = SqlScriptRunner.execCommand(getConnection(), select);
        assertEquals(2, res.size());
    }


    @Test
    public void testHeuristic() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x) VALUES (10)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH + EXTRA_HEURISTICS;

            given().accept(ContentType.JSON)
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("toMinimize.size()", is(0));

            SqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 12");

            given().accept(ContentType.JSON)
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("toMinimize.size()", is(1))
                    .body("toMinimize[0]", greaterThan(0f));

            SqlScriptRunner.execCommand(getConnection(), "SELECT x FROM Foo WHERE x = 10");

            given().accept(ContentType.JSON)
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("toMinimize.size()", is(2))
                    .body("toMinimize[0]", greaterThan(0f))
                    .body("toMinimize[1]", is(0f));

            given().delete(url)
                    .then()
                    .statusCode(204);

            given().accept(ContentType.JSON)
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("toMinimize.size()", is(0));

        } finally {
            starter.stop();
        }
    }


    @Test
    public void testVarNotInSelect() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(x INT, y INT)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (0, 0)");

        int y = 42;
        String select = "select f.x from Foo f where f.y=" + y;


        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH + EXTRA_HEURISTICS;

            SqlScriptRunner.execCommand(getConnection(), select);

            double a = getFirstAndDelete(url);
            assertTrue(a > 0d);

            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (x, y) VALUES (1, " + y + ")");
            SqlScriptRunner.execCommand(getConnection(), select);

            double b = getFirstAndDelete(url);
            assertTrue(b < a);
            assertEquals(0d, b, 0.0001);

        } finally {
            starter.stop();
        }
    }

    @Test
    public void testInnerJoin() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Bar(id INT Primary Key, value INT)");
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE Foo(id INT Primary Key, value INT, bar_id INT, " +
                "CONSTRAINT fk FOREIGN KEY (bar_id) REFERENCES Bar(id) )");

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, value) VALUES (0, 0)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, value, bar_id) VALUES (0, 0, 0)");

        int x = 10;
        int y = 20;

        String select = "select f.id, f.value, f.bar_id  from Foo f inner join Bar b on f.bar_id=b.id " +
                "where f.value=" + x + " and b.value=" + y + " limit 1";

        InstrumentedSutStarter starter = getInstrumentedSutStarter();

        try {
            String url = start(starter);
            url += BASE_PATH + EXTRA_HEURISTICS;

            SqlScriptRunner.execCommand(getConnection(), select);

            double a = getFirstAndDelete(url);
            assertTrue(a > 0d);

            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, value, bar_id) VALUES (1, " + x + ", 0)");
            SqlScriptRunner.execCommand(getConnection(), select);

            double b = getFirstAndDelete(url);
            assertTrue(b < a);

            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, value) VALUES (1, " + y + ")");
            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, value, bar_id) VALUES (2, 0, 1)");
            SqlScriptRunner.execCommand(getConnection(), select);

            double c = getFirstAndDelete(url);
            assertTrue(c < b);

            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Bar (id, value) VALUES (2, " + y + ")");
            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO Foo (id, value, bar_id) VALUES (3, " + x + ", 2)");
            SqlScriptRunner.execCommand(getConnection(), select);

            double d = getFirstAndDelete(url);
            assertTrue(d < c);
            assertEquals(0d, d, 0.0001);

        } finally {
            starter.stop();
        }
    }



    private Double getFirstAndDelete(String url) {
        double value = Double.parseDouble(given().accept(ContentType.JSON)
                .get(url)
                .then()
                .statusCode(200)
                .extract().body().path("toMinimize[0]").toString());
        given().delete(url).then().statusCode(204);

        return value;
    }

}
