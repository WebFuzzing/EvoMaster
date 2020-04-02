package org.evomaster.e2etests.spring.examples.testability;

import com.foo.rest.examples.spring.testability.TestabilityController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TestabilityEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TestabilityController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TestabilityEM",
                "org.bar.TestabilityEM",
                15_000,
                true,
                (args) -> {

                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/testability/{date}/{number}/{setting}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "ERROR");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "OK");
                },
                10);
    }
}