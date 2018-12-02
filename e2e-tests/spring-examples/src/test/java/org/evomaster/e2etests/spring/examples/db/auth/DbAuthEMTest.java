package org.evomaster.e2etests.spring.examples.db.auth;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class DbAuthEMTest extends DbAuthTestBase {

    @Test
    public void testRunEM() throws Throwable {

        //handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "true",
                    "--seed", "42",
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "1000",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS",
                    "--heuristicsForSQL", "true",
                    "--generateSqlDataWithSearch", "true"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

        assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/users", null);
        assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/db/projects", null);
        assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/projects", null);
        assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/projects", null);
        //});
    }
}
