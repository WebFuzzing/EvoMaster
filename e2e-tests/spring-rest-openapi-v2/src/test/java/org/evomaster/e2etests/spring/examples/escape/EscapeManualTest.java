package org.evomaster.e2etests.spring.examples.escape;

import com.foo.rest.examples.spring.escapes.EscapeResponseDto;
import io.restassured.http.ContentType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.containsString;

public class EscapeManualTest extends EscapeTestBase {


    @Test
    public void testSwaggerJSON() {

        SutInfoDto dto = remoteController.getSutInfo();

        String swaggerJson = given().accept(Formats.JSON_V1)
                .get(dto.restProblem.openApiUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Swagger swagger = new SwaggerParser().parse(swaggerJson);

        assertEquals("/", swagger.getBasePath());
        assertEquals(7, swagger.getPaths().size());
    }

    @Test
    public void testContainsDollar(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsDollar/" + false)
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsDollar/" + true)
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", is("This contains $"));
    }
    @Test
    public void testContainsQuote(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsQuote/" + false)
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsQuote/" + true)
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", containsString("\""));
    }

    @Test
    public void testEmptyBody(){
        given().accept("*/*")
                .contentType("text/plain")
                .body("d86zL")
                .post(baseUrlOfSut + "/api/escape/emptyBody")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body(containsString("0"));

        given().accept("*/*")
                .contentType("text/plain")
                .body("\"\"")
                .post(baseUrlOfSut + "/api/escape/emptyBody")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body(containsString("1"));
    }

    @Test
    public void testJsonBody(){
        EscapeResponseDto dto = new EscapeResponseDto();
        dto.response = "someResponse";
        dto.valid = true;

        given().accept("*/*")
                .contentType("application/json")
                .body(dto)
                .post(baseUrlOfSut + "/api/escape/jsonBody")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body(containsString("2"));


    }

    @Test
    public void testTrickyJson(){
        String s = "DontMindMe";
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/trickyJson/" + s)
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("'Content'", containsString(s))
                .body("'Tricky-dash'", containsString("You decide"))
                .body("'Tricky.dot'", containsString("you're pushing it"));

    }

    @Test
    public void testSlash(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsSlash/" + false)
                .then()
                .statusCode(200)
                .body("valid", is(false))
                .body("response", is("Nope"));


        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/containsSlash/" + true)
                .then()
                .statusCode(200)
                .body("valid", is(true))
                .body("response", is("This contains \\"));
    }

    @Test
    public void testEscapesJson(){
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/escape/escapesJson/" + true)
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("", hasItems("$-test", "\\-test", "\"-test"));

    }
}
