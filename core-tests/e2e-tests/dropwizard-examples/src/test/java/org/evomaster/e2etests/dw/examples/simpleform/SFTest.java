package org.evomaster.e2etests.dw.examples.simpleform;

import com.foo.rest.examples.dw.simpleform.SFController;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class SFTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        RestTestBase.initClass(new SFController());
    }

    @Test
    public void testRunEM() throws Throwable {

        handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "false",
                    "--seed", "" + defaultSeed++,
                    "--sutControllerPort", "" + controllerPort,
                    "--maxEvaluations", "500",
                    "--stoppingCriterion", "ACTION_EVALUATIONS"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.POST, 400);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200);
        });
    }
}
