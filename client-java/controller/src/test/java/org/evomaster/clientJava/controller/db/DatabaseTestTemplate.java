package org.evomaster.clientJava.controller.db;

import io.restassured.http.ContentType;
import org.evomaster.clientJava.controller.InstrumentedSutStarter;
import org.evomaster.clientJava.controllerApi.dto.SutRunDto;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;

import static io.restassured.RestAssured.given;
import static org.evomaster.clientJava.controllerApi.ControllerConstants.BASE_PATH;
import static org.evomaster.clientJava.controllerApi.ControllerConstants.RUN_SUT_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DatabaseTestTemplate {

    /*
        Useful link:
        https://www.tutorialspoint.com/sql/index.htm
     */

    private static Connection connection;



    @BeforeAll
    public static void initClass() throws Exception {
        InstrumentingAgent.initP6Spy("org.h2.Driver");

        connection = DriverManager.getConnection("jdbc:p6spy:h2:mem:db_test", "sa", "");
    }

    @BeforeEach
    public void initTest() throws Exception {

        /*
            Not supported in H2
            SqlScriptRunner.execCommand(connection, "DROP DATABASE db_test;");
            SqlScriptRunner.execCommand(connection, "CREATE DATABASE db_test;");
        */

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;");
    }

    protected Connection getConnection(){
        return connection;
    }


    protected String start(InstrumentedSutStarter starter) {
        boolean started = starter.start();
        assertTrue(started);

        int port = starter.getControllerServerPort();

        startSut(port);

        return "http://localhost:" + port;
    }

    protected void startSut(int port) {
        given().contentType(ContentType.JSON)
                .body(new SutRunDto(true, false))
                .put("http://localhost:" + port + BASE_PATH + RUN_SUT_PATH)
                .then()
                .statusCode(204);
    }

    protected InstrumentedSutStarter getInstrumentedSutStarter() {
        DatabaseFakeSutController sutController = new DatabaseFakeSutController(connection);
        sutController.setControllerPort(0);
        return new InstrumentedSutStarter(sutController);
    }
}
