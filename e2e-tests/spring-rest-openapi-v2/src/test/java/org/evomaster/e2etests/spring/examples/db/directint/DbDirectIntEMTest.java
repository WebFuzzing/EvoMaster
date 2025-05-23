package org.evomaster.e2etests.spring.examples.db.directint;

import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbDirectIntEMTest extends DbDirectIntTestBase {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRunEM_AVG_DISTANCE(boolean heuristicsForSQLAdvanced) throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE, heuristicsForSQLAdvanced);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRunEM_AVG_DISTANCE_SAME_N_ACTIONS(boolean heuristicsForSQLAdvanced) throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS, heuristicsForSQLAdvanced);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRunEM_BEST_MIN(boolean heuristicsForSQLAdvanced) throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.BEST_MIN, heuristicsForSQLAdvanced);
    }

    private void testRunEM(EMConfig.SecondaryObjectiveStrategy strategy, boolean heuristicsForSQLAdvanced) throws Throwable {

        final String outputFolder = "DbDirectEM_" + strategy;
        final String outputTestName = "org.bar.db.DirectEM_" + strategy + "_" + (heuristicsForSQLAdvanced ? "Complete" : "Partial");

        runTestHandlingFlakyAndCompilation(
                outputFolder,
                outputTestName,
                7_000,
                (args) -> {
                    setOption(args, "secondaryObjectiveStrategy", strategy.toString());
                    setOption(args, "heuristicsForSQL", "true");
                    setOption(args, "generateSqlDataWithSearch", "false");
                    setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");
                    setOption(args, "probOfSmartSampling", "0.0"); // on this example, it has huge negative impact

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/directint/{x}/{y}", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/db/directint", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/directint/{x}/{y}", null);

                    assertTextInTests(outputFolder, outputTestName, "controller.resetDatabase(listOf(\"db_direct_int_entity\"))");
                });
    }
}
