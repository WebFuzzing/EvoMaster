package org.evomaster.e2etests.spring.examples.db.directintwithsql;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbDirectIntWithSqlEMTest extends DbDirectIntWithSqlTestBase{

    @Disabled
    @Test
    public void testRunEM() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "2000",
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--heuristicsForSQL", "true",
                "--generateSqlData", "true"
        };

        Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

        assertTrue(solution.getIndividuals().size() >= 1);

        //the POST is deactivated in the controller
        assertNone(solution, HttpVerb.POST, 200);

        assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/directint/{x}/{y}", null);
        assertInsertionIntoTable(solution, "DB_DIRECT_INT_ENTITY");
        assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/directint/{x}/{y}", null);
    }
}
