package org.evomaster.e2etests.spring.examples.wiremock.jackson;

import com.foo.rest.examples.spring.wiremock.jackson.JacksonWMController;
import org.evomaster.ci.utils.CIUtils;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JacksonEMFlakyTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        JacksonWMController jacksonWMController = new JacksonWMController();
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        SpringTestBase.initClass(jacksonWMController, config);
    }

    @Disabled
    public void externalServiceMockingTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "JacksonWMGeneratedTest",
                "org.bar.JacksonWMGeneratedTest",
                1000,
                !CIUtils.isRunningGA(),
                (args) -> {

                    // IP set to 127.0.0.5 to confirm the test failure
                    // Use USER for external service IP selection strategy
                    // when running on a personal computer if it's macOS
                    // Note: When running parallel tests it's always good select
                    //  Random as strategy.
                    args.add("--externalServiceIPSelectionStrategy");
                    args.add("USER");
                    args.add("--externalServiceIP");
                    args.add("127.0.0.5");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jackson/json", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jackson/byte/{s}", "true");
                    // TODO: There is no method replacement to capture the value in the URL constructor to spin
                    //  WireMock. So this will fail always.
//                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jackson/url", "true");
                },
                3);
    }
}
