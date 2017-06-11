package org.evomaster.clientJava.controller.internal.db;

import io.restassured.http.ContentType;
import org.evomaster.clientJava.controller.InstrumentedSutStarter;
import org.evomaster.clientJava.controller.db.QueryResult;
import org.evomaster.clientJava.controller.db.SqlScriptRunner;
import org.evomaster.clientJava.controllerApi.dto.SutRunDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static io.restassured.RestAssured.given;
import static org.evomaster.clientJava.controllerApi.ControllerConstants.BASE_PATH;
import static org.evomaster.clientJava.controllerApi.ControllerConstants.EXTRA_HEURISTICS;
import static org.evomaster.clientJava.controllerApi.ControllerConstants.RUN_SUT_PATH;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseTest {

    /*
        Useful link:
        https://www.tutorialspoint.com/sql/index.htm
     */

    private static Connection connection;


    @BeforeAll
    public static void initClass() throws Exception{
        InstrumentedSutStarter.initP6Spy("org.h2.Driver");

        connection = DriverManager.getConnection("jdbc:p6spy:h2:mem:db_test", "sa", "");
    }

    @BeforeEach
    public void initTest() throws Exception{

        /*
            Not supported in H2
            SqlScriptRunner.execCommand(connection, "DROP DATABASE db_test;");
            SqlScriptRunner.execCommand(connection, "CREATE DATABASE db_test;");
        */

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;");
    }


    @Test
    public void testBase() throws Exception{

        SqlScriptRunner.execCommand(connection, "CREATE TABLE Foo(x INT)");

        QueryResult res = SqlScriptRunner.execCommand(connection, "select * from Foo");
        assertTrue(res.isEmpty());

        SqlScriptRunner.execCommand(connection, "INSERT INTO Foo (x) VALUES (4)");

        res = SqlScriptRunner.execCommand(connection, "select * from Foo");
        assertFalse(res.isEmpty());
    }

    @Test
    public void testHeuristic() throws Exception{

        SqlScriptRunner.execCommand(connection, "CREATE TABLE Foo(x INT)");
        SqlScriptRunner.execCommand(connection, "INSERT INTO Foo (x) VALUES (10)");

        DatabaseFakeSutController sutController = new DatabaseFakeSutController(connection);
        sutController.setControllerPort(0);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(sutController);

        try {
            boolean started = starter.start();
            assertTrue(started);

            int port = sutController.getControllerServerPort();

            //need to start fake SUT, otherwise DB driver does not get initialized
            given().contentType(ContentType.JSON)
                    .body(new SutRunDto(true,false))
                    .put("http://localhost:"+port+ BASE_PATH + RUN_SUT_PATH)
                    .then()
                    .statusCode(204);

            String url = "http://localhost:"+port+ BASE_PATH + EXTRA_HEURISTICS;

            given().accept(ContentType.JSON)
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("toMinimize.size()", is(0));

            SqlScriptRunner.execCommand(connection, "SELECT x FROM Foo WHERE x = 12");

            given().accept(ContentType.JSON)
                    .get(url)
                    .then()
                    .statusCode(200)
                    .body("toMinimize.size()", is(1))
                    .body("toMinimize[0]", greaterThan(0f));

            SqlScriptRunner.execCommand(connection, "SELECT x FROM Foo WHERE x = 10");

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

}
