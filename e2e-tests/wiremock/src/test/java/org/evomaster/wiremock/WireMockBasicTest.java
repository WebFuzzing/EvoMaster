package org.evomaster.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.foo.wiremock.WireMockApplication;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;


public class WireMockBasicTest {

    WireMockServer wireMockServer;

    private final String MOCK_RESPONSE = "Working";

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
                                .withBody(MOCK_RESPONSE)));
    }

    @Test
    public void testServiceStatus() {
        /// This test confirms that the WireMock is working
        given()
                .when()
                .get("http://localhost:" + wireMockServer.port() +"/api/call")
                .then()
                .assertThat()
                .statusCode(200)
                .body(is(MOCK_RESPONSE));
    }

    @Test
    public void testApplicationCall() {
        /**
         This test simulates the API call from the application to an external
         service in this case the URL for the external API is for the moment
         hardcoded */

        WireMockApplication wireMockApplication = new WireMockApplication();
        String results = wireMockApplication.callApi(wireMockServer.port());
        assertEquals(MOCK_RESPONSE, results, "Test passed");
    }
}
