package org.evomaster.e2etests.spring.examples.testability;

import com.foo.rest.examples.spring.testability.TestabilityController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.ci.utils.CIUtils;
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
    public void testRunEM() throws Throwable {

        defaultSeed = 142;

        CIUtils.skipIfOnCircleCI();

        runTestHandlingFlakyAndCompilation(
                "TestabilityEM",
                "org.bar.TestabilityEM",
                15_000,
                true,
                (args) -> {
//                    args.add("--baseTaintAnalysisProbability");
//                    args.add("0.9");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("false");

                    args.add("--weightBasedMutationRate");
                    args.add("false");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("NONE");

                    args.add("--archiveGeneMutation");
                    args.add("NONE");

                    args.add("--probOfArchiveMutation");
                    args.add("0.0");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/testability/{date}/{number}/{setting}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "ERROR");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/testability/{date}/{number}/{setting}", "OK");
                },
                10);
    }
}