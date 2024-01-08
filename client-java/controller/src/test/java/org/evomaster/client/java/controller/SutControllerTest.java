package org.evomaster.client.java.controller;

import io.restassured.RestAssured;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
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
        public List<AuthenticationDto> getInfoForAuthentication() {
            return null;
        }

        @Override
        public List<DbSpecification> getDbSpecifications() {
            return null;
        }


        @Override
        public ProblemInfo getProblemInfo() {
            return new RestProblem(SWAGGER_URL, null);
        }

        @Override
        public SutInfoDto.OutputFormat getPreferredOutputFormat() {
            return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
        }
    }

    private static EmbeddedSutController restController = new FakeRestController();

    @BeforeAll
    public static void initClass() {
        restController.setControllerPort(0);
        restController.startTheControllerServer();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = restController.getControllerServerPort();
        RestAssured.basePath = "/controller/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    public static void tearDown() {
        restController.stopSut();
    }

    @BeforeEach
    public void initTest() {
        if (restController.isSutRunning()) {
            restController.stopSut();
        }
    }


    @Test
    public void testNotRunning() {
        assertTrue(!restController.isSutRunning());

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("data.isSutRunning", is(false));
    }

    @Test
    public void testStartDirect() {

        restController.startSut();
        assertTrue(restController.isSutRunning());

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("data.isSutRunning", is(true));
    }

    @Test
    public void testStartRest() {

        restController.startSut();

        assertTrue(restController.isSutRunning());

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("data.isSutRunning", is(true));
    }


    @Test
    public void testGetSwaggerUrl() {

        given().accept(Formats.JSON_V1)
                .get("/infoSUT")
                .then()
                .statusCode(200)
                .body("data.restProblem.openApiUrl", is(SWAGGER_URL));
    }

    @Test
    public void testWarning() {

        String body = given().get("/")
                .then()
                .statusCode(400)
                .contentType(MediaType.TEXT_HTML)
                .extract().htmlPath().getString("");

        assertTrue(body.contains("WARNING"), "ERROR, received text:\n\n" + body);
    }
}