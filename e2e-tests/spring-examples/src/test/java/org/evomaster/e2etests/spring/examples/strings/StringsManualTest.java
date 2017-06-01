package org.evomaster.e2etests.spring.examples.strings;

import io.restassured.http.ContentType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.clientJava.controllerApi.Formats;
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringsManualTest extends StringsTestBase {


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
        assertEquals(3, swagger.getPaths().size());
    }

    @Test
    public void testEqualsFoo() {

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/equalsFoo/bar")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/equalsFoo/foo")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Test
    public void testStartEnds() {

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/startEnds/foo")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/startEnds/bar")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/startEnds/barfoo")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/startEnds/X12Y")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }


    @Test
    public void testContains() {

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/contains/foo")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/contains/12")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/contains/1234")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/strings/contains/456")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }
}
