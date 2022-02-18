package com.foo.wiremock;


import static org.hamcrest.Matchers.is;
import static io.restassured.RestAssured.given;

public class WireMockApplication {

    public String callApi(int port) {
        return given()
                .when()
                .get("http://localhost:" + port +"/api/call")
                .body().asString();
    }
}
