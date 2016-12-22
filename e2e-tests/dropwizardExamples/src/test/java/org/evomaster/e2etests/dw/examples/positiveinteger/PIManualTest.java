package org.evomaster.e2etests.dw.examples.positiveinteger;

import com.foo.rest.examples.positiveinteger.PostDto;
import io.restassured.http.ContentType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.clientJava.controller.EmbeddedStarter;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.SutInfoDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PIManualTest {

    private static EmbeddedStarter embeddedStarter;
    private static String baseUrl;

    @BeforeAll
    public static void initClass() {

        PIController controller = new PIController();
        embeddedStarter = new EmbeddedStarter(controller);
        embeddedStarter.start();

        int port = embeddedStarter.getControllerServerJettyPort();

        given().post("http://localhost:" + port + "/controller/api/startSUT")
                .then()
                .statusCode(204);

        SutInfoDto dto = given()
                .accept(Formats.JSON_V1)
                .get("http://localhost:" + port + "/controller/api/infoSUT")
                .then()
                .statusCode(200)
                .extract().as(SutInfoDto.class);

        baseUrl = dto.baseUrlOfSUT;
        assertNotNull(baseUrl);
    }

    @AfterAll
    public static void tearDown() {

        int port = embeddedStarter.getControllerServerJettyPort();

        given().post("http://localhost:" + port + "/controller/api/stopSUT")
                .then()
                .statusCode(204);
    }

    @BeforeEach
    public void initTest() {

        int port = embeddedStarter.getControllerServerJettyPort();

        given().post("http://localhost:" + port + "/controller/api/resetSUT")
                .then()
                .statusCode(204);
    }


    @Test
    public void testSwaggerJSON() {

        int port = embeddedStarter.getControllerServerJettyPort();

        SutInfoDto dto = given()
                .accept(Formats.JSON_V1)
                .get("http://localhost:" + port + "/controller/api/infoSUT")
                .then()
                .statusCode(200)
                .extract().as(SutInfoDto.class);

        String swaggerJson = given().accept(Formats.JSON_V1)
                .get(dto.swaggerJsonUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Swagger swagger = new SwaggerParser().parse(swaggerJson);

        assertEquals("/api", swagger.getBasePath());
        assertEquals(2, swagger.getPaths().size());
    }

    @Test
    public void testPost() {

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(new PostDto(5))
                .post(baseUrl + "/api/pi")
                .then()
                .statusCode(200)
                .body("isPositive", is(true));

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(new PostDto(-5))
                .post(baseUrl + "/api/pi")
                .then()
                .statusCode(200)
                .body("isPositive", is(false));
    }

    @Test
    public void testGet() {

        given().accept(ContentType.JSON)
                .get(baseUrl + "/api/pi/4")
                .then()
                .statusCode(200)
                .body("isPositive", is(true));

        given().accept(ContentType.JSON)
                .get(baseUrl + "/api/pi/-4")
                .then()
                .statusCode(200)
                .body("isPositive", is(false));
    }

}
