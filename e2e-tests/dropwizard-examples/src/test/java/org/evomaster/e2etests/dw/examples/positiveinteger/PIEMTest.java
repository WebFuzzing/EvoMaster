package org.evomaster.e2etests.dw.examples.positiveinteger;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class PIEMTest extends PITestBase {

    @Test
    public void testRunEM(){

        String[] args = new String[]{
                "--createTests", "false",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "200",
                "--stoppingCriterion", "FITNESS_EVALUATIONS"
        };

        Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);
        assertHasAtLeastOne(solution, HttpVerb.GET, 200);
        assertHasAtLeastOne(solution, HttpVerb.POST, 200);
    }


    @Test
    public void testCreateTest() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "20",
                "--stoppingCriterion", "FITNESS_EVALUATIONS"
        };

       Main.initAndRun(args);

       //TODO check file
    }
}
