package org.evomaster.e2etests.spring.examples.namedresource;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcand on 01.03.17.
 */
public class NREMTest extends NRTestBase {

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "NrEM",
                "org.bar.NrEM",
                3_000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //easy cases
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