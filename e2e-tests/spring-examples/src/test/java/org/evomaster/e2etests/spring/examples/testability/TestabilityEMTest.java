package org.evomaster.e2etests.spring.examples.testability;

import com.foo.rest.examples.spring.testability.TestabilityController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.e2etests.utils.CIUtils;
import org.junit.jupiter.api.BeforeAll;
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
    public void testWithDefault() throws Throwable {
        CIUtils.skipIfOnCircleCI();

        testRunEM("FIRST_NOT_COVERED_TARGET", "");
    }

    @Test
    public void testWithExpand() throws Throwable {
        CIUtils.skipIfOnCircleCI();

        testRunEM("EXPANDED_UPDATED_NOT_COVERED_TARGET", "Expand");
    }

    @Test
    public void testWithUpdated() throws Throwable {
        CIUtils.skipIfOnCircleCI();

        testRunEM("UPDATED_NOT_COVERED_TARGET", "Update");
    }

    private void testRunEM(String mutationTargetsSelectionStrategy, String packageSuffix) throws Throwable {

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "TestabilityEM",
                "org.bar.TestabilityEM"+packageSuffix,
                15_000,
                true,
                (args) -> {

                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    args.add("--mutationTargetsSelectionStrategy");
                    args.add(mutationTargetsSelectionStrategy);

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    /*
                        there seem exist some dependency among tests. After executing the first test, SUT fails to throw the exception in following two tests.
                     */
                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/testability/{date}/{number}/{setting}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "ERROR");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "OK");
                },
                10);
    }
}