package org.evomaster.e2etests.spring.examples.wiremock.base;

import com.alibaba.dcm.DnsCacheManipulator;
import com.foo.rest.examples.spring.wiremock.base.WireMockController;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import io.restassured.http.ContentType;
import org.evomaster.ci.utils.CIUtils;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.AfterAll;
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
        WireMockController wireMockController = new WireMockController();
        SpringTestBase.initClass(wireMockController);

        // DNS cache manipulator sets the IP for foo.bar to a different loopback address
        DnsCacheManipulator.setDnsCache("foo.bar", "127.0.0.3");

        /*
         * For the moment port is set to 8080, under a different loopback address
         * Ports 80 and 443 can be set, but require sudo permission, so application
         * should run as root
         * */
        wireMockServer = new WireMockServer(new WireMockConfiguration().bindAddress("127.0.0.3").port(8080).extensions(new ResponseTemplateTransformer(false)));
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

    }

    @AfterAll
    public static void shutdownServer() {
        wireMockServer.stop();
        DnsCacheManipulator.clearDnsCache();
    }

    @Test
    public void testEqualsFoo() {

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/equalsFoo/bar")
                .then()
                .statusCode(200)
                .body(is("false"));

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/equalsFoo/foo")
                .then()
                .statusCode(200)
                .body(is("true"));

    }

    @Test
    public void testExternalCall() {

        /*
         * The test will check whether the external call is a success or
         * not. If the target host replaced with the Wiremock, it'll respond
         * true otherwise false.
         * */
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external")
                .then()
                .statusCode(200)
                .body(is("true"));
    }
}
