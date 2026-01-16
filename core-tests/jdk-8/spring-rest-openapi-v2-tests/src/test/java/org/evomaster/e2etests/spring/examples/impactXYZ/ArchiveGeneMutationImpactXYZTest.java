package org.evomaster.e2etests.spring.examples.impactXYZ;

import com.foo.rest.examples.spring.impactXYZ.ImpactXYZRestController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;


public class ArchiveGeneMutationImpactXYZTest extends SpringTestBase {

    @Test
    public void testRunEM() throws Throwable {

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "TestAGM",
                "org.impactxyz.TestAGM",
                8_000,
                true,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add("0.5");

                    args.add("--weightBasedMutationRate");
                    args.add("true");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("APPROACH_IMPACT");

                    args.add("--archiveGeneMutation");
                    args.add("SPECIFIED_WITH_SPECIFIC_TARGETS");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 500, "/api/impactxyz/{x}", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "NOT_MATCHED");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_1");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_2");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_3");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/impactxyz/{x}", "CREATED_4");

                }, 3);
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactXYZRestController(Arrays.asList("/api/impactdto/{x}")));
    }
}
