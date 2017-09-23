package org.evomaster.clientJava.controller;

import io.restassured.RestAssured;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SutControllerTest {

    private static final String SWAGGER_URL = "localhost:9999/swagger.json";

    private static class FakeRestController extends EmbeddedSutController {

        public boolean running;

        @Override
        public String startSut() {
            running = true;
            return null;
        }

        @Override
        public boolean isSutRunning() {
            return running;
        }

        @Override
        public void stopSut() {
            running = false;
        }

        @Override
        public String getPackagePrefixesToCover() {
            return null;
        }

        @Override
        public void resetStateOfSUT() {

        }

        @Override
        public String getUrlOfSwaggerJSON() {
            return SWAGGER_URL;
        }

        @Override
        public List<AuthenticationDto> getInfoForAuthentication() {
            return null;
        }

        @Override
        public Connection getConnection() {
            return null;
        }

        @Override
        public String getDatabaseDriverName() {
            return null;
        }

        @Override
        public List<String> getEndpointsToSkip() {
            return null;
        }


    }

    private static EmbeddedSutController restController = new FakeRestController();

    @BeforeAll
    public static void initClass(){
        restController.setControllerPort(0);
        restController.startTheControllerServer();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = restController.getControllerServerPort();
        RestAssured.basePath = "/controller/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    public static void tearDown(){
        restController.stopSut();
    }

    @BeforeEach
    public void initTest(){
        if(restController.isSutRunning()){
            restController.stopSut();
        }
    }


    @Test
    public void testNotRunning(){
        assertTrue(! restController.isSutRunning());

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("isSutRunning", is(false));
    }

    @Test
    public void testStartDirect(){

        restController.startSut();
        assertTrue(restController.isSutRunning());

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("isSutRunning", is(true));
    }

    @Test
    public void testStartRest(){

        restController.startSut();

        assertTrue(restController.isSutRunning());

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("isSutRunning", is(true));
    }


    @Test
    public void testGetSwaggerUrl(){

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("swaggerJsonUrl", is(SWAGGER_URL));
    }
}