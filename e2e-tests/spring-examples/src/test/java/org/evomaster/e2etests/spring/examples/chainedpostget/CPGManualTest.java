package org.evomaster.e2etests.spring.examples.chainedpostget;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.evomaster.clientJava.controllerApi.EMTestUtils.resolveLocation;
import static org.hamcrest.CoreMatchers.is;

public class CPGManualTest extends CPGTestBase {

    @Test
    public void testGet() {

        String location = given()
                .post(baseUrlOfSut + "/api/cpg/x")
                .then()
                .statusCode(201)
                .extract().header("location");

        int a = 42;
        int b = 77;

        location = resolveLocation(location, baseUrlOfSut + "/api/cpg/x/{id}/y");

        given().contentType(ContentType.JSON)
                .body("{\"a\":" + a + ", \"b\":" + b + "}")
                .post(location)
                .then()
                .statusCode(201);

        given().accept(ContentType.JSON)
                .get(location)
                .then()
                .statusCode(200)
                .body("a", is(a))
                .body("b", is(b));
    }
}
