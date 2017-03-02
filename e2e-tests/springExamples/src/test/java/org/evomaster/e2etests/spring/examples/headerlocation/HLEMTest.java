package org.evomaster.e2etests.spring.examples.headerlocation;

import com.foo.rest.examples.spring.headerlocation.HeaderLocationController;
import com.foo.rest.examples.spring.postcollection.PostCollectionController;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HLEMTest extends SpringTestBase{

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new HeaderLocationController());
    }

    @Test
    public void testRunEM() {

        String[] args = new String[]{
                "--createTests", "false",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxFitnessEvaluations", "1000",
                "--stoppingCriterion", "FITNESS_EVALUATIONS"
        };

        Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);

        //easy cases
        assertNone(solution, HttpVerb.POST, 404);

        assertHasAtLeastOne(solution, HttpVerb.GET, 404);
        assertHasAtLeastOne(solution, HttpVerb.DELETE, 404);
        assertHasAtLeastOne(solution, HttpVerb.PATCH, 404);
        assertHasAtLeastOne(solution, HttpVerb.PUT, 201);
        assertHasAtLeastOne(solution, HttpVerb.POST, 201);

        //need smart sampling
        assertHasAtLeastOne(solution, HttpVerb.GET, 200);
        assertHasAtLeastOne(solution, HttpVerb.DELETE, 204);
        assertHasAtLeastOne(solution, HttpVerb.PATCH, 204);
        assertHasAtLeastOne(solution, HttpVerb.PUT, 204);
    }
}
