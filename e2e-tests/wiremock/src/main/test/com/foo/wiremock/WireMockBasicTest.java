package com.foo.wiremock;


import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;


public class WireMockBasicTest {

    WireMockServer wireMockServer;

    @BeforeEach
    public void initializeServer() {
        wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        wireMockServer.start();

        initializeEndPoint();
    }

    @AfterEach
    public void shutdownServer() {
        wireMockServer.stop();
    }

    public void initializeEndPoint() {
        wireMockServer.stubFor(get("/api/call")
                .willReturn(
                        aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withStatus(200)
                        .withBody("Working")));
    }

    @Test
    public void testServiceStatus() {

        given()
            .when()
                .get("http://localhost:" + wireMockServer.port() +"/api/call")
                .then()
                .assertThat()
                .statusCode(200)
                .body(is("Working"));
    }

    @Test
    public void testApplicationCall() {
        WireMockApplication wireMockApplication = new WireMockApplication();
        wireMockApplication.callApi(wireMockServer.port());
    }
}
