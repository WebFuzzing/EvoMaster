package org.evomaster.e2etests.spring.examples.wiremock;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class WireMockManualTest extends WireMockTestBase{

    @Test
    public void testEqualsFoo() {

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/equalsFoo/bar")
                .then()
                .statusCode(200)
                .body("valid", is(false));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/equalsFoo/foo")
                .then()
                .statusCode(200)
                .body("valid", is(true));

    }

    @Test
    public void testExternalCall() {
        /**
         * The echo call only response the given input when it's only alpha
         * characters. The first call suppose to send false, since it has
         * numeric characters in it.
         * */
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/123")
                .then()
                .statusCode(200)
                .body("valid", is(false));
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/foo")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }
}
