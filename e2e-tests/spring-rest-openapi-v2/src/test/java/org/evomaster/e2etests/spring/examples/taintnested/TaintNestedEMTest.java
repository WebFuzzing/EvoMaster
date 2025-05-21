package org.evomaster.e2etests.spring.examples.taintnested;

import com.foo.rest.examples.spring.taintnested.TaintNestedController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
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
                10_000,
                6,
                (args) -> {
                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args,"discoveredInfoRewardedInFitness", "true");
                    setOption(args,"baseTaintAnalysisProbability", "0.9");
                    /*
                        After the fix in Gene.doInitialize to use requiresRandomInitialization(),
                        this test started failing.
                        looks like extra query params and headers would impact taint analysis.
                        but those are not needed for this SUT.
                        so, somehow their taints is impacting... but they disactivated early
                        in the search, so in theory they should have no major side effect here.
                        but they do :(
                        TODO remove following setting, and understand what's going on
                     */
                    setOption(args, "extraQueryParam", "false");
                    setOption(args, "extraHeader", "false");

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