package org.evomaster.e2etests.spring.examples.escape;

import io.restassured.http.ContentType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.containsString;

public class EscapeManualTest extends EscapeTestBase {


    @Test
    public void testSwaggerJSON() {

        SutInfoDto dto = remoteController.getSutInfo();

        String swaggerJson = given().accept(Formats.JSON_V1)
                .get(dto.restProblem.swaggerJsonUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Swagger swagger = new SwaggerParser().parse(swaggerJson);

        assertEquals("/", swagger.getBasePath());
        assertEquals(2, swagger.getPaths().size());
    }

    @Test
    public void testContainsDollar(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsDollar/doesnt")
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsDollar/it%24ingdoes")
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", is("This contains $"));
    }
    @Test
    public void testContainsQuote(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsQuote/doesnt")
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsQuote/it%22ingdoes")
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", containsString("\""));
    }


    /*
    @Test
    public void testContainsQuote2(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsQuote/doesnt")
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsQuote/it%2F%2F%22ingdoes")
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", containsString("\""));
    }




    @Test
    public void testContainsSlash(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsSlash/doesnt")
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsSlash/it%5Cingdoes")
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", containsString("\\"))
                .body("response", containsString("%5C"));
    }

    @Test
    public void testContainsSingleQuote(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsSingleQuote/doesnt")
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsSingleQuote/it%27ingdoes")
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", containsString("\'"))
                .body("response", containsString("%27"));
    }
    */
}
