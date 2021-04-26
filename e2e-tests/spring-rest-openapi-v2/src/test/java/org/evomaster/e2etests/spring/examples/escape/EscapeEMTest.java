package org.evomaster.e2etests.spring.examples.escape;

import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EscapeEMTest extends EscapeTestBase {

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "EscapeEM",
                "org.bar.EscapeEM",
                10_000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/escape/containsDollar/{s}", "false");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/escape/containsQuote/{s}", "false");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "api/escape/emptyBody", "0");


                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/escape/containsDollar/{s}", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/escape/containsQuote/{s}", "true");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "api/escape/emptyBody", "1");

                    // a quick assertion on the dash and dot json problem
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "api/escape/trickyJson/{s}", "You decide");


                });
    }
}
