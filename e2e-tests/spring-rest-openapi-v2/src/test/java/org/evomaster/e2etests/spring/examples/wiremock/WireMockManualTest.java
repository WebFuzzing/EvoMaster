package org.evomaster.e2etests.spring.examples.wiremock;

import com.foo.rest.examples.spring.wiremock.WireMockController;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import io.restassured.http.ContentType;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class WireMockManualTest extends SpringTestBase {

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void initClass() throws Exception {
        // For the moment port is set to 10101
        wireMockServer = new WireMockServer(new WireMockConfiguration().port(52768).extensions(new ResponseTemplateTransformer(false)));
        wireMockServer.start();

        // WireMock endpoint will respond the third value of the request path
        wireMockServer.stubFor(get(urlMatching("/api/echo/([a-z]*)"))
                .atPriority(1)
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "text/plain")
                                .withStatus(200)
                                .withBody("{{request.path.[2]}}")
                                .withTransformers("response-template")));

        // to prevent from the 404 when no matching stub below stub is added
        wireMockServer.stubFor(get(urlMatching("/.*"))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Not found!!")));

        WireMockController wireMockController = new WireMockController();
        SpringTestBase.initClass(wireMockController);
    }

    @AfterEach
    public void shutdownServer() {
        wireMockServer.stop();
    }

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
//        given().accept(ContentType.JSON)
//                .get(baseUrlOfSut + "/api/wiremock/external/123")
//                .then()
//                .statusCode(200)
//                .body("valid", is(false));
        /**
         * Test modified to check whether the external call is a success or
         * not. If the target host replaced with the Wiremock, it'll respond
         * true otherwise false.
         * */
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external")
                .then()
                .statusCode(200)
                .body("valid", is(false));
    }
}
