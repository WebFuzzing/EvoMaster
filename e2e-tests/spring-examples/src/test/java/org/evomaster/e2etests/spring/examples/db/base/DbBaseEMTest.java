package org.evomaster.e2etests.spring.examples.db.base;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbBaseEMTest extends DbBaseTestBase {


    @Test
    public void testRunEM() throws Throwable {

        handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "true",
                    "--seed", "42",
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "10000",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS",
                    "--heuristicsForSQL", "true",
                    "--generateSqlDataWithSearch", "false"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/base/entitiesByName/{name}", "");
        });
    }
}
