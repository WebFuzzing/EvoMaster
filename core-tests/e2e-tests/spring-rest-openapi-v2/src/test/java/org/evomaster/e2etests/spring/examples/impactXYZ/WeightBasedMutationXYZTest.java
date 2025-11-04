package org.evomaster.e2etests.spring.examples.impactXYZ;

import com.foo.rest.examples.spring.impactXYZ.ImpactXYZRestController;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class WeightBasedMutationXYZTest extends SpringTestBase {

    /**
     * this test aims at testing whether gene selection can be distinguished based on their weights.
     */
    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ImpactWM",
                "org.bar.ImpactWM",
                1000,
                true,
                (args) -> {

                    args.add("--weightBasedMutationRate");
                    args.add("true");

                    // only use weight
                    args.add("--d");
                    args.add("0.0");

                    args.add("--doCollectImpact");
                    args.add("true");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("NONE");

                    //since there only exist one endpoint, we set the population for each target 3
                    args.add("--archiveTargetLimit");
                    args.add("3");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");

                    args.add("--focusedSearchActivationTime");
                    args.add("0.0");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    solution.getIndividuals().stream().allMatch(
                            s -> s.anyImpactInfo() && checkManipulatedTimes(s)
                    );

                }, 3);
    }

    // since weights of dto is more than x, dto has more chances to be mutated.
    private boolean checkManipulatedTimes(EvaluatedIndividual<RestIndividual> ind){
        return ind.getGeneImpact("x").stream().map(s->s.getTimesToManipulate()).reduce(0, Integer::sum) <= ind.getGeneImpact("dto").stream().map(s->s.getTimesToManipulate()).reduce(0, Integer::sum);
    }

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactXYZRestController(Arrays.asList("/api/impactxyz/{x}")));
    }
}
