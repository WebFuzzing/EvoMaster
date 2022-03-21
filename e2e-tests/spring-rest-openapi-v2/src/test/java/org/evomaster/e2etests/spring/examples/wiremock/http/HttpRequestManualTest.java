package org.evomaster.e2etests.spring.examples.wiremock.http;

import com.alibaba.dcm.DnsCacheManipulator;
import com.foo.rest.examples.spring.wiremock.http.HttpRequestController;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import io.restassured.http.ContentType;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class HttpRequestManualTest extends SpringTestBase {

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void initClass() throws Exception {
        DnsCacheManipulator.setDnsCache("foo.bar", "127.0.0.2");

        wireMockServer = new WireMockServer(new WireMockConfiguration().bindAddress("127.0.0.2").port(8080).extensions(new ResponseTemplateTransformer(false)));
        wireMockServer.start();

        /**
         * WireMock endpoint will respond the third value of the request path
         * as JSON response.
         * */
        wireMockServer.stubFor(get(urlMatching("/api/echo/([a-z]*)"))
                .atPriority(1)
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(200)
                                .withBody("{\"message\": \"{{request.path.[2]}}\"}")
                                .withTransformers("response-template")));

        // to prevent from the 404 when no matching stub below stub is added
        wireMockServer.stubFor(get(urlMatching("/.*"))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Not found!!")));

        HttpRequestController httpRequestController = new HttpRequestController();
        SpringTestBase.initClass(httpRequestController);
    }

    @AfterAll
    public static void shutdownServer() {
        wireMockServer.stop();
        DnsCacheManipulator.clearDnsCache();
    }

    @Test
    public void testURLConnection() {
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/url")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Disabled
    public void testHttpClient() {
        // Note: Test for Java 11 HttpClient not implemented

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/http")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Test
    public void testApacheHttpClient() {
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/apache")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Disabled
    public void testOkHttpClient() {
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/okhttp")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }
}
