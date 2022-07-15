package org.evomaster.e2etests.spring.examples.wiremock.search;

import com.foo.rest.examples.spring.wiremock.search.SearchController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExternalServiceActionsEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SearchController searchController = new SearchController();
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        SpringTestBase.initClass(searchController, config);
    }

    @Test
    public void requestTransformationTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "ExternalServiceActionsEMTest",
                "org.bar.ExternalServiceActionsEMTest",
                100,
                (args) -> {

                    args.add("--externalServiceIPSelectionStrategy");
                    args.add("USER");
                    args.add("--externalServiceIP");
                    args.add("127.0.0.2");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/wiremock/search", "false");
                });
    }
}