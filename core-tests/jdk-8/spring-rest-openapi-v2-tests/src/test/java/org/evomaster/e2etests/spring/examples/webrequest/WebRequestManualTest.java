package org.evomaster.e2etests.spring.examples.webrequest;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

public class WebRequestManualTest extends WebRequestTestBase {

    @Test
    public void testParams(){

        given().accept("*/*")
                .get(baseUrlOfSut + "/api/webrequest")
                .then()
                .statusCode(200)
                .body(containsString("FALSE"));

        given().accept("*/*")
                .param("a", "foo")
                .param("b", "bar")
                .get(baseUrlOfSut + "/api/webrequest")
                .then()
                .statusCode(200)
                .body(containsString("TRUE"));
    }
}
