package org.evomaster.e2etests.spring.examples.wiremock.service;

import com.foo.rest.examples.spring.wiremock.service.ServiceController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.resource.RestResourceCalls;
import org.evomaster.core.search.Action;
import org.evomaster.core.search.ActionFilter;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Need update code after refactoring")
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

                    // The below block of code is an experiment: Ignore
                    List<Action> actions = new ArrayList<>();
                    for (EvaluatedIndividual<RestIndividual> individual : solution.getIndividuals()) {
                        for (RestResourceCalls call : individual.getIndividual().getResourceCalls()) {
                            actions.addAll(call.seeActions(ActionFilter.ONLY_EXTERNAL_SERVICE));
                        }
                    }
                    assertEquals(actions.size(), 14);
                    // End block

                    // TODO: Multiple calls to the same service test casuses problems. Will be implmented
                    //  separatley.

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/wiremock/external", "false");

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external/complex", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/wiremock/external/complex", "false");
                    // TODO: Disabled till the Jackson method replacement handled to unmarshall the JSON
//                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/external/json", "false");
                });
    }
}