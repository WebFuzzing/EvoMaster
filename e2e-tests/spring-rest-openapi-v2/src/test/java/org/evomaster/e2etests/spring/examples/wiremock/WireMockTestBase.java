package org.evomaster.e2etests.spring.examples.wiremock;

import com.foo.rest.examples.spring.wiremock.WireMockController;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockTestBase extends SpringTestBase {

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void initClass() throws Exception {
        // For the moment port is set to 10101
        wireMockServer = new WireMockServer(new WireMockConfiguration().port(10101).extensions(new ResponseTemplateTransformer(false)));
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
}
