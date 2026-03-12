package org.evomaster.e2etests.spring.examples.expectations;

import com.foo.rest.examples.spring.expectations.ExpectationsController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpectationsEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception{

        SpringTestBase.initClass(new ExpectationsController());
    }

    @Test
    public void testRunEM() throws Throwable{

        runTestHandlingFlakyAndCompilation(
                "ExpectationsEM",
                "org.bar.ExpectationsEM",
                10_000,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/basicResponsesNumeric/{value}", "42");

                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/basicResponsesNumeric/{value}", "");
                }
        );
    }
}
