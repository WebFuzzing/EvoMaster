package org.evomaster.e2etests.spring.examples.expectations;

import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpectationsEMTest extends ExpectationsTestBase {

    @Test
    public void testRunEM() throws Throwable{

        runTestHandlingFlakyAndCompilation(
                "ExpectationsEM",
                "org.bar.ExpectationsEM",
                10_000,
                (args) -> {
                    args.add("--expectationsActive");
                    args.add("TRUE");


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/expectations/getExpectations/{b}", "True");

                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/expectations/getExpectations/{b}", "");
                }
        );
    }
}
