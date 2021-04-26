package org.evomaster.e2etests.spring.examples.headerlocation;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HLEMTest extends HLTestBase {


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "HeaderLocationEM",
                "org.bar.HeaderLocationEM",
                10_000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //easy cases
                    assertNone(solution, HttpVerb.POST, 404);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 404);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404);
                    assertHasAtLeastOne(solution, HttpVerb.PATCH, 404);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 201);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 201);

                    //need smart sampling
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 204);
                    assertHasAtLeastOne(solution, HttpVerb.PATCH, 204);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 204);
                });
    }
}
