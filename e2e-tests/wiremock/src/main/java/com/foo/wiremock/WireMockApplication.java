package com.foo.wiremock;


import static org.hamcrest.Matchers.is;
import static io.restassured.RestAssured.given;

public class WireMockApplication {

    public void callApi(int port) {
        given()
                .when()
                .get("http://localhost:" + port +"/api/call")
                .then()
                .assertThat()
                .statusCode(200)
                .body(is("Working"));
    }
}
