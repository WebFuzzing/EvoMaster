package org.evomaster.client.java.controller;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.api.dto.SutRunDto;
import org.evomaster.client.java.controller.internal.SutController;

import java.sql.Connection;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface DatabaseTestTemplate {

    /*
        Useful link:
        https://www.tutorialspoint.com/sql/index.htm
     */

    public Connection getConnection();

    default String start(InstrumentedSutStarter starter) {
        boolean started = starter.start();
        assertTrue(started);

        int port = starter.getControllerServerPort();

        startSut(port);

        return "http://localhost:" + port;
    }

    default void startSut(int port) {
        given().contentType(ContentType.JSON)
                .body(new SutRunDto(true, false, true))
                .put("http://localhost:" + port + BASE_PATH + RUN_SUT_PATH)
                .then()
                .statusCode(204);
    }

    default void startNewActionInSameTest(String url, int index){

        given().accept(ContentType.ANY)
                .contentType(ContentType.JSON)
                .body("{\"index\":" + index + "}")
                .put(url + NEW_ACTION)
                .then()
                .statusCode(204);
    }

    default void startNewTest(String url){

        given().accept(ContentType.ANY)
                .contentType(ContentType.JSON)
                .body(new SutRunDto(true, true, true))
                .put(url + RUN_SUT_PATH)
                .then()
                .statusCode(204);

        startNewActionInSameTest(url, 0);
    }


    public SutController getSutController();

    default InstrumentedSutStarter getInstrumentedSutStarter() {
        SutController sutController = getSutController();
        sutController.setControllerPort(0);
        return new InstrumentedSutStarter(sutController);
    }
}
