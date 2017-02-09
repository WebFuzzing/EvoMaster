package org.evomaster.e2etests.spring.examples.positiveinteger;


import com.foo.rest.examples.spring.positiveinteger.PostDto;
import io.restassured.http.ContentType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PIManualTest extends PITestBase{


    @Test
    public void testSwaggerJSON() {

        SutInfoDto dto = remoteController.getSutInfo();

        String swaggerJson = given().accept(Formats.JSON_V1)
                .get(dto.swaggerJsonUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Swagger swagger = new SwaggerParser().parse(swaggerJson);

        assertEquals("/", swagger.getBasePath());
        assertEquals(2, swagger.getPaths().size());
    }

    @Test
    public void testPost() {

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(new PostDto(5))
                .post(baseUrlOfSut + "/api/pi")
                .then()
                .statusCode(200)
                .body("isPositive", is(true));

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(new PostDto(-5))
                .post(baseUrlOfSut + "/api/pi")
                .then()
                .statusCode(200)
                .body("isPositive", is(false));
    }

    @Test
    public void testGet() {

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/pi/4")
                .then()
                .statusCode(200)
                .body("isPositive", is(true));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/pi/-4")
                .then()
                .statusCode(200)
                .body("isPositive", is(false));
    }

}
