package org.evomaster.e2etests.spring.examples.db.javatypes;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaTypesEMTest extends JavaTypesTestBase {


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbJavaTypesEM",
                "org.bar.db.JavaTypesEM",
                3_000,
                (args) -> {
                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/javatypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/db/javatypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/javatypes", null);
                });
    }


    @Test
    public void testRunEMWithSQL() throws Throwable {

        handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "true",
                    "--seed",  "" + defaultSeed++,
                    "--sutControllerPort", "" + controllerPort,
                    "--maxActionEvaluations", "3000",
                    "--stoppingCriterion", "FITNESS_EVALUATIONS",
                    "--heuristicsForSQL", "true",
                    "--generateSqlDataWithSearch", "true"
            };

            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args);

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/javatypes", null);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/db/javatypes", null);
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/javatypes", null);
        });
    }
}
