package org.evomaster.e2etests.spring.examples.strings;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringsEMTest extends StringsTestBase {

    @Test
    public void testRunEM() throws Throwable {

        handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "true",
                    "--seed", "42",
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "10000",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/equalsFoo/{s}", "false");
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/contains/{s}", "false");
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/startEnds/{s}", "false");

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/equalsFoo/{s}", "true");
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/contains/{s}", "true");
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/startEnds/{s}", "true");
        });
    }
}
