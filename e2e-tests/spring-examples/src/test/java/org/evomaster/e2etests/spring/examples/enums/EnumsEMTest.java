package org.evomaster.e2etests.spring.examples.enums;

import com.foo.rest.examples.spring.enums.EnumsController;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnumsEMTest extends SpringTestBase{

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EnumsController());
    }

    @Test
    public void testRunEM() throws Throwable {

        handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "false",
                    "--seed", "42",
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "50",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/enums/{target}", "0");
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/enums/{target}", "1");
        });
    }
}
