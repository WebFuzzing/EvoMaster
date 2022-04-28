package org.evomaster.e2etests.spring.examples.wiremock.service;

import com.alibaba.dcm.DnsCacheManipulator;
import com.foo.rest.examples.spring.wiremock.http.HttpRequestController;
import com.foo.rest.examples.spring.wiremock.service.ServiceController;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ExternalServiceTest extends SpringTestBase {
    /**
     * Ignore this test
     */

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void initClass() throws Exception {

        DnsCacheManipulator.setDnsCache("foo.bar", "127.0.0.2");
        DnsCacheManipulator.setDnsCache("baz.bar", "127.0.0.2");

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

        wireMockServer.stubFor(get(urlMatching("/api/echo/([a-z]*)\\?x=([0-9]*)&y=([a-z]*)"))
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

        ServiceController serviceController = new ServiceController();
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        SpringTestBase.initClass(serviceController,config);
    }

    @AfterAll
    public static void shutdownServer() {
        wireMockServer.stop();
        DnsCacheManipulator.clearDnsCache();
    }

    @Test
    public void testRunEM() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "ExternalServiceEM",
                "org.bar.ExternalServiceEM",
                500,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external", "true");
                });
    }
}
