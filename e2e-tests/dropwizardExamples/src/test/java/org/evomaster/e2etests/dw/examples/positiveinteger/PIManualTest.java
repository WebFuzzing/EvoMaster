package org.evomaster.e2etests.dw.examples.positiveinteger;

import com.foo.rest.examples.positiveinteger.PostDto;
import io.restassured.http.ContentType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.clientJava.controller.EmbeddedStarter;
import org.evomaster.clientJava.controller.RestController;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.SutInfoDto;
import org.evomaster.core.problem.rest.RemoteController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PIManualTest extends PITestBase{


    @Test
    public void testSwaggerJSON() {

        SutInfoDto dto = remoteController.getInfo();

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
