package org.evomaster.e2etests.spring.rpc.examples.db.directint;

import com.foo.rpc.examples.spring.db.base.DbBaseController;
import com.foo.rpc.examples.spring.db.base.DbBaseService;
import com.foo.rpc.examples.spring.db.directint.DbDirectIntController;
import com.foo.rpc.examples.spring.db.directint.DbDirectIntService;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbDirectIntEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new DbDirectIntController());
    }


    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testRunEM_AVG_DISTANCE(boolean heuristicsForSQLAdvanced) throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE, heuristicsForSQLAdvanced);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testRunEM_AVG_DISTANCE_SAME_N_ACTIONS(boolean heuristicsForSQLAdvanced) throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS, heuristicsForSQLAdvanced);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testRunEM_BEST_MIN(boolean heuristicsForSQLAdvanced) throws Throwable {
        testRunEM(EMConfig.SecondaryObjectiveStrategy.BEST_MIN, heuristicsForSQLAdvanced);
    }


    private void testRunEM(EMConfig.SecondaryObjectiveStrategy strategy, boolean heuristicsForSQLAdvanced) throws Throwable {
        final String outputFolder = "DbDirectEM_"+ strategy;
        final String outputTestName = "org.bar.db.DirectEM_" + strategy + (heuristicsForSQLAdvanced ? "Complete" : "Partial");

        runTestHandlingFlakyAndCompilation(
                outputFolder,
                outputTestName,
                7_000,
                (args) -> {

                    setOption(args,"secondaryObjectiveStrategy",strategy.toString());
                    setOption(args,"heuristicsForSQL","true");
                    setOption(args,"generateSqlDataWithSearch","false");
                    setOption(args,"heuristicsForSQLAdvanced",heuristicsForSQLAdvanced ? "true" : "false");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertContentInResponseForEndpoint(solution, DbDirectIntService.Iface.class.getName()+":get", "400");
                    assertContentInResponseForEndpoint(solution, DbDirectIntService.Iface.class.getName()+":get", "200");

                    assertTextInTests(outputFolder, outputTestName, "controller.resetDatabase(");
                    assertTextInTests(outputFolder, outputTestName, it -> it.toLowerCase().contains("db_direct_int_entity"));
                });
    }
}

