package org.evomaster.e2etests.spring.examples.taintnested;

import com.foo.rest.examples.spring.taintnested.TaintNestedController;
import org.evomaster.ci.utils.CIUtils;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaintNestedEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TaintNestedController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TaintNestedEM",
                "org.bar.TaintNestedEM",
                5_000,
                (args) -> {
                    args.add("--taintForceSelectionOfGenesWithSpecialization");
                    args.add("true");
                    args.add("--discoveredInfoRewardedInFitness");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "A");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "B");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "C");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "D");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "E");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "F");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "G");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "H");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "I");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "L");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/taintnested", "GOT IT!!!");
                });
    }
}