package org.evomaster.e2etests.spring.examples.webrequest;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

public class WebRequestEMTest extends WebRequestTestBase {


    @Test
    public void testRunEM() throws Throwable {

        handleFlaky(() -> {

            String[] args = new String[]{
                    "--createTests", "false",
                    "--seed", "42",
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "500",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/webrequest", "FALSE");
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/webrequest", "TRUE");
        });
    }
}
