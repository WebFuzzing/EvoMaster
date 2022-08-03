package org.evomaster.e2etests.spring.examples.wiremock.service;

import com.foo.rest.examples.spring.wiremock.service.ServiceController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExternalServiceMockingFlakyEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        ServiceController serviceController = new ServiceController();
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        SpringTestBase.initClass(serviceController,config);
    }

    @Test
    public void externalServiceMockingTest() throws Throwable {
        runTestHandlingFlaky(
                "ExternalServiceMockingEMTest",
                "org.bar.ExternalServiceMockingEMTest",
                500,
                false,
                (args) -> {
                    // IP set to 127.0.0.5 to confirm the test failure
                    // Use USER for external service IP selection strategy
                    // when running on a personal computer if it's macOS
                    // TODO: When running parallel tests it's always good select
                    //  Random as strategy.
                    args.add("--externalServiceIPSelectionStrategy");
                    args.add("USER");
                    args.add("--externalServiceIP");
                    args.add("127.0.0.5");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/wiremock/external", "false");
                    // TODO: Disabled till the Jackson method replacement handled to unmarshall the JSON
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external/json", "false");
                });
    }
}