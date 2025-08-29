package org.evomaster.e2etests.spring.examples.db.javatypes;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaTypesEMTest extends JavaTypesTestBase {


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


    @ParameterizedTest
    @ValueSource(booleans = { false, true})
    public void testRunEMWithSQL(boolean heuristicsForSQLAdvanced) throws Throwable {

        handleFlaky(() -> {
            List<String> args = new ArrayList<>();
            setOption(args, "createTests", "true");
            setOption(args, "seed", "" + defaultSeed++);
            setOption(args, "sutControllerPort", "" + controllerPort);
            setOption(args, "maxEvaluations", "3000");
            setOption(args, "stoppingCriterion", "ACTION_EVALUATIONS");
            setOption(args, "heuristicsForSQL", "true");
            setOption(args, "generateSqlDataWithSearch", "true");
            setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");


            Solution<RestIndividual> solution = (Solution<RestIndividual>) Main.initAndRun(args.toArray(new String[]{}));

            assertTrue(solution.getIndividuals().size() >= 1);

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/javatypes", null);
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/db/javatypes", null);
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/javatypes", null);
        });
    }
}
