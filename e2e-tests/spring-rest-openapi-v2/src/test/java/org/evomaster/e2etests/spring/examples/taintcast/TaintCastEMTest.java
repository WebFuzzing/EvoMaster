package org.evomaster.e2etests.spring.examples.taintcast;

import com.alibaba.dcm.DnsCacheManipulator;
import com.foo.rest.examples.spring.taintcast.TaintCastController;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaintCastEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        TaintCastController taintCastController = new TaintCastController();
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        SpringTestBase.initClass(taintCastController, config);
    }

    @Test
    public void testRunEM() throws Throwable {
        WireMockConfiguration wmConfig = new WireMockConfiguration()
                .bindAddress("127.0.0.1")
                .port(13579)
                .extensions(new ResponseTemplateTransformer(false));

        WireMockServer wm = new WireMockServer(wmConfig);
        wm.start();
        wm.stubFor(WireMock.post(WireMock.urlEqualTo("/api/fetch"))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(200)
                        .withBody("{\"results\": [{\"id\": \"9fLSZFMq\",\"name\": \"sample-1\"},{\"id\": \"9fLSZFMq\",\"name\": \"sample-1\"}]}")));

        DnsCacheManipulator.setDnsCache("mock.int", "127.0.0.1");

        runTestHandlingFlakyAndCompilation(
                "TaintCastEM",
                "org.bar.TaintCastEM",
                1000,
                (args) -> {
                    args.add("--externalServiceIPSelectionStrategy");
                    args.add("USER");
                    args.add("--externalServiceIP");
                    args.add("127.0.0.2");
                    args.add("--instrumentMR_NET");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taint/cast", "OK");
                });

        wm.shutdown();
        DnsCacheManipulator.clearDnsCache();
    }
}
