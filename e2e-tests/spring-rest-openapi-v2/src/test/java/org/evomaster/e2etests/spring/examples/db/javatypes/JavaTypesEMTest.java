package org.evomaster.e2etests.spring.examples.db.javatypes;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaTypesEMTest extends JavaTypesTestBase {

    @Test
    public void testRunEMWithSQL() throws Throwable {

        handleFlaky(() -> {
            String[] args = new String[]{
                    "--createTests", "true",
                    "--seed",  "" + defaultSeed++,
                    "--sutControllerPort", "" + controllerPort,
                    "--maxEvaluations", "3000",
                    "--stoppingCriterion", "ACTION_EVALUATIONS",
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


    @ParameterizedTest
    @ValueSource(booleans = { true, false})
    public void testRunEM(boolean heuristicsForSQLAdvanced) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbJavaTypesEM",
                "org.bar.db.JavaTypesEM" + (heuristicsForSQLAdvanced ? "Complete" : "Partial"),
                3_000,
                (args) -> {
                    setOption(args, "heuristicsForSQL", "true");
                    setOption(args, "generateSqlDataWithSearch", "false");
                    setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/javatypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/db/javatypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/javatypes", null);
                });
    }


}
