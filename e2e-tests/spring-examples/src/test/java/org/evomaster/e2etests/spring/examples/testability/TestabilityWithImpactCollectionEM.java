package org.evomaster.e2etests.spring.examples.testability;

import com.foo.rest.examples.spring.testability.TestabilityController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzh on 2020-07-01
 */
public class TestabilityWithImpactCollectionEM extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TestabilityController());
    }

    @Test
    void testRunEM() throws Throwable {

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "TestabilityEM",
                "org.bar.TestabilityWithImpactCollectionEM",
                15_000,
                true,
                (args) -> {

                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--doCollectImpact");
                    args.add("true");

                    args.add("--saveImpactAfterMutation");
                    args.add("true");
                    args.add("--impactAfterMutationFile");
                    args.add("target/TestabilityWithImpactCollectionEM/impactInfo.csv");

                    args.add("--saveMutationInfo");
                    args.add("true");
                    args.add("--mutatedGeneFile");
                    args.add("target/TestabilityWithImpactCollectionEM/mutatedWithImpact.csv");


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    /*
                        there seem exist some dependency among tests. After executing the first test, SUT fails to throw the exception in following two tests.
                     */
                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/testability/{date}/{number}/{setting}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "ERROR");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "OK");
                },
                20);
    }
}
