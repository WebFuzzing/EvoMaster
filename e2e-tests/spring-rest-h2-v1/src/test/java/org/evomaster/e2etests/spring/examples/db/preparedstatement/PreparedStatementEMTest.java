package org.evomaster.e2etests.spring.examples.db.preparedstatement;

import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreparedStatementEMTest extends PreparedStatementTestBase {

    @Test
    public void testRunEM_AVG_DISTANCE() throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE);
    }

    @Test
    public void testRunEM_AVG_DISTANCE_SAME_N_ACTIONS() throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS);
    }

    @Test
    public void testRunEM_BEST_MIN() throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.BEST_MIN);
    }

    private void testRunEM(EMConfig.SecondaryObjectiveStrategy strategy) throws Throwable {

        final String outputFolder = "PreparedStatement_"+ strategy;
        final String outputTestName = "org.bar.db.PreparedStatement_" + strategy;

        runTestHandlingFlakyAndCompilation(
                outputFolder,
                outputTestName,
                7_000,
                (args) -> {
                    args.add("--secondaryObjectiveStrategy");
                    args.add("" + strategy);
                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");
                    args.add("--probOfSmartSampling");
                    args.add("0.0"); // on this example, it has huge negative impact

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/preparedStatement/{integerValue}/{stringValue}/{booleanValue}", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/db/preparedStatement/", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/preparedStatement/{integerValue}/{stringValue}/{booleanValue}", null);

                    assertTextInTests(outputFolder, outputTestName, "controller.resetDatabase(listOf(\"db_prepared_statement_entity\"))");
                });
    }
}
