package org.evomaster.e2etests.spring.examples.postcollection;

import com.foo.rest.examples.spring.postcollection.PostCollectionController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PCEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new PostCollectionController());
    }

    @Test
    public void testRunEM() throws Throwable {

        defaultSeed = 45;
        /*
            NOTE THAT
            default resource-based solution would have side effect on creating multiple resources in one test
            before size of resource is handled, we employ the solution without resource handling.

            man: now enable it with adaptive length handling with structure mutator
         */

        runTestHandlingFlakyAndCompilation(
                "PcEM",
                "org.bar.PcEM",
                1_000,
                (args) -> {

                    args.add("--probOfSmartSampling");
                    args.add("0.5");
                    args.add("--resourceSampleStrategy");
                    args.add("NONE");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 201);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200);
                });
    }
}
