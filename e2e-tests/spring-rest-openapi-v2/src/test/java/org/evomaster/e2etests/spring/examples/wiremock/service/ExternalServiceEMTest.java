package org.evomaster.e2etests.spring.examples.wiremock.service;

import com.alibaba.dcm.DnsCacheManipulator;
import com.foo.rest.examples.spring.wiremock.service.ServiceController;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.inject.Injector;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.external.service.ExternalServiceInfo;
import org.evomaster.core.problem.external.service.ExternalServices;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.service.ResourceSampler;
import org.evomaster.core.problem.rest.service.RestResourceFitness;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalServiceEMTest extends SpringTestBase {
    /**
     * Test to check the AdditionalInfoDto transaction between driver and core.
     *
     * ExecutionTracer reset the AdditionalInfoList several times during the start-up. As the
     * result, calls made during the start-up got captured and get cleared during this.
     *
     */

    private static WireMockServer wireMockServer;

    @BeforeAll
    public static void initClass() throws Exception {

        DnsCacheManipulator.setDnsCache("foo.bar", "127.0.0.2");
        DnsCacheManipulator.setDnsCache("baz.bar", "127.0.0.2");

        wireMockServer = new WireMockServer(new WireMockConfiguration().bindAddress("127.0.0.5").port(8080).extensions(new ResponseTemplateTransformer(false)));
        wireMockServer.start();

        /*
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
    public void externalServiceCallsCaptureTest() throws Throwable {
        String[] args = new String[]{
                "--createTests", "false",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "1",
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--executiveSummary", "false",
                "--expectationsActive" , "true",
                "--outputFormat", "JAVA_JUNIT_5",
                "--outputFolder", "target/em-tests/ExternalServiceEM"
        };

        Injector injector = init(Arrays.asList(args));

        ExternalServices externalServices = injector.getInstance(ExternalServices.class);

        RestResourceFitness restResourceFitness = injector.getInstance(RestResourceFitness.class);
        ResourceSampler resourceSampler = injector.getInstance(ResourceSampler.class);
        RestIndividual restIndividual = resourceSampler.sample();

        // asserts whether the call made during the start-up is captured
        assertEquals(1, externalServices.getExternalServices().size(), externalServices.getExternalServices().stream().map(ExternalServiceInfo::getRemoteHostname).collect(Collectors.joining(",")));
        assertEquals("baz.bar", externalServices.getExternalServices().get(0).getRemoteHostname());
        restResourceFitness.calculateCoverage(restIndividual, Collections.emptySet());

        // assertion after the execution
        assertEquals(2, externalServices.getExternalServices().size());

    }
}
